package net.chen.legacyLand.war;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.events.WarEndEvent;
import net.chen.legacyLand.war.siege.SiegeWar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 战争管理器
 */
public class WarManager {
    private static WarManager instance;
    private final Map<String, War> wars;
    private final Map<UUID, String> playerWarMap;

    private WarManager() {
        this.wars = new HashMap<>();
        this.playerWarMap = new HashMap<>();
    }

    public static WarManager getInstance() {
        if (instance == null) {
            instance = new WarManager();
        }
        return instance;
    }

    /**
     * 创建战争
     */
    public War createWar(WarType type,String warname, String attackerNation, String defenderNation,
                         String attackerTown, String defenderTown) {
        War war = new War(warname, type, attackerNation, defenderNation, attackerTown, defenderTown);
        wars.put(warname, war);

        // 触发事件
        net.chen.legacyLand.events.WarStartEvent event = new net.chen.legacyLand.events.WarStartEvent(war);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);

        // 保存到数据库
        net.chen.legacyLand.LegacyLand.getInstance().getDatabaseManager().saveWar(war);

        return war;
    }

    /**
     * 获取战争
     */
    public War getWar(String warId) {
        return wars.get(warId);
    }

    /**
     * 获取玩家参与的战争
     */
    public War getPlayerWar(UUID playerId) {
        String warId = playerWarMap.get(playerId);
        return warId != null ? wars.get(warId) : null;
    }

    /**
     * 获取玩家参与的战争
     */
    public War getPlayerWar(Player player) {
        return getPlayerWar(player.getUniqueId());
    }

    /**
     * 添加参与者到战争
     */
    public boolean addParticipant(String warId, WarParticipant participant, boolean isAttacker) {
        War war = wars.get(warId);
        if (war == null) return false;

        if (isAttacker) {
            war.addAttacker(participant);
        } else {
            war.addDefender(participant);
        }

        playerWarMap.put(participant.getPlayerId(), warId);

        // 保存参与者到数据库
        net.chen.legacyLand.LegacyLand.getInstance().getDatabaseManager().saveWarParticipant(
            warId, participant.getPlayerId(), isAttacker ? "ATTACKER" : "DEFENDER",
            participant.getRole().name(), participant.getSupplies());

        return true;
    }

    /**
     * 移除参与者
     */
    public void removeParticipant(UUID playerId) {
        playerWarMap.remove(playerId);
    }

    /**
     * 开始战争
     */
    public boolean startWar(String warId) {
        War war = wars.get(warId);
        if (war == null) return false;

        // 检查前哨战是否准备好
        if (!war.isOutpostReady()) {
            return false;
        }

        war.start();
        return true;
    }

    /**
     * 结束战争
     */
    public void endWar(String warId, WarStatus endStatus, String winner, String loser) {
        War war = wars.get(warId);
        if (war == null) return;

        war.end(endStatus, winner, loser);

        // 触发事件
        WarEndEvent event = new WarEndEvent(
            war, endStatus, winner, loser);
        Bukkit.getPluginManager().callEvent(event);

        // 保存到数据库
        LegacyLand.getInstance().getDatabaseManager().saveWar(war);

        // 恢复所有参与者的游戏模式为生存
        restoreParticipantsGameMode(war);

        // 清理玩家映射
        war.getAttackers().keySet().forEach(playerWarMap::remove);
        war.getDefenders().keySet().forEach(playerWarMap::remove);
    }

    /**
     * 恢复所有参与者的游戏模式为生存
     */
    private void restoreParticipantsGameMode(War war) {
        // 恢复攻击方
        for (UUID playerId : war.getAttackers().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage("§a战争已结束，你的游戏模式已恢复为生存模式。");
                }
            }
        }

        // 恢复防守方
        for (UUID playerId : war.getDefenders().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage("§a战争已结束，你的游戏模式已恢复为生存模式。");
                }
            }
        }
    }

    /**
     * 投降
     */
    public boolean surrender(String warId, boolean isAttacker) {
        War war = wars.get(warId);
        if (war == null || war.getStatus().isEnded()) return false;

        String winner = isAttacker ? war.getDefenderNation() : war.getAttackerNation();
        String loser = isAttacker ? war.getAttackerNation() : war.getDefenderNation();

        endWar(warId, WarStatus.ENDED_SURRENDER, winner, loser);
        return true;
    }

    /**
     * 和谈（平局）
     */
    public boolean makePeace(String warId) {
        War war = wars.get(warId);
        if (war == null || war.getStatus().isEnded()) return false;

        endWar(warId, WarStatus.ENDED_DRAW, null, null);
        return true;
    }

    /**
     * 检查战争胜负
     */
    public void checkWarConditions(String warId, SiegeWar siegeWar) {
        War war = wars.get(warId);
        if (war == null || war.getStatus().isEnded()) return;

        // 检查是否超过1小时，强制平局
        if (war.isOverOneHour()) {
            endWar(warId, WarStatus.ENDED_DRAW, null, null);
            return;
        }

        // 检查防守方是否失败（所有核心被摧毁）
        if (siegeWar.areAllCoresDestroyed()) {
            endWar(warId, WarStatus.ENDED_VICTORY, war.getAttackerNation(), war.getDefenderNation());
            return;
        }

        // 检查进攻方是否失败（补给线被切断或前线无士兵）
        if (siegeWar.isSupplyLineCut() || war.getActiveAttackerCount() == 0) {
            endWar(warId, WarStatus.ENDED_DEFEAT, war.getDefenderNation(), war.getAttackerNation());
        }
    }

    /**
     * 获取所有进行中的战争
     */
    public Collection<War> getActiveWars() {
        return wars.values().stream()
                .filter(war -> !war.getStatus().isEnded())
                .toList();
    }

    /**
     * 获取所有战争
     */
    public Collection<War> getAllWars() {
        return Collections.unmodifiableCollection(wars.values());
    }

    /**
     * 获取国家参与的战争
     */
    public List<War> getNationWars(String nationName) {
        return wars.values().stream()
                .filter(war -> war.getAttackerNation().equals(nationName) ||
                              war.getDefenderNation().equals(nationName))
                .toList();
    }

    /**
     * 获取城镇参与的战争
     */
    public List<War> getTownWars(String townName) {
        return wars.values().stream()
                .filter(war -> war.getAttackerTown().equals(townName) ||
                              war.getDefenderTown().equals(townName))
                .toList();
    }

    /**
     * 检查两个国家是否在战争中
     */
    public boolean isAtWar(String nation1, String nation2) {
        return wars.values().stream()
                .anyMatch(war -> !war.getStatus().isEnded() &&
                        ((war.getAttackerNation().equals(nation1) && war.getDefenderNation().equals(nation2)) ||
                         (war.getAttackerNation().equals(nation2) && war.getDefenderNation().equals(nation1))));
    }
}
