package net.chen.legacyLand.resource.pricing;

import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.database.DatabaseManager;
import net.chen.legacyLand.resource.pricing.event.ChunkExhaustedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 区块资源稀缺度定价系统的核心管理器（P1：储量普查；P2：采集累加 + 定时衰减）。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>Lazy 加载：玩家进入区块时若 DB 无记录则触发一次扫描，避免一次性遍历整张地图。</li>
 *   <li>内存缓存：扫描结果以 ConcurrentHashMap 缓存，避免重复查询 DB。</li>
 *   <li>扫描去重：scanLock 集合保证同一区块不会被并发扫描。</li>
 *   <li>异步：快照在事件线程获取后，扫描+持久化在虚拟线程执行。</li>
 *   <li>P2 延迟衰减：BlockBreak/Explode 仅在 {@link #pendingDecrements} 累加，
 *       由 {@link ChunkResourceRecalcTask} 定时 flush，避免每次破坏触发 DB 写。</li>
 * </ul>
 */
public class ChunkResourceManager {

    @Getter
    private static volatile ChunkResourceManager instance;

    private final LegacyLand plugin;
    @Getter
    private final ResourcePricingConfig config;
    private final DatabaseManager databaseManager;

    /** key = world:x:z；value = 已扫描数据。 */
    private final ConcurrentHashMap<String, ChunkResourceData> cache = new ConcurrentHashMap<>();
    /** 正在扫描的区块 key，避免重复入队。 */
    private final java.util.Set<String> scanLock = ConcurrentHashMap.newKeySet();

    /** P2: 累加的待 flush 衰减量。key = chunk key, value = 待扣减总权重。 */
    private final ConcurrentHashMap<String, Double> pendingDecrements = new ConcurrentHashMap<>();
    /** P2: 玩家放置标记。key = chunk key, value = 编码后方块坐标集合。 */
    private final ConcurrentHashMap<String, Set<Long>> playerPlacedBlocks = new ConcurrentHashMap<>();

    private final AtomicLong scanCount = new AtomicLong();
    private final AtomicLong cacheHit = new AtomicLong();
    private final AtomicLong totalDecremented = new AtomicLong();
    private final AtomicLong exhaustedEventCount = new AtomicLong();

    private ChunkResourceManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.config = new ResourcePricingConfig();
        this.config.load(plugin);
    }

    public static ChunkResourceManager init(LegacyLand plugin) {
        if (instance == null) {
            synchronized (ChunkResourceManager.class) {
                if (instance == null) {
                    instance = new ChunkResourceManager(plugin);
                }
            }
        }
        return instance;
    }

    /**
     * 在区块加载事件触发时调用：
     * 1) 查缓存 / DB，命中则缓存返回；
     * 2) 未命中则提交虚拟线程异步扫描。
     * <p>
     * 调用方必须保证 chunk 在主线程或对应区域线程上下文，因为 {@link Chunk#getChunkSnapshot()}
     * 不能在任意异步线程调用。
     */
    public void scanIfAbsent(Chunk chunk) {
        if (!config.isEnabled()) return;

        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        String key = ChunkResourceData.key(world, x, z);

        if (cache.containsKey(key)) {
            cacheHit.incrementAndGet();
            return;
        }
        if (!scanLock.add(key)) return; // 已在扫描中

        // 先尝试 DB 加载，避免重复扫描已有记录
        ChunkResourceData existing = loadFromDatabaseSafe(world, x, z);
        if (existing != null && !config.isRescanOnLoad()) {
            cache.put(key, existing);
            scanLock.remove(key);
            return;
        }

        // 必须在调用线程获取快照与 worldMinY（事件线程上下文，ChunkSnapshot 不暴露 minY）
        ChunkSnapshot snapshot;
        int worldMinY;
        int worldMaxY;
        try {
            snapshot = chunk.getChunkSnapshot(false, true, false);
            worldMinY = chunk.getWorld().getMinHeight();
            worldMaxY = chunk.getWorld().getMaxHeight();
        } catch (Throwable t) {
            scanLock.remove(key);
            LegacyLand.logger.warning("[ResourcePricing] 获取区块快照失败: " + t.getMessage());
            return;
        }

        Thread.ofVirtual()
                .name("LegacyLand-ChunkScan-" + key)
                .start(new ChunkScanTask(snapshot, world, worldMinY, worldMaxY, config));
    }

    /** 由 {@link ChunkScanTask} 在扫描完成后回调。 */
    void persistScanned(ChunkResourceData data) {
        try {
            databaseManager.saveChunkResource(data);
            cache.put(data.key(), data);
            scanCount.incrementAndGet();
        } catch (Throwable t) {
            LegacyLand.logger.warning("[ResourcePricing] 持久化扫描结果失败: " + t.getMessage());
        } finally {
            scanLock.remove(data.key());
        }
    }

    void releaseScanLock(String key) {
        scanLock.remove(key);
    }

    /** 查询区块储量数据；优先从缓存读取，缺失则懒加载一次。 */
    public Optional<ChunkResourceData> get(String world, int x, int z) {
        String key = ChunkResourceData.key(world, x, z);
        ChunkResourceData cached = cache.get(key);
        if (cached != null) return Optional.of(cached);

        ChunkResourceData fromDb = loadFromDatabaseSafe(world, x, z);
        if (fromDb != null) cache.put(key, fromDb);
        return Optional.ofNullable(fromDb);
    }

    private ChunkResourceData loadFromDatabaseSafe(String world, int x, int z) {
        try {
            return databaseManager.loadChunkResource(world, x, z);
        } catch (Throwable t) {
            LegacyLand.logger.warning("[ResourcePricing] 读取区块数据失败: " + t.getMessage());
            return null;
        }
    }

    public long getScanCount() { return scanCount.get(); }
    public long getCacheHit() { return cacheHit.get(); }
    public int getCacheSize() { return cache.size(); }
    public long getTotalDecremented() { return totalDecremented.get(); }
    public long getExhaustedEventCount() { return exhaustedEventCount.get(); }
    public int getPendingDecrementCount() { return pendingDecrements.size(); }

    // -----------------------------------------------------------------------
    // P2: 采集追踪 + 定时衰减
    // -----------------------------------------------------------------------

    /**
     * 累加一个待 flush 的衰减量。仅写内存，不触发 DB 与价格计算。
     * 由 BlockBreakEvent / BlockExplodeEvent / EntityExplodeEvent 监听器调用。
     */
    public void accumulateDecrement(String world, int chunkX, int chunkZ, double delta) {
        if (!config.isEnabled() || delta <= 0.0) return;
        String key = ChunkResourceData.key(world, chunkX, chunkZ);
        pendingDecrements.merge(key, delta, Double::sum);
    }

    /** 标记一个由玩家放置的方块；再次破坏该方块时不计入耗竭。 */
    public void markPlayerPlaced(String world, int chunkX, int chunkZ, int bx, int by, int bz) {
        String key = ChunkResourceData.key(world, chunkX, chunkZ);
        playerPlacedBlocks
                .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(encodeBlockPos(bx, by, bz));
    }

    /** 查询并移除玩家放置标记（一次性消耗）。命中 true 表示该方块由玩家放置，调用方应跳过累加。 */
    public boolean consumePlayerPlaced(String world, int chunkX, int chunkZ, int bx, int by, int bz) {
        String key = ChunkResourceData.key(world, chunkX, chunkZ);
        Set<Long> set = playerPlacedBlocks.get(key);
        if (set == null) return false;
        boolean removed = set.remove(encodeBlockPos(bx, by, bz));
        if (set.isEmpty()) playerPlacedBlocks.remove(key, set);
        return removed;
    }

    /** 简单位编码：x(26b 含符号) | y(12b 含 minY 偏移) | z(26b 含符号)。-32M~32M 区间足够。 */
    private static long encodeBlockPos(int x, int y, int z) {
        long ux = ((long) x) & 0x3FFFFFFL;       // 26 bit
        long uy = ((long) (y + 2048)) & 0xFFFL;  // 12 bit, 容纳 -2048~2047
        long uz = ((long) z) & 0x3FFFFFFL;
        return (ux << 38) | (uy << 26) | uz;
    }

    /**
     * 由 {@link ChunkResourceRecalcTask} 定时调用：
     * 1) 抽取 {@link #pendingDecrements} 快照并清空；
     * 2) 逐项更新 cache、写库（{@link DatabaseManager#decrementChunkResource}）；
     * 3) 若 currentValue 由 &gt;0 降到 ≤0，触发 {@link ChunkExhaustedEvent}。
     */
    public void flushPendingDecrements() {
        if (!config.isEnabled() || pendingDecrements.isEmpty()) return;

        Map<String, Double> snapshot = new HashMap<>();
        for (Map.Entry<String, Double> e : pendingDecrements.entrySet()) {
            Double removed = pendingDecrements.remove(e.getKey());
            if (removed != null) snapshot.put(e.getKey(), removed);
        }
        if (snapshot.isEmpty()) return;

        for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            double delta = entry.getValue();
            if (delta <= 0.0) continue;

            String[] parts = key.split(":");
            if (parts.length != 3) continue;
            String world = parts[0];
            int cx, cz;
            try {
                cx = Integer.parseInt(parts[1]);
                cz = Integer.parseInt(parts[2]);
            } catch (NumberFormatException nfe) {
                continue;
            }

            ChunkResourceData data = cache.get(key);
            if (data == null) {
                data = loadFromDatabaseSafe(world, cx, cz);
                if (data == null) continue; // 区块尚未普查，跳过
                cache.put(key, data);
            }

            double before = data.getCurrentValue();
            if (before <= 0.0) {
                totalDecremented.addAndGet((long) delta);
                continue; // 已耗竭，无需重复事件 / 写库
            }

            double after = Math.max(0.0, before - delta);
            data.setCurrentValue(after);
            totalDecremented.addAndGet((long) delta);

            try {
                databaseManager.decrementChunkResource(world, cx, cz, delta);
            } catch (Throwable t) {
                LegacyLand.logger.warning("[ResourcePricing] 衰减写库失败 " + key + ": " + t.getMessage());
            }

            if (after <= 0.0) {
                exhaustedEventCount.incrementAndGet();
                final ChunkResourceData snap = data;
                try {
                    Bukkit.getPluginManager().callEvent(new ChunkExhaustedEvent(world, cx, cz, snap));
                } catch (Throwable t) {
                    LegacyLand.logger.warning("[ResourcePricing] 触发 ChunkExhaustedEvent 失败 " + key + ": " + t.getMessage());
                }
            }

            if (config.isLogVerbose()) {
                LegacyLand.logger.info("[ResourcePricing] flush " + key
                        + " Δ=" + delta + " " + before + " → " + after);
            }
        }
    }

    /**
     * 兼容旧调用方的入口。P2 起转发到 {@link #accumulateDecrement}，不再立刻写库。
     */
    public void decrementCurrentValue(String world, int x, int z, double delta) {
        accumulateDecrement(world, x, z, delta);
    }
}
