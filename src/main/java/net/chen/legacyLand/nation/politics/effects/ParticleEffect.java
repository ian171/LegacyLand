package net.chen.legacyLand.nation.politics.effects;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.politics.PoliticalEffect;
import net.chen.legacyLand.util.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子效果 - 为国家领导者显示粒子图案
 */
public class ParticleEffect implements PoliticalEffect {

    private final Particle particle;
    private final ParticlePattern pattern;
    private final TownyAPI townyAPI;
    private final Map<String, FoliaScheduler.TaskHandle> activeTasks = new ConcurrentHashMap<>();

    public ParticleEffect(Particle particle, ParticlePattern pattern) {
        this.particle = particle;
        this.pattern = pattern;
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public String getId() {
        return "particle-effect";
    }

    @Override
    public void onApply(Nation nation) {
        // 为国王启动粒子效果
        Resident king = nation.getKing();
        if (king != null) {
            Player player = king.getPlayer();
            if (player != null && player.isOnline()) {
                startParticleEffect(player, nation.getName());
            }
        }
    }

    @Override
    public void onRemove(Nation nation) {
        // 停止国王的粒子效果
        FoliaScheduler.TaskHandle task = activeTasks.remove(nation.getName());
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "为国家领导者显示" + pattern.name() + "形状的" + particle.name() + "粒子效果";
    }

    /**
     * 为玩家启动粒子效果
     */
    public void startParticleEffect(Player player, String nationName) {
        // 如果已有任务，先取消
        FoliaScheduler.TaskHandle existingTask = activeTasks.get(nationName);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // 每秒显示一次粒子效果
        FoliaScheduler.TaskHandle task = FoliaScheduler.runTaskTimerAtLocation(
                LegacyLand.getInstance(), player.getLocation(), () -> {
            if (!player.isOnline()) {
                stopParticleEffect(nationName);
                return;
            }

            Location loc = player.getLocation().add(0, 0.1, 0); // 在玩家脚下
            spawnParticlePattern(loc, particle, pattern);
        }, 1L, 20L);

        activeTasks.put(nationName, task);
    }

    /**
     * 停止粒子效果
     */
    public void stopParticleEffect(String nationName) {
        FoliaScheduler.TaskHandle task = activeTasks.remove(nationName);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 根据图案生成粒子
     */
    private void spawnParticlePattern(Location center, Particle particle, ParticlePattern pattern) {
        switch (pattern) {
            case STAR -> spawnStar(center, particle);
            case CIRCLE -> spawnCircle(center, particle);
            case SQUARE -> spawnSquare(center, particle);
        }
    }

    /**
     * 生成五角星图案
     */
    private void spawnStar(Location center, Particle particle) {
        double radius = 0.5;
        int points = 5;
        double angleStep = Math.PI * 4 / points; // 五角星需要转两圈

        for (int i = 0; i < points * 2; i++) {
            double angle = i * angleStep;
            double r = (i % 2 == 0) ? radius : radius * 0.4; // 外点和内点半径不同
            double x = center.getX() + r * Math.cos(angle);
            double z = center.getZ() + r * Math.sin(angle);
            center.getWorld().spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
        }

        // 连接线段
        for (int i = 0; i < points * 2; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double r1 = (i % 2 == 0) ? radius : radius * 0.4;
            double r2 = ((i + 1) % 2 == 0) ? radius : radius * 0.4;

            double x1 = center.getX() + r1 * Math.cos(angle1);
            double z1 = center.getZ() + r1 * Math.sin(angle1);
            double x2 = center.getX() + r2 * Math.cos(angle2);
            double z2 = center.getZ() + r2 * Math.sin(angle2);

            // 在两点之间插值生成粒子
            int steps = 5;
            for (int j = 0; j <= steps; j++) {
                double t = (double) j / steps;
                double x = x1 + (x2 - x1) * t;
                double z = z1 + (z2 - z1) * t;
                center.getWorld().spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * 生成圆形图案
     */
    private void spawnCircle(Location center, Particle particle) {
        double radius = 0.5;
        int points = 30;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            center.getWorld().spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * 生成正方形图案
     */
    private void spawnSquare(Location center, Particle particle) {
        double size = 0.5;
        int pointsPerSide = 8;

        // 四条边
        for (int side = 0; side < 4; side++) {
            for (int i = 0; i <= pointsPerSide; i++) {
                double t = (double) i / pointsPerSide;
                double x = center.getX();
                double z = center.getZ();

                switch (side) {
                    case 0 -> { // 上边
                        x += -size + 2 * size * t;
                        z += size;
                    }
                    case 1 -> { // 右边
                        x += size;
                        z += size - 2 * size * t;
                    }
                    case 2 -> { // 下边
                        x += size - 2 * size * t;
                        z += -size;
                    }
                    case 3 -> { // 左边
                        x += -size;
                        z += -size + 2 * size * t;
                    }
                }

                center.getWorld().spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * 粒子图案枚举
     */
    public enum ParticlePattern {
        STAR,    // 五角星
        CIRCLE,  // 圆形
        SQUARE   // 正方形
    }
}
