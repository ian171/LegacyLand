package net.chen.legacyLand.resource.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.resource.*;
import net.chen.legacyLand.util.FoliaScheduler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * 工业精炼监听器
 * 限制高纯度资源只能在国家熔炉精炼
 */
public class IndustrialRefineListener implements Listener {

    private final LegacyLand plugin;
    private final Logger logger;
    private final Economy economy;
    private final IndustrialZoneManager zoneManager;

    // 精炼税率（5%）
    private static final double REFINE_TAX_RATE = 0.05;

    public IndustrialRefineListener(LegacyLand plugin, Economy economy) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.economy = economy;
        this.zoneManager = IndustrialZoneManager.getInstance();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();

        // 检查是否是高纯度资源
        if (!ResourceItemFactory.isHighPurityResource(source)) {
            return;
        }

        Location furnaceLocation = event.getBlock().getLocation();
        ResourceType resourceType = ResourceItemFactory.getResourceType(source);

        // 检查是否在国家熔炉区域内
        Optional<IndustrialZoneManager.IndustrialZone> zone = zoneManager.getZoneAt(furnaceLocation);

        if (zone.isEmpty() || !zone.get().canRefine(resourceType)) {
            // 不在国家熔炉内，取消精炼
            event.setCancelled(true);

            // 通知附近玩家
            notifyNearbyPlayers(furnaceLocation,
                    "§c§l[工业系统] §c" + resourceType.getColoredName() + " §c只能在国家熔炉中精炼！");

            return;
        }

        // 在国家熔炉内，允许精炼
        String nationName = zone.get().getNationName();

        // 异步处理税收扣除（避免阻塞主线程）
        FoliaScheduler.runTaskGlobal(plugin, () -> {
            try {
                // 查找最近的玩家（作为操作者）
                Player nearestPlayer = findNearestPlayer(furnaceLocation, 10.0);

                if (nearestPlayer != null) {
                    handleRefineTax(nearestPlayer, nationName, resourceType);
                }

            } catch (Exception e) {
                logger.warning("处理精炼税收时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理精炼税收
     */
    private void handleRefineTax(Player player, String nationName, ResourceType resourceType) {
        // 获取玩家余额
        double balance = economy.getBalance(player);

        // 计算税费（基于资源价值）
        double resourceValue = getResourceValue(resourceType);
        double taxAmount = resourceValue * REFINE_TAX_RATE;

        if (balance < taxAmount) {
            player.sendMessage("§c§l[工业系统] §c余额不足，无法支付精炼税（需要 $" + String.format("%.2f", taxAmount) + "）");
            return;
        }

        // 扣除玩家税费
        economy.withdrawPlayer(player, taxAmount);

        // 转入国家账户
        Nation nation = TownyAPI.getInstance().getNation(nationName);
        if (nation != null) {
            try {
                nation.getAccount().deposit(taxAmount, "精炼税收入");
            } catch (Exception e) {
                logger.warning("转入国家账户失败: " + e.getMessage());
            }
        }

        // 通知玩家
        player.sendMessage("§a§l[工业系统] §a精炼成功！已支付精炼税 §e$" + String.format("%.2f", taxAmount));
        player.sendMessage("§7税费已转入 " + nationName + " 国库");
    }

    /**
     * 获取资源价值（用于计算税费）
     */
    private double getResourceValue(ResourceType resourceType) {
        return switch (resourceType) {
            case HIGH_PURITY_IRON -> 50.0;
            case HIGH_PURITY_GOLD -> 100.0;
            case HIGH_PURITY_COPPER -> 30.0;
            default -> 10.0;
        };
    }

    /**
     * 查找最近的玩家
     */
    private Player findNearestPlayer(Location location, double radius) {
        return location.getWorld().getNearbyEntities(location, radius, radius, radius)
                .stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .findFirst()
                .orElse(null);
    }

    /**
     * 通知附近玩家
     */
    private void notifyNearbyPlayers(Location location, String message) {
        location.getWorld().getNearbyEntities(location, 10, 10, 10)
                .stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(player -> player.sendMessage(message));
    }
}
