package net.chen.legacyLand.listeners;

import net.chen.legacyLand.war.War;
import net.chen.legacyLand.war.WarManager;
import net.chen.legacyLand.war.WarParticipant;
import net.chen.legacyLand.war.siege.SiegeWar;
import net.chen.legacyLand.war.siege.SiegeWarManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * 战争事件监听器
 */
public class WarEventListener implements Listener {

    private final WarManager warManager;
    private final SiegeWarManager siegeWarManager;

    public WarEventListener() {
        this.warManager = WarManager.getInstance();
        this.siegeWarManager = SiegeWarManager.getInstance();
    }

    /**
     * 玩家死亡事件 - 消耗补给
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        War war = warManager.getPlayerWar(player);

        if (war == null || war.getStatus().isEnded()) {
            return;
        }

        // 获取参与者
        WarParticipant participant = war.getAttackers().get(player.getUniqueId());
        if (participant == null) {
            participant = war.getDefenders().get(player.getUniqueId());
        }

        if (participant == null) {
            return;
        }

        // 消耗一份补给
        if (!participant.consumeSupply(1)) {
            player.sendMessage("§c你的补给已耗尽，退出战场！");
            participant.setActive(false);
        } else {
            player.sendMessage("§e你已阵亡，消耗1份补给。剩余补给: §f" + participant.getSupplies());
        }
    }

    /**
     * 玩家重生事件 - 部署到核心或前哨战
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        War war = warManager.getPlayerWar(player);

        if (war == null || war.getStatus().isEnded()) {
            return;
        }

        // 获取参与者
        WarParticipant participant = war.getAttackers().get(player.getUniqueId());
        boolean isAttacker = participant != null;

        if (participant == null) {
            participant = war.getDefenders().get(player.getUniqueId());
        }

        if (participant == null || !participant.isActive()) {
            // 设置为旁观模式
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§c你已退出战争，处于旁观模式。");
            return;
        }

        // 获取攻城战信息
        SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
        if (siegeWar == null) {
            return;
        }

        // 重生到城市核心
        String townName = isAttacker ? war.getAttackerTown() : war.getDefenderTown();
        Location coreLocation = siegeWar.getCityCores().get(townName + "_core");

        if (coreLocation != null) {
            event.setRespawnLocation(coreLocation);
            player.sendMessage("§a你已重生到城市核心。");
        }
    }

    /**
     * 玩家移动事件 - 检查战争区域权限
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null) return;

        // 检查所有进行中的战争
        for (War war : warManager.getActiveWars()) {
            SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
            if (siegeWar == null) continue;

            // 检查是否在战争区
            if (siegeWar.isInWarZone(to)) {
                if (!isWarParticipant(player, war, siegeWar)) {
                    // 非参战人员，设置为旁观模式
                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.sendMessage("§c你进入了战争区域，已切换为旁观模式。");
                    }
                    return;
                }
            }

            // 检查是否在前线
            if (siegeWar.isInFrontline(to)) {
                WarParticipant participant = war.getAttackers().get(player.getUniqueId());
                if (participant == null) {
                    participant = war.getDefenders().get(player.getUniqueId());
                }

                if (participant == null) {
                    // 非参战人员
                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.sendMessage("§c你进入了前线区域，已切换为旁观模式。");
                    }
                    return;
                }

                // 检查是否是战士或后勤兵
                if (participant.getRole() != net.chen.legacyLand.war.WarRole.SOLDIER &&
                    participant.getRole() != net.chen.legacyLand.war.WarRole.LOGISTICS) {
                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.sendMessage("§c只有战士和后勤兵可以进入前线！");
                    }
                }
            }
        }
    }

    /**
     * 检查玩家是否是战争参与者
     */
    private boolean isWarParticipant(Player player, War war, SiegeWar siegeWar) {
        WarParticipant participant = war.getAttackers().get(player.getUniqueId());
        if (participant != null) return true;

        participant = war.getDefenders().get(player.getUniqueId());
        return participant != null;
    }
}
