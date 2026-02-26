package net.chen.legacyLand.market;

import lombok.Data;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 市场数据类
 * 一个市场对应 Towny 中的一个地块（TownBlock）
 */
@Data
public class Market {
    private final String id;
    private final String nationName;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final UUID approvedBy;
    private final long createdAt;
    // key = "world,x,y,z"
    private final Map<String, MarketChest> chests;

    public Market(String id, String nationName, String worldName, int chunkX, int chunkZ, UUID approvedBy) {
        this.id = id;
        this.nationName = nationName;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.approvedBy = approvedBy;
        this.createdAt = System.currentTimeMillis();
        this.chests = new ConcurrentHashMap<>();
    }

    /**
     * 检查坐标是否在此市场地块内（chunk 级别）
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) return false;
        return location.getChunk().getX() == chunkX && location.getChunk().getZ() == chunkZ;
    }

    public void addChest(MarketChest chest) {
        chests.put(chest.getLocationKey(), chest);
    }

    public void removeChest(String locationKey) {
        chests.remove(locationKey);
    }

    public MarketChest getChest(String locationKey) {
        return chests.get(locationKey);
    }

    public List<MarketChest> getAllChests() {
        return new ArrayList<>(chests.values());
    }

    public static String toLocationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
