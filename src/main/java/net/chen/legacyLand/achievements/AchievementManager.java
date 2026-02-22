package net.chen.legacyLand.achievements;

import lombok.Getter;
import lombok.Setter;
import net.chen.legacyLand.database.DatabaseManager;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;

import java.util.UUID;

public class AchievementManager {
    @Setter
    @Getter
    private static AchievementManager instance;
    private final PlayerManager playerManager;
    private final DatabaseManager databaseManager;

    public AchievementManager(PlayerManager playerManager, DatabaseManager databaseManager) {
        this.playerManager = playerManager;
        this.databaseManager = databaseManager;
    }

    /**
     * 授予玩家成就
     */
    public void grantAchievement(UUID playerId, Achievements achievement) {
        PlayerData playerData = playerManager.getPlayerData(playerId);
        if (playerData != null && !playerData.hasAchievement(achievement)) {
            playerData.addAchievement(achievement);
            // 实时保存到数据库
            databaseManager.savePlayerAchievement(playerId, achievement.name());
        }
    }

    /**
     * 移除玩家成就
     */
    public void repealAchievement(UUID playerId, Achievements achievement) {
        PlayerData playerData = playerManager.getPlayerData(playerId);
        if (playerData != null && playerData.hasAchievement(achievement)) {
            playerData.removeAchievement(achievement);
            // 注意：当前数据库接口没有删除成就的方法，只能重新保存整个玩家数据
            databaseManager.savePlayerData(playerData);
        }
    }

    /**
     * 检查玩家是否拥有成就
     */
    public boolean hasAchievement(UUID playerId, Achievements achievement) {
        PlayerData playerData = playerManager.getPlayerData(playerId);
        return playerData != null && playerData.hasAchievement(achievement);
    }
}
