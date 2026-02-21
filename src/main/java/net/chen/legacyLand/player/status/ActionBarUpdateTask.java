package net.chen.legacyLand.player.status;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * ActionBar 更新任务
 * 每0.5秒更新一次所有在线玩家的 ActionBar 显示
 */
public class ActionBarUpdateTask extends BukkitRunnable {

    private final PlayerManager playerManager;
    private final TemperatureManager temperatureManager;
    private final PlayerStatusManager statusManager;

    public ActionBarUpdateTask() {
        this.playerManager = PlayerManager.getInstance();
        this.temperatureManager = TemperatureManager.getInstance();
        this.statusManager = PlayerStatusManager.getInstance();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
            if (playerData == null) {
                continue;
            }

            // 构建 ActionBar 消息
            Component message = buildActionBarMessage(player, playerData);
            player.sendActionBar(message);
        }
    }

    /**
     * 构建 ActionBar 消息
     */
    private Component buildActionBarMessage(Player player, PlayerData playerData) {
        Component message = Component.empty();

        // 生命值显示
        message = message.append(buildHealthDisplay(player));
        message = message.append(Component.text(" | ", NamedTextColor.DARK_GRAY));

        // 饱食度显示
        message = message.append(buildFoodDisplay(player));
        message = message.append(Component.text(" | ", NamedTextColor.DARK_GRAY));

        // 饮水值显示
        message = message.append(buildHydrationDisplay(playerData));
        message = message.append(Component.text(" | ", NamedTextColor.DARK_GRAY));

        // 体温显示
        message = message.append(buildTemperatureDisplay(playerData));

        // 状态显示（如果有异常状态）
        Component statusDisplay = buildStatusDisplay(player);
        if (statusDisplay != null) {
            message = message.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
            message = message.append(statusDisplay);
        }

        return message;
    }

    /**
     * 构建生命值显示
     */
    private Component buildHealthDisplay(Player player) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double healthPercent = health / maxHealth;

        TextColor color;
        if (healthPercent > 0.6) {
            color = NamedTextColor.GREEN;
        } else if (healthPercent > 0.3) {
            color = NamedTextColor.YELLOW;
        } else {
            color = NamedTextColor.RED;
        }

        return Component.text("❤ ", color)
                .append(Component.text(String.format("%.1f", health), color))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", maxHealth), NamedTextColor.GRAY));
    }

    /**
     * 构建饱食度显示
     */
    private Component buildFoodDisplay(Player player) {
        int foodLevel = player.getFoodLevel();
        double foodPercent = foodLevel / 20.0;

        TextColor color;
        if (foodPercent > 0.6) {
            color = NamedTextColor.GREEN;
        } else if (foodPercent > 0.3) {
            color = NamedTextColor.YELLOW;
        } else {
            color = NamedTextColor.RED;
        }

        return Component.text("🍖 ", color)
                .append(Component.text(foodLevel, color))
                .append(Component.text("/20", NamedTextColor.GRAY));
    }

    /**
     * 构建饮水值显示
     */
    private Component buildHydrationDisplay(PlayerData playerData) {
        int hydration = playerData.getHydration();
        double hydrationPercent = hydration / 10.0;

        TextColor color;
        if (hydrationPercent > 0.6) {
            color = NamedTextColor.AQUA;
        } else if (hydrationPercent > 0.3) {
            color = NamedTextColor.YELLOW;
        } else {
            color = NamedTextColor.RED;
        }

        return Component.text("💧 ", color)
                .append(Component.text(hydration, color))
                .append(Component.text("/10", NamedTextColor.GRAY));
    }

    /**
     * 构建体温显示
     */
    private Component buildTemperatureDisplay(PlayerData playerData) {
        double temperature = playerData.getTemperature();
        String tempColor = temperatureManager.getTemperatureColor(temperature);

        TextColor color;
        if (temperature <= 0) {
            color = NamedTextColor.DARK_BLUE;
        } else if (temperature <= 15) {
            color = NamedTextColor.BLUE;
        } else if (temperature <= 27) {
            color = NamedTextColor.GREEN;
        } else if (temperature <= 35) {
            color = NamedTextColor.GOLD;
        } else {
            color = NamedTextColor.RED;
        }

        String icon = temperature > 27 ? "🔥" : (temperature < 15 ? "❄" : "🌡");

        return Component.text(icon + " ", color)
                .append(Component.text(String.format("%.1f°C", temperature), color));
    }

    /**
     * 构建状态显示（仅显示异常状态）
     */
    private Component buildStatusDisplay(Player player) {
        BodyStatus bodyStatus = statusManager.getPlayerBodyStatus().get(player.getUniqueId());

        // 只显示异常状态
        if (bodyStatus != null && bodyStatus != BodyStatus.NORMAL) {
            TextColor color = NamedTextColor.RED;

            // 根据状态类型选择颜色
            switch (bodyStatus) {
                case JOYFUL -> color = NamedTextColor.LIGHT_PURPLE;
                case NERVOUS -> color = NamedTextColor.YELLOW;
                default -> color = NamedTextColor.RED;
            }

            return Component.text("⚠ ", color)
                    .append(Component.text(bodyStatus.getDisplayName(), color));
        }

        return null;
    }
}
