package net.chen.legacyLand.item.items;

import net.chen.legacyLand.item.CustomItem;
import net.chen.legacyLand.item.ItemsRegistry;
import net.chen.legacyLand.item.attribute.ItemAttributes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 自定义物品注册表 - 在此声明所有自定义物品
 */
public final class ModItems {

    // ========== 物品声明 ==========

    public static final CustomItem MERCURY_THERMOMETER = ItemsRegistry.register(
            "mercury_thermometer",
            new CustomItem() {
                @Override public String getId() { return "mercury_thermometer"; }
                @Override public int getCmd() { return 1001; }
                @Override public String getTexturePath() { return "legacyland:item/mercury_thermometer"; }
                @Override public Material getBaseMaterial() { return Material.GLASS_BOTTLE; }

                @Override
                public void onUse(Player player, ItemStack item, PlayerInteractEvent event) {
                    CustomItem.super.onUse(player, item, event);
                }
            }
    );

    public static final CustomItem BLEED_SPEAR = ItemsRegistry.register(
            "bleed_spear",
            new BleedSpear(
                    Material.IRON_SWORD,
                    10002,
                    ItemAttributes.builder()
                            .damage(6)
                            .weight(8.5)
                            .tech("INDUSTRIAL", 5)
                            .build()
            )
    );

    /** 触发所有物品的静态初始化（在插件 onEnable 中调用一次） */
    public static void init() {}

    private ModItems() {}
}

