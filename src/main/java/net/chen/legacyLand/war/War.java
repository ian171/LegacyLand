package net.chen.legacyLand.war;

import lombok.Data;
import org.bukkit.Location;

import java.util.*;

/**
 * 战争数据模型
 */
@Data
public class War {
    private final String warName;
    private final WarType type;
    private final String attackerNation;
    private final String defenderNation;
    private final String attackerTown;
    private final String defenderTown;

    private WarStatus status;
    private long startTime;
    private long endTime;

    // 参与者
    private final Map<UUID, WarParticipant> attackers;
    private final Map<UUID, WarParticipant> defenders;

    // 支援城市
    private final Set<String> attackerSupportTowns;
    private final Set<String> defenderSupportTowns;

    // 前哨战位置
    private Location outpostLocation;
    private long outpostEstablishTime;
    private boolean outpostValid;

    // 战争结果
    private String winner;
    private String loser;

    public War(String warName, WarType type, String attackerNation, String defenderNation,
               String attackerTown, String defenderTown) {
        this.warName = warName;
        this.type = type;
        this.attackerNation = attackerNation;
        this.defenderNation = defenderNation;
        this.attackerTown = attackerTown;
        this.defenderTown = defenderTown;
        this.status = WarStatus.PREPARING;
        this.attackers = new HashMap<>();
        this.defenders = new HashMap<>();
        this.attackerSupportTowns = new HashSet<>();
        this.defenderSupportTowns = new HashSet<>();
        this.outpostValid = false;
    }

    /**
     * 添加攻击方参与者
     */
    public void addAttacker(WarParticipant participant) {
        attackers.put(participant.getPlayerId(), participant);
    }

    /**
     * 添加防守方参与者
     */
    public void addDefender(WarParticipant participant) {
        defenders.put(participant.getPlayerId(), participant);
    }

    /**
     * 获取活跃的攻击方人数
     */
    public int getActiveAttackerCount() {
        return (int) attackers.values().stream().filter(WarParticipant::isActive).count();
    }

    /**
     * 获取活跃的防守方人数
     */
    public int getActiveDefenderCount() {
        return (int) defenders.values().stream().filter(WarParticipant::isActive).count();
    }

    /**
     * 开始战争
     */
    public void start() {
        this.status = WarStatus.ACTIVE;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 结束战争
     */
    public void end(WarStatus endStatus, String winner, String loser) {
        this.status = endStatus;
        this.endTime = System.currentTimeMillis();
        this.winner = winner;
        this.loser = loser;
    }

    /**
     * 获取战争持续时间（毫秒）
     */
    public long getDuration() {
        if (startTime == 0) return 0;
        if (endTime == 0) return System.currentTimeMillis() - startTime;
        return endTime - startTime;
    }

    /**
     * 检查是否超过1小时
     */
    public boolean isOverOneHour() {
        return getDuration() > 3600000; // 1小时 = 3600000毫秒
    }

    /**
     * 检查前哨战是否已维持1小时
     */
    public boolean isOutpostReady() {
        if (!outpostValid || outpostEstablishTime == 0) return false;
        return System.currentTimeMillis() - outpostEstablishTime >= 3600000;
    }
}
