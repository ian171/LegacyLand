package net.chen.legacyLand.war.siege;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.events.OutpostCompletedEvent;
import net.chen.legacyLand.events.OutpostDiscoveredEvent;
import net.chen.legacyLand.war.War;
import net.chen.legacyLand.war.WarManager;
import net.chen.legacyLand.war.WarStatus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * 前哨战监控任务
 * 每分钟检查一次前哨战状态
 */
public class OutpostMonitorTask extends BukkitRunnable {

    private final LegacyLand plugin;
    private final WarManager warManager;
    private final SiegeWarManager siegeWarManager;

    public OutpostMonitorTask(LegacyLand plugin) {
        this.plugin = plugin;
        this.warManager = WarManager.getInstance();
        this.siegeWarManager = SiegeWarManager.getInstance();
    }

    @Override
    public void run() {
        // 遍历所有进行中的战争
        for (War war : warManager.getActiveWars()) {
            if (war.getStatus() != WarStatus.PREPARING) {
                continue;
            }

            SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
            if (siegeWar == null || siegeWar.getOutpost() == null) {
                continue;
            }

            Outpost outpost = siegeWar.getOutpost();
            if (!outpost.isActive()) {
                continue;
            }

            Location outpostLoc = outpost.getLocation();
            if (outpostLoc == null || outpostLoc.getWorld() == null) {
                continue;
            }

            // 检查附近的玩家
            Collection<Player> nearbyPlayers = outpostLoc.getNearbyPlayers(32);

            int allyCount = 0;
            Player enemyPlayer = null;

            for (Player player : nearbyPlayers) {
                // 检查是否是攻方成员
                if (war.getAttackers().containsKey(player.getUniqueId())) {
                    allyCount++;
                }
                // 检查是否是守方成员（敌人）
                else if (war.getDefenders().containsKey(player.getUniqueId())) {
                    enemyPlayer = player;
                    break;
                }
            }

            // 如果被敌人发现，前哨战失效
            if (enemyPlayer != null) {
                handleOutpostDiscovered(outpost, enemyPlayer, war);
                continue;
            }

            // 检查是否有足够的人维持前哨战（至少2人）
            if (allyCount < 2) {
                // 人数不足，重置计时
                outpost.resetProgress();
                continue;
            }

            // 人数足够，增加进度
            outpost.addProgress(1); // 每分钟增加1分钟进度

            // 检查是否完成（60分钟）
            if (outpost.getProgress() >= 60) {
                handleOutpostCompleted(outpost, war);
            }
        }
    }

    /**
     * 处理前哨战被发现
     */
    private void handleOutpostDiscovered(Outpost outpost, Player discoverer, War war) {
        outpost.setActive(false);

        // 触发事件
        OutpostDiscoveredEvent event = new OutpostDiscoveredEvent(outpost, discoverer, war.getWarName());
        Bukkit.getPluginManager().callEvent(event);

        // 通知双方
        notifyWarParticipants(war, "§c前哨战被敌方发现！前哨战已失效！");
        discoverer.sendMessage("§a你发现了敌方的前哨战！敌方的前哨战已失效！");

        // 保存数据
        plugin.getDatabaseManager().saveSiegeWar(siegeWarManager.getSiegeWarByWarId(war.getWarName()));
    }

    /**
     * 处理前哨战完成
     */
    private void handleOutpostCompleted(Outpost outpost, War war) {
        outpost.setCompleted(true);
        war.setStatus(WarStatus.ACTIVE);
        war.setStartTime(System.currentTimeMillis());

        // 触发事件
        OutpostCompletedEvent event = new OutpostCompletedEvent(outpost, war.getWarName());
        Bukkit.getPluginManager().callEvent(event);

        // 通知双方
        notifyWarParticipants(war, "§a前哨战已完成！战争正式开始！");

        // 保存数据
        plugin.getDatabaseManager().saveWar(war);
        plugin.getDatabaseManager().saveSiegeWar(siegeWarManager.getSiegeWarByWarId(war.getWarName()));
    }

    /**
     * 通知战争参与者
     */
    private void notifyWarParticipants(War war, String message) {
        for (java.util.UUID uuid : war.getAttackers().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
        for (java.util.UUID uuid : war.getDefenders().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}
