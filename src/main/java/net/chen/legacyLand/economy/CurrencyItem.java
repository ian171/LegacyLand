package net.chen.legacyLand.economy;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import net.chen.legacyLand.LegacyLand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 货币物品工具类
 * 处理带 NBT 标签的纸张货币
 */
public class CurrencyItem {
    private static final NamespacedKey KEY_CURRENCY = new NamespacedKey(LegacyLand.getInstance(), "currency");
    private static final NamespacedKey KEY_NATION = new NamespacedKey(LegacyLand.getInstance(), "nation");
    private static final NamespacedKey KEY_DENOMINATION = new NamespacedKey(LegacyLand.getInstance(), "denomination");
    private static final NamespacedKey KEY_SERIAL = new NamespacedKey(LegacyLand.getInstance(), "serial");
    private static final NamespacedKey KEY_ISSUED_AT = new NamespacedKey(LegacyLand.getInstance(), "issued_at");

    /**
     * 创建货币物品
     * @param nationName 国家名称
     * @param denomination 面值
     * @param serialNumber 序列号
     * @return 货币物品
     */
    public static ItemStack createCurrency(String nationName, double denomination, String serialNumber) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 设置 NBT 数据
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_CURRENCY, PersistentDataType.STRING, "true");
        container.set(KEY_NATION, PersistentDataType.STRING, nationName);
        container.set(KEY_DENOMINATION, PersistentDataType.DOUBLE, denomination);
        container.set(KEY_SERIAL, PersistentDataType.STRING, serialNumber);
        container.set(KEY_ISSUED_AT, PersistentDataType.LONG, System.currentTimeMillis());

        // 设置显示名称和 Lore
        meta.setDisplayName("§6§l" + nationName + " 货币");
        List<String> lore = new ArrayList<>();
        lore.add("§7面值: §e" + formatAmount(denomination) + " §7金锭");
        lore.add("§7序列号: §f" + serialNumber);
        lore.add("§8官方发行 · 防伪认证");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 创建国库印章（特殊物品，无法在生存模式获取）
     * @param nationName 国家名称
     * @return 国库印章
     */
    public static ItemStack createTreasurySeal(String nationName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 设置特殊 NBT 标记
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_CURRENCY, PersistentDataType.STRING, "seal");
        container.set(KEY_NATION, PersistentDataType.STRING, nationName);
        container.set(KEY_SERIAL, PersistentDataType.STRING, "SEAL-" + UUID.randomUUID().toString());

        // 设置显示
        meta.setDisplayName("§5§l" + nationName + " 国库印章");
        List<String> lore = new ArrayList<>();
        lore.add("§7用于铸造官方货币");
        lore.add("§c§l无法在生存模式获取");
        lore.add("§8国家主权象征");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 检查是否为货币物品
     */
    public static boolean isCurrency(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String currencyTag = container.get(KEY_CURRENCY, PersistentDataType.STRING);
        return "true".equals(currencyTag);
    }

    /**
     * 检查是否为国库印章
     */
    public static boolean isTreasurySeal(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String currencyTag = container.get(KEY_CURRENCY, PersistentDataType.STRING);
        return "seal".equals(currencyTag);
    }

    /**
     * 获取货币国家
     */
    public static String getNation(ItemStack item) {
        if (!isCurrency(item) && !isTreasurySeal(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(KEY_NATION, PersistentDataType.STRING);
    }

    /**
     * 获取货币面值
     */
    public static double getDenomination(ItemStack item) {
        if (!isCurrency(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Double denomination = container.get(KEY_DENOMINATION, PersistentDataType.DOUBLE);
        return denomination != null ? denomination : 0;
    }

    /**
     * 获取序列号
     */
    public static String getSerialNumber(ItemStack item) {
        if (!isCurrency(item) && !isTreasurySeal(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(KEY_SERIAL, PersistentDataType.STRING);
    }

    /**
     * 获取发行时间
     */
    public static long getIssuedAt(ItemStack item) {
        if (!isCurrency(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Long issuedAt = container.get(KEY_ISSUED_AT, PersistentDataType.LONG);
        return issuedAt != null ? issuedAt : 0;
    }

    /**
     * 格式化金额显示
     */
    private static String formatAmount(double amount) {
        if (amount >= 1000000) {
            return String.format("%.2fM", amount / 1000000);
        } else if (amount >= 1000) {
            return String.format("%.2fK", amount / 1000);
        } else {
            return String.format("%.2f", amount);
        }
    }

    /**
     * 计算物品堆中的总货币价值
     */
    public static double getTotalValue(ItemStack item) {
        if (!isCurrency(item)) return 0;
        return getDenomination(item) * item.getAmount();
    }
}
