package net.chen.legacyLand.organization.outpost;

import lombok.Data;
import org.bukkit.inventory.ItemStack;

/**
 * 据点货物数据类
 */
@Data
public class OutpostGoods {
    private final String id;
    private final String outpostId;
    private final ItemStack item;
    private double price;
    private int quantity;
    private final long addedAt;

    public OutpostGoods(String id, String outpostId, ItemStack item, double price, int quantity) {
        this.id = id;
        this.outpostId = outpostId;
        this.item = item.clone();
        this.price = price;
        this.quantity = quantity;
        this.addedAt = System.currentTimeMillis();
    }

    /**
     * 减少数量
     */
    public boolean decreaseQuantity(int amount) {
        if (quantity >= amount) {
            quantity -= amount;
            return true;
        }
        return false;
    }

    /**
     * 增加数量
     */
    public void increaseQuantity(int amount) {
        quantity += amount;
    }

    /**
     * 是否有库存
     */
    public boolean hasStock() {
        return quantity > 0;
    }
}
