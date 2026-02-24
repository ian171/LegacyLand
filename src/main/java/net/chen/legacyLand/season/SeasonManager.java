package net.chen.legacyLand.season;

import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.player.status.TemperatureManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

/**
 * 季节管理器
 * 负责管理季节循环、进度和天气效果
 */
public class SeasonManager {

    private final LegacyLand plugin;
    @Getter
    private Season currentSeason;
    @Getter
    private int currentDay; // 当前子季节的第几天
    @Getter
    private int daysPerSubSeason; // 每个子季节持续天数
    private BukkitTask seasonTask;
    private long lastDayTime; // 上次检查的游戏时间

    public SeasonManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.currentSeason = Season.EARLY_SPRING;
        this.currentDay = 1;
        this.daysPerSubSeason = 8; // 默认每个子季节8天
        this.lastDayTime = 0;
    }

    /**
     * 启动季节系统
     */
    public void start() {
        // 从数据库加载季节数据
        loadSeasonData();

        // 每20 ticks (1秒) 检查一次
        seasonTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkDayProgress, 20L, 20L);
        plugin.getLogger().info("季节系统已启动 - 当前季节: " + currentSeason.getDisplayName());
    }

    /**
     * 停止季节系统
     */
    public void stop() {
        if (seasonTask != null) {
            seasonTask.cancel();
        }
        // 保存季节数据
        saveSeasonData();
    }

    /**
     * 检查日期进度
     */
    private void checkDayProgress() {
        World world = Bukkit.getWorlds().getFirst(); // 主世界
        if (world == null) return;

        long currentTime = world.getFullTime();
        long daysPassed = currentTime / 24000L;

        // 检查是否过了一天
        if (daysPassed > lastDayTime) {
            lastDayTime = daysPassed;
            advanceDay();
        }
    }

    /**
     * 切换到下一个季节
     */
    private void advanceSeason() {
        Season oldSeason = currentSeason;
        currentSeason = currentSeason.next();

        plugin.getLogger().info("季节变化: " + oldSeason.getDisplayName() + " -> " + currentSeason.getDisplayName());

        // 广播季节变化
        Bukkit.broadcastMessage("§e§l[季节] §f季节已变更为: §a" + currentSeason.getDisplayName() + " §7(" + currentSeason.getType().getDisplayName() + ")");

        // 更新所有在线玩家的温度基准
        updateAllPlayersTemperature();

        // 应用季节效果
        applySeasonEffects();
    }

    /**
     * 更新所有在线玩家的温度基准
     */
    private void updateAllPlayersTemperature() {
        TemperatureManager.getInstance().setSeasonBaseTemperature(currentSeason.getKey());
    }

    /**
     * 应用季节效果（天气、环境等）
     */
    private void applySeasonEffects() {
        World world = Bukkit.getWorlds().getFirst();
        if (world == null) return;

        // 根据季节调整天气概率
        switch (currentSeason.getType()) {
            case SPRING:
                // 春季增加降雨频率
                if (Math.random() < 0.3) {
                    world.setStorm(true);
                }
                break;
            case SUMMER:
                // 夏季晴朗
                world.setStorm(false);
                world.setThundering(false);
                break;
            case AUTUMN:
                // 秋季偶尔下雨
                if (Math.random() < 0.2) {
                    world.setStorm(true);
                }
                break;
            case WINTER:
                // 冬季下雪（通过降雨实现，在寒冷生物群系会变成雪）
                if (Math.random() < 0.4) {
                    world.setStorm(true);
                }
                break;
        }
    }

    /**
     * 手动设置季节
     */
    public void setSeason(Season season) {
        Season oldSeason = this.currentSeason;
        this.currentSeason = season;
        this.currentDay = 1;

        plugin.getLogger().info("手动设置季节: " + oldSeason.getDisplayName() + " -> " + season.getDisplayName());

        // 更新所有在线玩家的温度基准
        updateAllPlayersTemperature();

        // 应用季节效果
        applySeasonEffects();
    }

    /**
     * 设置每个子季节的持续天数
     */
    public void setDaysPerSubSeason(int days) {
        this.daysPerSubSeason = Math.max(1, days);
        plugin.getLogger().info("每个子季节持续天数已设置为: " + this.daysPerSubSeason);
    }

    /**
     * 获取季节进度百分比
     */
    public double getSeasonProgress() {
        return (double) currentDay / daysPerSubSeason * 100.0;
    }

    /**
     * 获取当前季节信息
     */
    public String getSeasonInfo() {
        return String.format("§e当前季节: §a%s §7(%s)\n§e进度: §f第%d天/%d天 §7(%.1f%%)",
                currentSeason.getDisplayName(),
                currentSeason.getType().getDisplayName(),
                currentDay,
                daysPerSubSeason,
                getSeasonProgress());
    }

    /**
     * 保存季节数据到数据库
     */
    private void saveSeasonData() {
        plugin.getDatabaseManager().saveSeasonData(
                currentSeason.getKey(),
                currentDay,
                daysPerSubSeason
        );
    }

    /**
     * 从数据库加载季节数据
     */
    private void loadSeasonData() {
        Map<String, Object> data = plugin.getDatabaseManager().loadSeasonData();
        if (data != null) {
            String seasonKey = (String) data.get("current_season");
            this.currentSeason = Season.fromKey(seasonKey);
            this.currentDay = (int) data.get("current_day");
            this.daysPerSubSeason = (int) data.get("days_per_sub_season");
            plugin.getLogger().info("已从数据库加载季节数据: " + currentSeason.getDisplayName() + " 第" + currentDay + "天");
        } else {
            plugin.getLogger().info("未找到季节数据，使用默认值");
        }
    }

    /**
     * 推进一天（同时保存数据）
     */
    private void advanceDay() {
        currentDay++;

        // 检查是否需要切换到下一个子季节
        if (currentDay > daysPerSubSeason) {
            currentDay = 1;
            advanceSeason();
        }

        Bukkit.getServer().sendMessage(Component.text("季节进度: " + currentSeason.getDisplayName() + " 第" + currentDay + "天"));

        // 保存到数据库
        saveSeasonData();
    }
}
