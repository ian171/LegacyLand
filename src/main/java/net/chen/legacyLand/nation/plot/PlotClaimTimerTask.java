package net.chen.legacyLand.nation.plot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

/**
 * PlotClaim 定时任务
 * 每秒更新占领进度
 */
public class PlotClaimTimerTask extends BukkitRunnable {

    private final PlotClaimManager manager = PlotClaimManager.getInstance();

    @Override
    public void run() {
        for (PlotClaim claim : manager.getActiveClaims()) {
            // 跳过暂停的占领
            if (claim.isPaused()) {
                continue;
            }

            // 增加进度
            claim.setProgress(claim.getProgress() + 1);

            // 发送 ActionBar 进度提示
            Player player = org.bukkit.Bukkit.getPlayer(claim.getPlayerId());
            if (player != null) {
                int progress = claim.getProgress();
                int required = claim.getRequiredSeconds();
                int percentage = (progress * 100) / required;

                String bar = generateProgressBar(percentage);
                String message = String.format("§e占领进度: %s §f%d/%d 秒 (§a%d%%§f)", bar, progress, required, percentage);

                player.sendActionBar(Component.text(message));
            }

            // 检查是否完成
            if (claim.getProgress() >= claim.getRequiredSeconds()) {
                manager.completeClaim(claim);
            }
        }
    }

    /**
     * 生成进度条
     */
    private String generateProgressBar(int percentage) {
        int bars = 20;
        int filled = (percentage * bars) / 100;

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < bars; i++) {
            if (i == filled) {
                bar.append("§7");
            }
            bar.append("|");
        }

        return bar.toString();
    }
}
