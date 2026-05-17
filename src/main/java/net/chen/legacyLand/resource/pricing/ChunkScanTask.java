package net.chen.legacyLand.resource.pricing;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

/**
 * 区块储量扫描任务（P1）。
 * <p>
 * 关键优化 —— <b>Palette Early-Exit</b>：
 * Minecraft 区块按 16 高一段（section）使用 palette 存储方块；当 section 仅含 AIR 时
 * {@link ChunkSnapshot#isSectionEmpty(int)} 返回 true，可直接跳过整段 4096 个方块。
 * 对于地表以上的几何空气段（大部分 overworld 区块的 6~10 段），该剪枝可减少 60% 以上的循环。
 * <p>
 * Section 索引计算：sy = (y - worldMinY) >> 4。worldMinY 必须在调度方主线程获取后传入，
 * ChunkSnapshot 自身不暴露此值。
 */
public class ChunkScanTask implements Runnable {

    private final ChunkSnapshot snapshot;
    private final String worldName;
    private final int worldMinY;
    private final ResourcePricingConfig config;

    public ChunkScanTask(ChunkSnapshot snapshot, String worldName, int worldMinY, ResourcePricingConfig config) {
        this.snapshot = snapshot;
        this.worldName = worldName;
        this.worldMinY = worldMinY;
        this.config = config;
    }

    @Override
    public void run() {
        String key = ChunkResourceData.key(worldName, snapshot.getX(), snapshot.getZ());
        try {
            ChunkResourceData data = scan();
            ChunkResourceManager.getInstance().persistScanned(data);
        } catch (Throwable t) {
            LegacyLand.logger.warning("[ResourcePricing] 扫描区块失败 (" + key + "): " + t.getMessage());
            ChunkResourceManager.getInstance().releaseScanLock(key);
        }
    }

    private ChunkResourceData scan() {
        int yMin = config.getScanYMin();
        int yMax = config.getScanYMax();

        // Palette Early-Exit: 按 section 跳过纯空气段
        int sectionMin = Math.floorDiv(yMin - worldMinY, 16);
        int sectionMax = Math.floorDiv(yMax - worldMinY, 16);

        double rawValue = 0.0;
        int skippedSections = 0;
        int scannedSections = 0;

        for (int sy = sectionMin; sy <= sectionMax; sy++) {
            if (isSectionEmptySafe(sy)) {
                skippedSections++;
                continue;
            }
            scannedSections++;

            int sectionBaseY = sy * 16 + worldMinY;
            int yLo = Math.max(yMin, sectionBaseY);
            int yHi = Math.min(yMax, sectionBaseY + 15);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = yLo; y <= yHi; y++) {
                        Material mat = snapshot.getBlockType(x, y, z);
                        double w = config.weightOf(mat);
                        if (w > 0.0) rawValue += w;
                    }
                }
            }
        }

        String biome = sampleBiome(yMax);
        double biomeFactor = config.biomeFactorOf(biome);
        double initial = rawValue * biomeFactor;

        if (config.isLogVerbose()) {
            LegacyLand.logger.info(String.format(
                    "[ResourcePricing] chunk(%s,%d,%d) scanned: rawValue=%.1f, biome=%s, factor=%.2f, sections=%d, skipped=%d",
                    worldName, snapshot.getX(), snapshot.getZ(),
                    rawValue, biome, biomeFactor, scannedSections, skippedSections));
        }

        return new ChunkResourceData(
                worldName,
                snapshot.getX(),
                snapshot.getZ(),
                biome,
                initial,
                initial,
                biomeFactor,
                System.currentTimeMillis()
        );
    }

    private boolean isSectionEmptySafe(int sy) {
        try {
            return snapshot.isSectionEmpty(sy);
        } catch (Throwable ignored) {
            return false; // 越界等异常一律视为非空，回退到全扫描
        }
    }

    private String sampleBiome(int yMax) {
        try {
            int sampleY = Math.min(64, yMax);
            return snapshot.getBiome(8, sampleY, 8).getKey().getKey().toUpperCase();
        } catch (Throwable t) {
            return "UNKNOWN";
        }
    }
}
