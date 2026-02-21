package net.chen.legacyLand.player.status;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 玩家状态监听器
 */
public class PlayerStatusListener implements Listener {

    private final PlayerStatusManager statusManager;
    private final TemperatureManager temperatureManager;
    private final PlayerManager playerManager;

    public PlayerStatusListener() {
        this.statusManager = PlayerStatusManager.getInstance();
        this.temperatureManager = TemperatureManager.getInstance();
        this.playerManager = PlayerManager.getInstance();
    }

    /**
     * 监听玩家受伤事件
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();

        // 根据伤害类型应用不同的受伤状态
        switch (cause) {
            case ENTITY_ATTACK -> {
                // 检查攻击者使用的武器
                if (event.getDamager() instanceof Player attacker) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    applyWeaponInjury(player, weapon);
                }
            }
            case PROJECTILE -> {
                // 箭伤
                if (Math.random() < 0.3) { // 30%概率
                    statusManager.applyInjuryStatus(player, InjuryStatus.ARROW_WOUND);
                    // 造成额外伤害（5%-10%生命值）
                    double damage = player.getMaxHealth() * (0.05 + Math.random() * 0.05);
                    player.damage(damage);
                }
            }
            case FIRE, FIRE_TICK, LAVA -> {
                // 火伤
                if (Math.random() < 0.3) {
                    statusManager.applyInjuryStatus(player, InjuryStatus.BURN);
                }
            }
            case FALL -> {
                // 跌落伤害可能导致骨折
                if (event.getFinalDamage() >= 6.0 && Math.random() < 0.2) { // 20%概率
                    statusManager.applyLifeInjuryStatus(player, LifeInjuryStatus.FRACTURE);
                }
            }
        }
    }

    /**
     * 根据武器类型应用受伤状态
     */
    private void applyWeaponInjury(Player player, ItemStack weapon) {
        Material type = weapon.getType();

        if (type.name().contains("SWORD")) {
            // 剑类武器 - 刀伤
            if (Math.random() < 0.3) { // 30%概率
                statusManager.applyInjuryStatus(player, InjuryStatus.BLADE_WOUND);
            }
        } else if (type.name().contains("AXE")) {
            // 斧类武器 - 斧伤
            if (Math.random() < 0.3) {
                statusManager.applyInjuryStatus(player, InjuryStatus.AXE_WOUND);
                // 造成额外伤害（10%-15%生命值）
                double damage = player.getMaxHealth() * (0.10 + Math.random() * 0.05);
                player.damage(damage);
            }
        }

        // 检查武器是否有毒性附魔（可以通过ItemsAdder自定义）
        if (weapon.hasItemMeta() && weapon.getItemMeta().hasLore()) {
            String lore = String.join("", weapon.getItemMeta().getLore());
            if (lore.contains("毒性") || lore.contains("poison")) {
                if (Math.random() < 0.3) {
                    statusManager.applyInjuryStatus(player, InjuryStatus.POISON);
                }
            }
        }
    }

    /**
     * 监听玩家食用食物事件
     */
    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Material foodType = item.getType();

        // 记录食物消费
        statusManager.recordFoodConsumption(player, foodType.name());

        // 检查是否是生食
        if (isRawFood(foodType) && Math.random() < 0.1) { // 10%概率食物中毒
            statusManager.applyBodyStatus(player, BodyStatus.FOOD_POISONING);
        }

        // 检查饮水
        if (foodType == Material.POTION) {
            PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
            if (playerData != null) {
                playerData.restoreHydration(2);
            }
        }
    }

    /**
     * 监听玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // 不清理数据，保留到下次登录
    }

    /**
     * 判断是否是生食
     */
    private boolean isRawFood(Material food) {
        return food == Material.BEEF || food == Material.PORKCHOP ||
               food == Material.CHICKEN || food == Material.MUTTON ||
               food == Material.COD || food == Material.SALMON ||
               food == Material.RABBIT;
    }
}
