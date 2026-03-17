package net.chen.legacyLand.item;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.item.attribute.AttributeEvaluator;
import net.chen.legacyLand.item.attribute.ItemAttributes;
import net.chen.legacyLand.item.event.ItemEvents;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 自定义物品基础接口，模仿 Fabric API 注册风格
 */
public interface CustomItem {

    NamespacedKey ITEM_ID_KEY = new NamespacedKey(LegacyLand.getInstance(), "item_id");

    /** 物品唯一 ID，如 "bleed_spear" */
    String getId();

    /** CustomModelData 值 */
    int getCmd();

    /** 材质贴图路径，如 "legacyland:item/bleed_spear" */
    String getTexturePath();

    /** 基础材质 */
    Material getBaseMaterial();

    /** 物品属性，无属性返回 null */
    default ItemAttributes getAttributes() { return null; }

    /**
     * 生成 ItemStack，写入 CMD、PDC、隐藏 Flag，并应用属性
     */
    default ItemStack createItemStack() {
        ItemStack item = new ItemStack(getBaseMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setCustomModelData(getCmd());
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, getId());

        item.setItemMeta(meta);

        // 写入属性（不依赖玩家，使用基础值）
        if (getAttributes() != null) {
            applyAttributes(item, getAttributes());
        }

        return item;
    }

    /**
     * 将属性写入 PDC 并设置原生 AttributeModifier
     */
    default void applyAttributes(ItemStack item, ItemAttributes attrs) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 写入 PDC
        var pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(LegacyLand.getInstance(), "attr_damage"),
                PersistentDataType.DOUBLE, attrs.getAttackDamage());
        pdc.set(new NamespacedKey(LegacyLand.getInstance(), "attr_speed"),
                PersistentDataType.DOUBLE, attrs.getAttackSpeed());
        pdc.set(new NamespacedKey(LegacyLand.getInstance(), "attr_weight"),
                PersistentDataType.DOUBLE, attrs.getWeight());
        pdc.set(new NamespacedKey(LegacyLand.getInstance(), "attr_max_temp"),
                PersistentDataType.DOUBLE, attrs.getMaxTemperature());

        // 设置原生 AttributeModifier（显示在原版 UI 上）
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        new NamespacedKey(LegacyLand.getInstance(), getId() + "_damage"),
                        attrs.getAttackDamage() - 1.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        new NamespacedKey(LegacyLand.getInstance(), getId() + "_speed"),
                        attrs.getAttackSpeed() - 4.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

        item.setItemMeta(meta);
    }

    /**
     * 根据玩家国家科技动态更新 Lore 警告
     */
    default void refreshLore(Player player, ItemStack item) {
        ItemAttributes attrs = getAttributes();
        if (attrs == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // 移除旧警告行
        lore.removeIf(line -> line.startsWith("§c⚠"));

        // 添加新警告（科技不足时）
        String warning = AttributeEvaluator.buildTechWarningLore(player, attrs);
        if (warning != null) lore.add(warning);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /** 玩家右键/交互时触发 */
    default void onUse(Player player, ItemStack item, PlayerInteractEvent event) {
        ItemEvents.ItemClickEvent itemClickEvent = new ItemEvents.ItemClickEvent(item,player,new Date());
        itemClickEvent.callEvent();
    }

    /** 玩家攻击实体时触发 */
    default void onAttack(Player player, ItemStack item, EntityDamageByEntityEvent event) {}
}
