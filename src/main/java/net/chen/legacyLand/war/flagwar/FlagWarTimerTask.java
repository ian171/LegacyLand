package net.chen.legacyLand.war.flagwar;

import net.chen.legacyLand.LegacyLand;

import java.util.ArrayList;
import java.util.List;

/**
 * FlagWar 计时器任务
 * 每秒更新所有活跃 FlagWar 的计时器进度
 */
public class FlagWarTimerTask implements Runnable {

    private final FlagWarManager flagWarManager;
    // 默认战争持续时间（秒），可配置
    private final int warDurationSeconds;

    public FlagWarTimerTask() {
        this.flagWarManager = FlagWarManager.getInstance();
        this.warDurationSeconds = LegacyLand.getInstance().getConfig().getInt("flagwar.war-duration-seconds", 300); // 默认5分钟
    }

    @Override
    public void run() {
        List<FlagWarData> toProcess = new ArrayList<>(flagWarManager.getActiveFlagWars());

        for (FlagWarData flagWar : toProcess) {
            if (flagWar.getStatus().isEnded()) {
                continue;
            }

            // 计算进度百分比
            long elapsed = System.currentTimeMillis() - flagWar.getStartTime();
            int progress = (int) ((elapsed / 1000.0 / warDurationSeconds) * 100);
            progress = Math.min(progress, 100);

            flagWarManager.updateTimerProgress(flagWar, progress);
        }
    }
}
