package net.chen.legacyLand.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import net.chen.legacyLand.player.status.BodyStatus;
import net.chen.legacyLand.player.status.PlayerStatusManager;
import net.chen.legacyLand.player.status.TemperatureManager;
import net.chen.legacyLand.season.SeasonManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * LegacyLand PlaceholderAPI 扩展
 * 提供玩家状态、季节等变量
 */
public class LegacyLandPlaceholder extends PlaceholderExpansion {

    private final LegacyLand plugin;
    private final PlayerManager playerManager;
    private final TemperatureManager temperatureManager;
    private final PlayerStatusManager statusManager;
    private final SeasonManager seasonManager;

    public LegacyLandPlaceholder(LegacyLand plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.playerManager = PlayerManager.getInstance();
        this.temperatureManager = TemperatureManager.getInstance();
        this.statusManager = PlayerStatusManager.getInstance();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "legacyland";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Chen";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "";
        }

        // 玩家状态相关
        switch (params.toLowerCase()) {
            // 生命值
            case "health":
                return String.format("%.1f", player.getHealth());
            case "health_max":
                return String.format("%.1f", player.getMaxHealth());
            case "health_percent":
                return String.format("%.0f", (player.getHealth() / player.getMaxHealth()) * 100);
            case "health_icon":
                return "❤";
            case "health_color":
                double healthPercent = player.getHealth() / player.getMaxHealth();
                if (healthPercent > 0.6) return "§a";
                else if (healthPercent > 0.3) return "§e";
                else return "§c";

            // 饱食度
            case "food":
                return String.valueOf(player.getFoodLevel());
            case "food_max":
                return "20";
            case "food_percent":
                return String.format("%.0f", (player.getFoodLevel() / 20.0) * 100);
            case "food_icon":
                return "🍖";
            case "food_color":
                double foodPercent = player.getFoodLevel() / 20.0;
                if (foodPercent > 0.6) return "§a";
                else if (foodPercent > 0.3) return "§e";
                else return "§c";

            // 饮水值
            case "hydration":
                return String.valueOf(playerData.getHydration());
            case "hydration_max":
                return "10";
            case "hydration_percent":
                return String.format("%.0f", (playerData.getHydration() / 10.0) * 100);
            case "hydration_icon":
                return "💧";
            case "hydration_color":
                double hydrationPercent = playerData.getHydration() / 10.0;
                if (hydrationPercent > 0.6) return "§b";
                else if (hydrationPercent > 0.3) return "§e";
                else return "§c";

            // 体温
            case "temperature":
                return String.format("%.1f", playerData.getTemperature());
            case "temperature_icon":
                double temp = playerData.getTemperature();
                if (temp > 27) return "🔥";
                else if (temp < 15) return "❄";
                else return "🌡";
            case "temperature_color":
                double temperature = playerData.getTemperature();
                if (temperature <= 0) return "§1";
                else if (temperature <= 15) return "§9";
                else if (temperature <= 27) return "§a";
                else if (temperature <= 35) return "§6";
                else return "§c";

            // 身体状态
            case "status":
                BodyStatus status = statusManager.getPlayerBodyStatus().get(player.getUniqueId());
                return status != null ? status.getDisplayName() : "正常";
            case "status_icon":
                BodyStatus bodyStatus = statusManager.getPlayerBodyStatus().get(player.getUniqueId());
                return (bodyStatus != null && bodyStatus != BodyStatus.NORMAL) ? "⚠" : "";
            case "status_color":
                BodyStatus bs = statusManager.getPlayerBodyStatus().get(player.getUniqueId());
                if (bs == null || bs == BodyStatus.NORMAL) return "§a";
                else if (bs == BodyStatus.JOYFUL) return "§d";
                else if (bs == BodyStatus.NERVOUS) return "§e";
                else return "§c";

            // 职业
            case "profession_main":
                return playerData.getMainProfession() != null ? playerData.getMainProfession().getDisplayName() : "无";
            case "profession_main_level":
                return String.valueOf(playerData.getMainProfessionLevel());
            case "profession_sub":
                return playerData.getSubProfession() != null ? playerData.getSubProfession().getDisplayName() : "无";
            case "profession_sub_level":
                return String.valueOf(playerData.getSubProfessionLevel());

            // 季节相关
            case "season":
                return seasonManager.getCurrentSeason().getDisplayName();
            case "season_type":
                return seasonManager.getCurrentSeason().getType().getDisplayName();
            case "season_day":
                return String.valueOf(seasonManager.getCurrentDay());
            case "season_day_max":
                return String.valueOf(seasonManager.getDaysPerSubSeason());
            case "season_progress":
                return String.format("%.0f", seasonManager.getSeasonProgress());
            case "season_base_temp":
                return String.format("%.1f", seasonManager.getCurrentSeason().getBaseTemperature());

            default:
                return null;
        }
    }
}
