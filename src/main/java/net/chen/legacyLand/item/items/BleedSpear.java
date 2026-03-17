package net.chen.legacyLand.item.items;

import net.chen.legacyLand.item.CustomItem;
import net.chen.legacyLand.item.attribute.AttributeEvaluator;
import net.chen.legacyLand.item.attribute.ItemAttributes;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 放血长矛 - 攻击时附加流血（中毒）效果
 * 科技需求：INDUSTRIAL Tier 5
 */
public class BleedSpear implements CustomItem {

    private final Material material;
    private final int cmd;
    private final ItemAttributes attributes;

    public BleedSpear(Material material, int cmd, ItemAttributes attributes) {
        this.material   = material;
        this.cmd        = cmd;
        this.attributes = attributes;
    }

    @Override public String getId(){ return "bleed_spear"; }
    @Override public int getCmd(){ return cmd; }
    @Override public Material getBaseMaterial(){ return material; }
    @Override public String getTexturePath(){ return "legacyland:item/bleed_spear"; }
    @Override public ItemAttributes getAttributes(){ return attributes; }

    @Override
    public void onAttack(Player player, ItemStack item, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 科技不足时不触发流血效果
        if (!AttributeEvaluator.meetsTechRequirement(player, attributes)) return;

        // 附加流血（用中毒模拟），持续 3 秒
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true));
        player.sendActionBar("§c⚔ 放血！");
    }
}
