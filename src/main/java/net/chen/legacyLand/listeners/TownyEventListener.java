package net.chen.legacyLand.listeners;

import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.object.Town;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.config.ConfigManager;
import net.chen.legacyLand.war.siege.SiegeWarManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Towny 事件监听器
 * 监听城镇创建等事件
 */
public class TownyEventListener implements Listener {

    private final SiegeWarManager siegeWarManager;
    private final ConfigManager configManager;

    public TownyEventListener() {
        this.siegeWarManager = SiegeWarManager.getInstance();
        this.configManager = LegacyLand.getInstance().getConfigManager();
    }

    /**
     * 城镇创建事件 - 自动放置城市核心
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTownCreate(NewTownEvent event) {
        // 检查是否启用自动放置城市核心
        if (!configManager.isAutoPlaceCityCore()) {
            return;
        }

        Town town = event.getTown();

        try {
            // 获取城镇出生点
            Location spawnLocation = town.getSpawn();
            if (spawnLocation == null) {
                return;
            }

            // 在出生点下方放置城市核心（信标）
            Location coreLocation = spawnLocation.clone().subtract(0, 1, 0);
            Block coreBlock = coreLocation.getBlock();

            // 检查是否可以放置
            if (coreBlock.getType() == Material.AIR || coreBlock.getType().isSolid()) {
                // 放置信标作为城市核心
                coreBlock.setType(Material.BEACON);

                // 在信标下方放置基座
                Location baseLocation = coreLocation.clone().subtract(0, 1, 0);

                // 从配置获取基座材料和大小
                Material baseMaterial = configManager.getCoreBaseMaterial();
                int baseSize = configManager.getCoreBaseSize();
                int radius = (baseSize - 1) / 2;

                // 创建基座
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block b = baseLocation.clone().add(x, 0, z).getBlock();
                        if (b.getType() == Material.AIR || !b.getType().isSolid()) {
                            b.setType(baseMaterial);
                        }
                    }
                }

                // 通知玩家
                if (event.getTown().getMayor() != null && event.getTown().getMayor().getPlayer() != null) {
                    String prefix = configManager.getMessagePrefix();
                    event.getTown().getMayor().getPlayer().sendMessage(prefix + "§a城市核心已自动放置在城镇出生点！");
                    event.getTown().getMayor().getPlayer().sendMessage(prefix + "§e城市核心是战争中的重要目标，请妥善保护！");
                }
            }
        } catch (Exception ignore) {
        }
    }
}
