package net.chen.legacyLand.resource;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源物品工厂
 * 创建和识别特殊资源物品
 */
public class ResourceItemFactory {

    private static Plugin plugin;
    private static NamespacedKey RESOURCE_TYPE_KEY;

    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
        RESOURCE_TYPE_KEY = new NamespacedKey(plugin, "resource_type");
    }

    /**
     * 创建资源物品
     */
    public static ItemStack createResourceItem(ResourceType resourceType, int amount) {
        ItemStack item = new ItemStack(resourceType.getBaseMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 设置显示名称
            meta.setDisplayName(resourceType.getColoredName());

            // 设置 Lore
            List<String> lore = new ArrayList<>();
            lore.add("§7类型: " + resourceType.getDisplayName());
            lore.add("§7重量: §f" + resourceType.getWeight() + " 单位/个");
            lore.add("§8特殊资源");
            meta.setLore(lore);

            // 设置 NBT 标签
            meta.getPersistentDataContainer().set(
                    RESOURCE_TYPE_KEY,
                    PersistentDataType.STRING,
                    resourceType.name()
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 检查物品是否是特殊资源
     */
    public static boolean isResourceItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(RESOURCE_TYPE_KEY, PersistentDataType.STRING);
    }

    /**
     * 获取物品的资源类型
     */
    public static ResourceType getResourceType(ItemStack item) {
        if (!isResourceItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String typeName = meta.getPersistentDataContainer().get(RESOURCE_TYPE_KEY, PersistentDataType.STRING);

        try {
            return ResourceType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 检查物品是否是高纯度资源
     */
    public static boolean isHighPurityResource(ItemStack item) {
        ResourceType type = getResourceType(item);
        if (type == null) {
            return false;
        }

        return type == ResourceType.HIGH_PURITY_IRON ||
               type == ResourceType.HIGH_PURITY_GOLD ||
               type == ResourceType.HIGH_PURITY_COPPER;
    }
}
