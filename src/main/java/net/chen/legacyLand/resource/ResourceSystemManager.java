package net.chen.legacyLand.resource;

import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.resource.listeners.BiomeResourceListener;
import net.chen.legacyLand.resource.listeners.IndustrialRefineListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * 资源系统管理器
 * 统一管理资源产出、精炼、物流等功能
 */
public class ResourceSystemManager {
    @Getter
    private static ResourceSystemManager instance;

    private final LegacyLand plugin;
    private final Logger logger;
    private final IndustrialZoneManager zoneManager;
    private final Economy economy;

    private ResourceSystemManager(LegacyLand plugin, Economy economy) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.economy = economy;
        this.zoneManager = IndustrialZoneManager.getInstance();
    }

    public static ResourceSystemManager getInstance(LegacyLand plugin, Economy economy) {
        if (instance == null) {
            synchronized (ResourceSystemManager.class) {
                if (instance == null) {
                    instance = new ResourceSystemManager(plugin, economy);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化资源系统
     */
    public void init() {
        // 初始化资源物品工厂
        ResourceItemFactory.init(plugin);

        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(
                new BiomeResourceListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new IndustrialRefineListener(plugin, economy), plugin);

        logger.info("资源系统已加载");
    }

    // ==================== API 接口 ====================

    /**
     * 注册工业区域
     *
     * @param nationName 国家名称
     * @param center 区域中心
     * @param radius 区域半径
     * @param type 区域类型
     */
    public void registerIndustrialZone(String nationName, Location center, double radius,
                                       IndustrialZoneManager.ZoneType type) {
        IndustrialZoneManager.IndustrialZone zone =
                new IndustrialZoneManager.IndustrialZone(nationName, center, radius, type);
        zoneManager.registerZone(nationName, zone);

        logger.info(String.format("已注册工业区域: %s - %s (半径: %.1f)",
                nationName, type.getDisplayName(), radius));
    }

    /**
     * 检查位置是否在工业区域内
     */
    public Optional<IndustrialZoneManager.IndustrialZone> getZoneAt(Location location) {
        return zoneManager.getZoneAt(location);
    }

    /**
     * 创建资源物品
     */
    public ItemStack createResourceItem(ResourceType resourceType, int amount) {
        return ResourceItemFactory.createResourceItem(resourceType, amount);
    }

    /**
     * 检查物品是否是特殊资源
     */
    public boolean isResourceItem(ItemStack item) {
        return ResourceItemFactory.isResourceItem(item);
    }

    /**
     * 获取物品的资源类型
     */
    public ResourceType getResourceType(ItemStack item) {
        return ResourceItemFactory.getResourceType(item);
    }

    /**
     * 计算运输成本
     */
    public LogisticsCalculator.TransportCost calculateTransportCost(
            Location from, Location to, ItemStack... items) {
        return LogisticsCalculator.calculateTransportCost(from, to, items);
    }

    /**
     * 计算简化运输成本
     */
    public double calculateSimpleTransportCost(Location from, Location to, double weight) {
        return LogisticsCalculator.calculateSimpleCost(from, to, weight);
    }

    /**
     * 给玩家发放资源物品
     */
    public boolean giveResourceToPlayer(Player player, ResourceType resourceType, int amount) {
        try {
            ItemStack item = createResourceItem(resourceType, amount);
            player.getInventory().addItem(item);
            player.sendMessage("§a§l[资源系统] §a你获得了 " +
                    resourceType.getColoredName() + " §ax" + amount);
            return true;
        } catch (Exception e) {
            logger.warning("给予玩家资源时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取玩家背包中的资源总重量
     */
    public double getPlayerInventoryWeight(Player player) {
        double totalWeight = 0.0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            ResourceType type = getResourceType(item);
            double weight = type != null ? type.getWeight() : 1.0;
            totalWeight += weight * item.getAmount();
        }

        return totalWeight;
    }

    /**
     * 显示运输成本预览（GUI 接口）
     */
    public void showTransportCostPreview(Player player, Location from, Location to) {
        double weight = getPlayerInventoryWeight(player);
        double cost = calculateSimpleTransportCost(from, to, weight);

        player.sendMessage("§e§l=== 运输成本预览 ===");
        player.sendMessage("§7起点: §f" + formatLocation(from));
        player.sendMessage("§7终点: §f" + formatLocation(to));
        player.sendMessage("§7距离: §f" + String.format("%.1f", from.distance(to)) + " 格");
        player.sendMessage("§7货物重量: §f" + String.format("%.2f", weight) + " 单位");
        player.sendMessage("§e总费用: §6$" + String.format("%.2f", cost));
    }

    /**
     * 格式化位置信息
     */
    private String formatLocation(Location loc) {
        return String.format("%s (%.0f, %.0f, %.0f)",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
