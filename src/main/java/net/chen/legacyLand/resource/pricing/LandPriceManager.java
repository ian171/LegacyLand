package net.chen.legacyLand.resource.pricing;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.util.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 地价交互管理器（P3）。
 * <p>
 * 维护：
 * <ul>
 *   <li>每个区块的询问列表（ask/reply 用）。</li>
 *   <li>每位玩家公开展示的区块集合（show/hide 用）。</li>
 *   <li>询问 TTL 清理。</li>
 * </ul>
 * 状态全部内存持有，重启清空——询问与公开标记是会话级状态。
 */
public class LandPriceManager {

    @Getter
    private static volatile LandPriceManager instance;

    private final LegacyLand plugin;

    /** chunkKey → 所有针对该区块的询问。 */
    private final ConcurrentHashMap<String, List<LandPriceInquiry>> inquiriesByChunk = new ConcurrentHashMap<>();
    /** playerId → 该玩家公开的区块集合。 */
    private final ConcurrentHashMap<UUID, Set<String>> shownByPlayer = new ConcurrentHashMap<>();
    /** 反向索引：chunkKey → 公开它的玩家数量（>0 即视为公开）。 */
    private final ConcurrentHashMap<String, Integer> publicChunks = new ConcurrentHashMap<>();

    private final AtomicLong idGen = new AtomicLong(1);

    private LandPriceManager(LegacyLand plugin) {
        this.plugin = plugin;
    }

    public static LandPriceManager init(LegacyLand plugin) {
        if (instance == null) {
            synchronized (LandPriceManager.class) {
                if (instance == null) instance = new LandPriceManager(plugin);
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Permission helpers
    // -----------------------------------------------------------------------

    /** 当前 chunk 的 Town；未声明返回 null。 */
    public Town townOf(Location loc) {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(loc);
        if (tb == null || !tb.hasTown()) return null;
        return tb.getTownOrNull();
    }

    /** 玩家是否为该 Town 的成员（含市长/国王）。 */
    public boolean isMember(Player player, Town town) {
        if (town == null) return false;
        try {
            Resident r = TownyAPI.getInstance().getResident(player);
            if (r == null) return false;
            for (Resident member : town.getResidents()) {
                if (member.getUUID().equals(r.getUUID())) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // -----------------------------------------------------------------------
    // Show / Hide
    // -----------------------------------------------------------------------

    /** 切换玩家对某 chunk 的公开状态。返回切换后的状态：true=公开，false=隐藏。 */
    public boolean toggleShown(UUID playerId, String chunkKey) {
        Set<String> set = shownByPlayer.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        boolean nowShown;
        if (set.contains(chunkKey)) {
            set.remove(chunkKey);
            publicChunks.compute(chunkKey, (k, v) -> v == null || v <= 1 ? null : v - 1);
            nowShown = false;
        } else {
            set.add(chunkKey);
            publicChunks.merge(chunkKey, 1, Integer::sum);
            nowShown = true;
        }
        return nowShown;
    }

    public boolean setShown(UUID playerId, String chunkKey, boolean show) {
        Set<String> set = shownByPlayer.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        boolean changed;
        if (show) {
            changed = set.add(chunkKey);
            if (changed) publicChunks.merge(chunkKey, 1, Integer::sum);
        } else {
            changed = set.remove(chunkKey);
            if (changed) publicChunks.compute(chunkKey, (k, v) -> v == null || v <= 1 ? null : v - 1);
        }
        return changed;
    }

    public boolean isPubliclyShown(String chunkKey) {
        Integer n = publicChunks.get(chunkKey);
        return n != null && n > 0;
    }

    // -----------------------------------------------------------------------
    // Inquiries
    // -----------------------------------------------------------------------

    public LandPriceInquiry submitInquiry(Player asker, String world, int cx, int cz, String message, long ttlMillis) {
        cleanupExpired(ttlMillis);
        String chunkKey = ChunkResourceData.key(world, cx, cz);
        LandPriceInquiry inq = new LandPriceInquiry(
                idGen.getAndIncrement(),
                asker.getUniqueId(),
                asker.getName(),
                chunkKey, world, cx, cz,
                message == null ? "" : message,
                System.currentTimeMillis(),
                null, null, null, 0L);
        inquiriesByChunk
                .computeIfAbsent(chunkKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(inq);
        return inq;
    }

    public List<LandPriceInquiry> listInquiries(String chunkKey, long ttlMillis) {
        cleanupExpired(ttlMillis);
        List<LandPriceInquiry> list = inquiriesByChunk.get(chunkKey);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    /** 找到指定 id 的询问；若 chunkKey 不匹配返回 null。 */
    public LandPriceInquiry findInquiry(String chunkKey, long id) {
        List<LandPriceInquiry> list = inquiriesByChunk.get(chunkKey);
        if (list == null) return null;
        synchronized (list) {
            for (LandPriceInquiry q : list) if (q.id() == id) return q;
        }
        return null;
    }

    /** 回复询问，返回是否成功（询问不存在或已回复返回 false）。 */
    public boolean replyInquiry(String chunkKey, long id, Player replier, double price) {
        List<LandPriceInquiry> list = inquiriesByChunk.get(chunkKey);
        if (list == null) return false;
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                LandPriceInquiry q = list.get(i);
                if (q.id() == id) {
                    if (q.isReplied()) return false;
                    list.set(i, q.withReply(replier.getUniqueId(), replier.getName(), price, System.currentTimeMillis()));
                    notifyAsker(q, replier, price);
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyAsker(LandPriceInquiry q, Player replier, double price) {
        Player asker = Bukkit.getPlayer(q.askerId());
        if (asker == null || !asker.isOnline()) return;
        asker.sendMessage(LanguageManager.getInstance().translate(
                "landprice.reply_received",
                replier.getName(),
                q.chunkX() + "," + q.chunkZ(),
                String.format("%.2f", price)));
    }

    /** 清理过期询问。 */
    public void cleanupExpired(long ttlMillis) {
        if (ttlMillis <= 0) return;
        long cutoff = System.currentTimeMillis() - ttlMillis;
        inquiriesByChunk.entrySet().removeIf(entry -> {
            List<LandPriceInquiry> list = entry.getValue();
            synchronized (list) {
                list.removeIf(q -> q.createdAt() < cutoff);
                return list.isEmpty();
            }
        });
    }

    /** 玩家离线时调用，释放其公开标记。 */
    public void onPlayerQuit(UUID playerId) {
        Set<String> set = shownByPlayer.remove(playerId);
        if (set == null) return;
        for (String key : new HashSet<>(set)) {
            publicChunks.compute(key, (k, v) -> v == null || v <= 1 ? null : v - 1);
        }
    }

    public int getInquiryChunkCount() { return inquiriesByChunk.size(); }
    public int getPublicChunkCount() { return publicChunks.size(); }

    public LegacyLand getPlugin() { return plugin; }
}
