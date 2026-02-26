package net.chen.legacyLand.config;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 配置管理器(未启用）
 */
public class ConfigManager {
    private final LegacyLand plugin;
    private FileConfiguration config;

    public ConfigManager(LegacyLand plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        // 保存默认配置（如果不存在）
        plugin.saveDefaultConfig();

        // 加载现有配置
        plugin.reloadConfig();
        config = plugin.getConfig();

        // 更新配置文件（添加新配置项但保留现有值和注释）
        updateConfig();

        // 加载温度变化速率
        double tempChangeRate = config.getDouble("player-status.temperature-change-rate", 0.5);
        net.chen.legacyLand.player.status.TemperatureManager.getInstance().setTemperatureChangeRate(tempChangeRate);

        plugin.getLogger().info("配置文件已加载");
    }

    /**
     * 更新配置文件（添加缺失的配置项）
     */
    private void updateConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // 加载默认配置
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream == null) {
            return;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

        boolean updated = false;

        // 检查并添加缺失的配置项
        for (String key : defaultConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
                updated = true;
                plugin.getLogger().info("添加新配置项: " + key);
            }
        }

        // 如果有更新，保存配置文件
        if (updated) {
            try {
                config.save(configFile);
                plugin.getLogger().info("配置文件已更新，添加了新的配置项");
            } catch (IOException e) {
                plugin.getLogger().severe("保存配置文件失败: " + e.getMessage());
            }
        }

        try {
            defaultConfigStream.close();
        } catch (IOException e) {
            plugin.getLogger().warning("关闭默认配置流失败: " + e.getMessage());
        }
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // ========== 数据库配置 ==========
    public boolean isDev = config.getBoolean("experimental.devmode");

    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getDatabaseFilename() {
        return config.getString("database.filename", "legacyland.db");
    }

    // ========== 国家系统配置 ==========

    public double getNationCreationCost() {
        return config.getDouble("nation.creation-cost", 1000.0);
    }

    public int getNationNameMinLength() {
        return config.getInt("nation.name-min-length", 2);
    }

    public int getNationNameMaxLength() {
        return config.getInt("nation.name-max-length", 20);
    }

    public double getNationInitialTreasury() {
        return config.getDouble("nation.initial-treasury", 0.0);
    }

    public boolean isNationDeletionAllowed() {
        return config.getBoolean("nation.allow-deletion", true);
    }

    // ========== 税收系统配置 ==========

    public double getDefaultTaxRate(String taxType) {
        return config.getDouble("tax.default." + taxType, 0.0);
    }

    public double getMaxTaxRate() {
        return config.getDouble("tax.max-rate", 100.0);
    }

    public double getMinTaxRate() {
        return config.getDouble("tax.min-rate", 0.0);
    }

    // ========== 外交系统配置 ==========

    public int getWarCooldown() {
        return config.getInt("diplomacy.war-cooldown", 86400);
    }

    public int getAllianceCooldown() {
        return config.getInt("diplomacy.alliance-cooldown", 3600);
    }

    public boolean isMultipleAlliancesAllowed() {
        return config.getBoolean("diplomacy.allow-multiple-alliances", true);
    }

    public int getMaxAlliances() {
        return config.getInt("diplomacy.max-alliances", 5);
    }

    // ========== 战争系统配置 ==========

    public boolean isWarEnabled() {
        return config.getBoolean("war.enabled", true);
    }

    public int getOutpostDuration() {
        return config.getInt("war.outpost-duration", 3600);
    }

    public int getOutpostMinPlayers() {
        return config.getInt("war.outpost-min-players", 2);
    }

    public int getWarMaxDuration() {
        return config.getInt("war.max-duration", 3600);
    }

    public boolean isForceDrawOnTimeout() {
        return config.getBoolean("war.force-draw-on-timeout", true);
    }

    public int getInitialSupplies() {
        return config.getInt("war.initial-supplies", 10);
    }

    public int getDeathSupplyCost() {
        return config.getInt("war.death-supply-cost", 1);
    }

    public int getOutpostRespawnCost() {
        return config.getInt("war.outpost-respawn-cost", 2);
    }

    public boolean isCivilWarAllowed() {
        return config.getBoolean("war.allow-civil-war", true);
    }

    // ========== 攻城战系统配置 ==========

    public boolean isSiegeEnabled() {
        return config.getBoolean("siege.enabled", true);
    }

    public int getAdjacentDistance() {
        return config.getInt("siege.adjacent-distance", 48);
    }

    public int getMaxSupplyStations() {
        return config.getInt("siege.max-supply-stations", 8);
    }

    public double getSupplyStationCost() {
        return config.getDouble("siege.supply-station-cost", 10.0);
    }

    public int getSupplyStationCapacity() {
        return config.getInt("siege.supply-station-capacity", 64);
    }

    public boolean isAutoPlaceCityCore() {
        return config.getBoolean("siege.auto-place-city-core", true);
    }

    public Material getCoreBaseMaterial() {
        String materialName = config.getString("siege.core-base-material", "IRON_BLOCK");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.IRON_BLOCK;
        }
    }

    public int getCoreBaseSize() {
        return config.getInt("siege.core-base-size", 3);
    }

    // ========== 战争区域配置 ==========

    public boolean isSpectatorModeForNonParticipants() {
        return config.getBoolean("war-zone.spectator-mode-for-non-participants", true);
    }

    public boolean isFrontlineSoldierOnly() {
        return config.getBoolean("war-zone.frontline-soldier-only", true);
    }

    public boolean isRestoreGameModeOnEnd() {
        return config.getBoolean("war-zone.restore-gamemode-on-end", true);
    }

    public String getRestoreGameMode() {
        return config.getString("war-zone.restore-gamemode", "SURVIVAL");
    }

    // ========== 战争保护配置 ==========

    public boolean isWarProtectionEnabled() {
        return config.getBoolean("war-protection.enabled", true);
    }

    public boolean isEnemyInteractionAllowed() {
        return config.getBoolean("war-protection.allow-enemy-interaction", false);
    }

    public boolean isNeutralInteractionAllowed() {
        return config.getBoolean("war-protection.allow-neutral-interaction", true);
    }

    public boolean isFriendlyInteractionAllowed() {
        return config.getBoolean("war-protection.allow-friendly-interaction", true);
    }

    // ========== 消息配置 ==========

    public String getMessagePrefix() {
        return config.getString("messages.prefix", "§6[LegacyLand]§r ");
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("messages.debug", false);
    }

    public String getLanguage() {
        return config.getString("messages.language", "zh_CN");
    }

    // ========== 性能配置 ==========

    public int getOutpostCheckInterval() {
        return config.getInt("performance.outpost-check-interval", 100);
    }

    public int getWarConditionCheckInterval() {
        return config.getInt("performance.war-condition-check-interval", 200);
    }

    public int getSupplyLineCheckInterval() {
        return config.getInt("performance.supply-line-check-interval", 200);
    }

    public boolean isAsyncProcessingEnabled() {
        return config.getBoolean("performance.async-processing", true);
    }

    // ========== 集成配置 ==========

    public boolean isTownyIntegrationEnabled() {
        return config.getBoolean("integration.towny.enabled", true);
    }

    public boolean isSyncNationData() {
        return config.getBoolean("integration.towny.sync-nation-data", true);
    }

    public boolean isVaultIntegrationEnabled() {
        return config.getBoolean("integration.vault.enabled", false);
    }

    public boolean isUseVaultForTax() {
        return config.getBoolean("integration.vault.use-for-tax", false);
    }

    // ========== 实验性功能 ==========

    public boolean isExperimentalEnabled() {
        return config.getBoolean("experimental.enabled", false);
    }

    public boolean isAutoSupplyLineCalculation() {
        return config.getBoolean("experimental.auto-supply-line-calculation", false);
    }

    public boolean isDynamicWarZone() {
        return config.getBoolean("experimental.dynamic-war-zone", false);
    }
}
