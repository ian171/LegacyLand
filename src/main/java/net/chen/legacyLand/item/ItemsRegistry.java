package net.chen.legacyLand.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 物品注册中心，模仿 Fabric Registry 风格
 *
 * 用法：
 * public static final CustomItem MERCURY_THERMOMETER =
 *     ItemsRegistry.register("mercury_thermometer", new ThermometerItem());
 */
public final class ItemsRegistry {

    private static final Map<String, CustomItem> REGISTRY = new HashMap<>();

    private ItemsRegistry() {}

    /**
     * 注册自定义物品
     */
    public static <T extends CustomItem> T register(String id, T item) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("物品 ID 已注册: " + id);
        }
        REGISTRY.put(id, item);
        return item;
    }

    /**
     * 通过 ID 获取 CustomItem 实例
     */
    public static CustomItem get(String id) {
        return REGISTRY.get(id);
    }

    /**
     * 通过 ID 生成 ItemStack
     */
    public static ItemStack make(String id) {
        CustomItem item = REGISTRY.get(id);
        if (item == null) throw new IllegalArgumentException("未找到物品: " + id);
        return item.createItemStack();
    }

    /**
     * 从 ItemStack 的 PDC 中读取 item_id
     */
    public static String getItemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(CustomItem.ITEM_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * 从 ItemStack 反查 CustomItem 实例
     */
    public static CustomItem fromStack(ItemStack stack) {
        String id = getItemId(stack);
        return id != null ? REGISTRY.get(id) : null;
    }

    public static Map<String, CustomItem> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
