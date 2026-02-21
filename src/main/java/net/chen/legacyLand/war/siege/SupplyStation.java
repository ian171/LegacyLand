package net.chen.legacyLand.war.siege;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 补给站
 */
@Data
public class SupplyStation {
    private final String warId;
    private final String townName;
    private final Location location;
    private final UUID ownerId;
    private final long createTime;
    private boolean active;
    private final List<ItemStack> supplies;
    private int maxSupplies;

    public SupplyStation(String warId, String townName, Location location, UUID ownerId) {
        this.warId = warId;
        this.townName = townName;
        this.location = location;
        this.ownerId = ownerId;
        this.createTime = System.currentTimeMillis();
        this.active = true;
        this.supplies = new ArrayList<>();
        this.maxSupplies = 64; // 默认最多64个补给
    }

    /**
     * 添加补给
     */
    public boolean addSupply(ItemStack supply) {
        if (supplies.size() >= maxSupplies) {
            return false;
        }
        supplies.add(supply);
        return true;
    }

    /**
     * 移除补给
     */
    public ItemStack removeSupply() {
        if (supplies.isEmpty()) {
            return null;
        }
        return supplies.remove(0);
    }

    /**
     * 获取补给数量
     */
    public int getSupplyCount() {
        return supplies.size();
    }

    /**
     * 摧毁补给站
     */
    public void destroy() {
        this.active = false;
        this.supplies.clear();
    }

    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return supplies.size() >= maxSupplies;
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return supplies.isEmpty();
    }
}
