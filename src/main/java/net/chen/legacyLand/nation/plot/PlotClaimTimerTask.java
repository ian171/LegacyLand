package net.chen.legacyLand.nation.plot;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * PlotClaim 定时任务
 * 每秒更新占领进度
 */
public class PlotClaimTimerTask implements Runnable {

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

            // 更新 BossBar 进度
            Player player = Bukkit.getPlayer(claim.getPlayerId());
            if (player != null) {
                double percentage = (double) claim.getProgress() / claim.getRequiredSeconds();
                updateBossBar(claim, player, percentage);
            }

            // 检查是否完成
            if (claim.getProgress() >= claim.getRequiredSeconds()) {
                removeBossBar(claim);
                manager.completeClaim(claim);
            }
        }
    }

    /**
     * 更新占领进度条
     */
    private void updateBossBar(PlotClaim claim, Player player, double percentage) {
        BossBar bar = claim.getBossBar();
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    "§e占领进度",
                    BarColor.RED,
                    BarStyle.SEGMENTED_12
            );
            claim.setBossBar(bar);
        }

        bar.setProgress(Math.min(percentage, 1.0));
        bar.setTitle("§e占领进度 §f" + (int) (percentage * 100) + "%");

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        bar.setVisible(true);
    }

    /**
     * 移除占领进度条
     */
    public static void removeBossBar(PlotClaim claim) {
        BossBar bar = claim.getBossBar();
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
            claim.setBossBar(null);
        }
    }
}
