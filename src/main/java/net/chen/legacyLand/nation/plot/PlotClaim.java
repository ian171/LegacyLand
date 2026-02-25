package net.chen.legacyLand.nation.plot;

import com.palmergames.bukkit.towny.object.WorldCoord;
import lombok.Data;
import org.bukkit.boss.BossBar;

import java.util.UUID;

/**
 * PlotClaim 数据模型
 * 存储单次地块占领的所有信息
 */
@Data
public class PlotClaim {
    private final String claimId;
    private final UUID playerId;
    private final String townName;
    private final WorldCoord worldCoord;
    private final long startTime;
    private int progress;
    private final int requiredSeconds;
    private boolean paused;
    private String pauseReason;
    private transient BossBar bossBar;

    public PlotClaim(String claimId, UUID playerId, String townName, WorldCoord worldCoord,
                     long startTime, int progress, int requiredSeconds, boolean paused, String pauseReason) {
        this.claimId = claimId;
        this.playerId = playerId;
        this.townName = townName;
        this.worldCoord = worldCoord;
        this.startTime = startTime;
        this.progress = progress;
        this.requiredSeconds = requiredSeconds;
        this.paused = paused;
        this.pauseReason = pauseReason;
    }
}
