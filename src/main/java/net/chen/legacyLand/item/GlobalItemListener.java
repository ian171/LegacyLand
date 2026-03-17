package net.chen.legacyLand.item;

import net.chen.legacyLand.item.attribute.ItemAttributes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import net.chen.legacyLand.LegacyLand;

/**
 * 全局物品事件分发器 + 负重系统
 */
public class GlobalItemListener implements Listener {

    // 基础移速
    private static final float BASE_WALK_SPEED = 0.2f;
    // 每超过 10 单位重量降低 5%
    private static final double WEIGHT_UNIT = 10.0;
    private static final double SPEED_PENALTY_PER_UNIT = 0.05;
    private static final float MIN_WALK_SPEED = 0.5f;

    // ========== 物品交互分发 ==========

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        CustomItem customItem = ItemsRegistry.fromStack(item);
        if (customItem == null) return;

        customItem.refreshLore(event.getPlayer(), item);
        customItem.onUse(event.getPlayer(), item, event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem customItem = ItemsRegistry.fromStack(item);
        if (customItem == null) return;

        // 应用科技效能修正到实际伤害
        ItemAttributes attrs = customItem.getAttributes();
        if (attrs != null) {
            double ratio = net.chen.legacyLand.item.attribute.AttributeEvaluator
                    .getEfficiencyRatio(player, attrs);
            if (ratio < 1.0) {
                event.setDamage(event.getDamage() * ratio);
            }
        }

        customItem.onAttack(player, item, event);
    }

    // ========== 负重系统 ==========

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        // 下一 tick 更新，确保物品已切换
        new BukkitRunnable() {
            @Override public void run() {
                updateWalkSpeed(event.getPlayer());
            }
        }.runTask(LegacyLand.getInstance());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        new BukkitRunnable() {
            @Override public void run() {
                updateWalkSpeed(player);
            }
        }.runTask(LegacyLand.getInstance());
    }

    /**
     * 计算快捷栏所有自定义物品总重量并更新移速
     * 公式：重量每超过 10，移速降低 5%
     */
    private void updateWalkSpeed(Player player) {
        double totalWeight = 0.0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            CustomItem ci = ItemsRegistry.fromStack(stack);
            if (ci == null || ci.getAttributes() == null) continue;
            totalWeight += ci.getAttributes().getWeight();
        }

        double units = totalWeight / WEIGHT_UNIT;
        double penalty = units * SPEED_PENALTY_PER_UNIT;
        float newSpeed = (float) Math.max(MIN_WALK_SPEED, BASE_WALK_SPEED * (1.0 - penalty));
        player.setWalkSpeed(newSpeed);
    }
}
