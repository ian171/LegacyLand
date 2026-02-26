package net.chen.legacyLand.organization.outpost;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 据点数据类
 */
@Data
public class Outpost {
    private final String id;
    private final String organizationId;
    private final Location center;
    private final int radius;
    private OutpostStatus status;
    private final Map<String, OutpostGoods> goods;
    private final long createdAt;

    public Outpost(String id, String organizationId, Location center, int radius) {
        this.id = id;
        this.organizationId = organizationId;
        this.center = center;
        this.radius = radius;
        this.status = OutpostStatus.OPEN;
        this.goods = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 检查位置是否在据点范围内
     */
    public boolean isInRange(Location location) {
        if (!location.getWorld().equals(center.getWorld())) {
            return false;
        }
        return location.distanceSquared(center) <= radius * radius;
    }

    /**
     * 添加货物
     */
    public void addGoods(OutpostGoods goods) {
        this.goods.put(goods.getId(), goods);
    }

    /**
     * 移除货物
     */
    public void removeGoods(String goodsId) {
        goods.remove(goodsId);
    }

    /**
     * 获取货物
     */
    public OutpostGoods getGoods(String goodsId) {
        return goods.get(goodsId);
    }

    /**
     * 获取所有货物
     */
    public List<OutpostGoods> getAllGoods() {
        return new ArrayList<>(goods.values());
    }

    /**
     * 是否开放
     */
    public boolean isOpen() {
        return status == OutpostStatus.OPEN;
    }

    /**
     * 开放据点
     */
    public void open() {
        this.status = OutpostStatus.OPEN;
    }

    /**
     * 关闭据点
     */
    public void close() {
        this.status = OutpostStatus.CLOSED;
    }
}
