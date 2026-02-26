package net.chen.legacyLand.market;

import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

/**
 * 市场销售箱数据类
 */
@Data
public class MarketChest {
    private final String id;
    private final String marketId;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final UUID ownerUuid;
    // 单个物品售价（买家每次购买一个物品支付的金额）
    private double pricePerItem;
    // 是否已设置价格（箱子放置后必须先 /price set 才能对外销售）
    private boolean priceSet;
    private final long createdAt;

    public MarketChest(String id, String marketId, Location location, UUID ownerUuid) {
        this.id = id;
        this.marketId = marketId;
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.ownerUuid = ownerUuid;
        this.pricePerItem = 0;
        this.priceSet = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getLocationKey() {
        return worldName + "," + x + "," + y + "," + z;
    }

    public boolean isActive() {
        return priceSet && pricePerItem > 0;
    }
}
