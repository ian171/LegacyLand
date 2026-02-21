package net.chen.legacyLand.player.listeners;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import net.chen.legacyLand.player.status.TemperatureManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家事件监听器
 */
public class PlayerEventListener implements Listener {

    private final PlayerManager playerManager;
    private final TemperatureManager temperatureManager;

    public PlayerEventListener() {
        this.playerManager = PlayerManager.getInstance();
        this.temperatureManager = TemperatureManager.getInstance();
    }

    /**
     * 玩家加入服务器
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerManager.loadPlayerData(player);

        // 设置玩家最大血量
        try {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(data.getMaxHealth());
        } catch (Exception e) {
            player.setMaxHealth(data.getMaxHealth());
        }

        // 加载玩家温度到 TemperatureManager
        temperatureManager.getPlayerTemperature().put(player.getUniqueId(), data.getTemperature());

        // 欢迎消息
        if (data.getMainProfession() == null) {
            player.sendMessage("§e欢迎来到 LegacyLand！");
            player.sendMessage("§e使用 §6/player profession main <职业> §e选择你的主职业。");
        }
    }

    /**
     * 玩家退出服务器
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerManager.savePlayerData(player.getUniqueId());
        playerManager.removePlayerData(player.getUniqueId());

        // 清理 TemperatureManager 中的温度数据
        temperatureManager.getPlayerTemperature().remove(player.getUniqueId());
    }
}
