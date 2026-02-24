package net.chen.legacyLand.war.flagwar;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

/**
 * FlagWar 数据模型
 * 存储单次旗帜战争的所有信息
 */
@Data
public class FlagWarData {
    private final String flagWarId;
    private final UUID attackerId;
    private final String attackerNation;
    private final String attackerTown;
    private final String defenderNation;
    private final String defenderTown;

    // 旗帜位置
    private final Location flagLocation;
    private final Location timerBlockLocation;
    private final Location beaconLocation;

    // 时间信息
    private final long startTime;
    private long endTime;

    // 状态
    private FlagWarStatus status;
    private int timerProgress; // 0-100，表示计时器进度

    // 经济成本
    private double stakingFee;
    private double defenseBreakFee;
    private double victoryCost;

    // 地块信息
    private String townBlockCoords;
    private boolean isHomeBlock;

    public FlagWarData(String flagWarId, UUID attackerId, String attackerNation, String attackerTown,
                       String defenderNation, String defenderTown, Location flagLocation) {
        this.flagWarId = flagWarId;
        this.attackerId = attackerId;
        this.attackerNation = attackerNation;
        this.attackerTown = attackerTown;
        this.defenderNation = defenderNation;
        this.defenderTown = defenderTown;
        this.flagLocation = flagLocation;
        this.timerBlockLocation = flagLocation.clone().add(0, 1, 0);
        this.beaconLocation = flagLocation.clone().add(0, 50, 0);
        this.startTime = System.currentTimeMillis();
        this.status = FlagWarStatus.ACTIVE;
        this.timerProgress = 0;
    }

    /**
     * 获取当前计时器应该显示的羊毛颜色
     */
    public Material getTimerWoolColor() {
        if (timerProgress < 25) {
            return Material.LIME_WOOL; // 绿色
        } else if (timerProgress < 50) {
            return Material.YELLOW_WOOL; // 黄色
        } else if (timerProgress < 75) {
            return Material.ORANGE_WOOL; // 橙色
        } else {
            return Material.RED_WOOL; // 红色
        }
    }

    /**
     * 检查是否已完成（计时器达到100%）
     */
    public boolean isCompleted() {
        return timerProgress >= 100;
    }

    /**
     * 获取战争持续时间（秒）
     */
    public long getDuration() {
        if (endTime == 0) {
            return (System.currentTimeMillis() - startTime)/1000;
        }
        return (endTime - startTime)/1000;
    }

    /**
     * 结束战争
     */
    public void end(FlagWarStatus endStatus) {
        this.status = endStatus;
        this.endTime = System.currentTimeMillis();
    }
}
