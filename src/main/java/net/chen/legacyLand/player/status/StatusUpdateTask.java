package net.chen.legacyLand.player.status;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 状态更新任务
 * 每5秒检查一次所有在线玩家的状态
 */
public class StatusUpdateTask implements Runnable {

    private final PlayerStatusManager statusManager;
    private final TemperatureManager temperatureManager;
    private final PlayerManager playerManager;

    public StatusUpdateTask() {
        this.statusManager = PlayerStatusManager.getInstance();
        this.temperatureManager = TemperatureManager.getInstance();
        this.playerManager = PlayerManager.getInstance();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
            if (playerData == null) {
                continue;
            }
            // 更新温度
            temperatureManager.updatePlayerTemperature(player, playerData);

            // 检查并应用身体状态
            statusManager.checkAndApplyBodyStatus(player, playerData);

            // 检查饮水值（疾跑累计时间超过2min后，每两分钟掉一滴水滴）
            if (player.isSprinting()) {
                //TODO: 实现计时器
                if (Math.random() < 0.01) { // 小概率触发
                    playerData.consumeHydration(1);
                    if (playerData.getHydration() <= 0) {
                        player.sendMessage("§c你口渴了！需要喝水！");
                    }
                }
            }
        }
    }
}
