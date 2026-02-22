package net.chen.legacyLand.database;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.player.PlayerData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据库管理器 - 使用工厂模式支持多种数据库
 * 支持 SQLite, MySQL, MongoDB
 */
public class DatabaseManager {
    private final LegacyLand plugin;
    private IDatabase database;

    public DatabaseManager(LegacyLand plugin) {
        this.plugin = plugin;
    }

    /**
     * 连接数据库
     * 根据配置文件选择数据库类型
     */
    public void connect() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        switch (dbType) {
            case "mysql":
                database = new MySQLDatabase(plugin);
                LegacyLand.logger.info("使用 MySQL 数据库");
                break;
            case "mongodb":
                database = new MongoDatabase(plugin);
                LegacyLand.logger.info("使用 MongoDB 数据库");
                break;
            case "sqlite":
            default:
                database = new SQLiteDatabase(plugin);
                LegacyLand.logger.info("使用 SQLite 数据库");
                break;
        }

        database.connect();
    }

    /**
     * 断开数据库连接
     */
    public void disconnect() {
        if (database != null) {
            database.disconnect();
        }
    }

    // ========== 国家扩展数据 ==========

    public void saveNationGovernment(String nationName, GovernmentType governmentType) {
        database.saveNationGovernment(nationName, governmentType);
    }

    public GovernmentType loadNationGovernment(String nationName) {
        return database.loadNationGovernment(nationName);
    }

    public void savePlayerRole(String nationName, UUID playerId, NationRole role) {
        database.savePlayerRole(nationName, playerId, role);
    }

    public Map<UUID, NationRole> loadNationRoles(String nationName) {
        return database.loadNationRoles(nationName);
    }

    public void removePlayerRole(String nationName, UUID playerId) {
        database.removePlayerRole(nationName, playerId);
    }

    public void deleteNationData(String nationName) {
        database.deleteNationData(nationName);
    }

    // ========== 外交关系 ==========

    public void saveDiplomacyRelation(DiplomacyRelation relation) {
        database.saveDiplomacyRelation(relation);
    }

    public List<DiplomacyRelation> loadAllDiplomacyRelations() {
        return database.loadAllDiplomacyRelations();
    }

    public void deleteDiplomacyRelation(String nation1, String nation2) {
        database.deleteDiplomacyRelation(nation1, nation2);
    }

    // ========== 玩家数据 ==========

    public void savePlayerData(PlayerData data) {
        database.savePlayerData(data);
    }

    public PlayerData loadPlayerData(UUID playerId, String playerName) {
        return database.loadPlayerData(playerId, playerName);
    }

    // ========== 战争数据 ==========

    public void saveWar(net.chen.legacyLand.war.War war) {
        database.saveWar(war);
    }

    public void saveWarData(String warName, Map<String, Object> warData) {
        database.saveWarData(warName, warData);
    }

    public Map<String, Object> loadWarData(String warName) {
        return database.loadWarData(warName);
    }

    public void deleteWarData(String warName) {
        database.deleteWarData(warName);
    }

    public void saveWarParticipant(String warName, UUID playerId, String role) {
        database.saveWarParticipant(warName, playerId, role);
    }

    public Map<UUID, String> loadWarParticipants(String warName) {
        return database.loadWarParticipants(warName);
    }

    public void saveSiegeWar(Map<String, Object> siegeData) {
        database.saveSiegeWar(siegeData);
    }

    public Map<String, Object> loadSiegeWar(String siegeId) {
        return database.loadSiegeWar(siegeId);
    }

    // ========== 成就数据 ==========

    public void savePlayerAchievement(UUID playerId, String achievementId) {
        database.savePlayerAchievement(playerId, achievementId);
    }

    public List<String> loadPlayerAchievements(UUID playerId) {
        return database.loadPlayerAchievements(playerId);
    }

    // ========== 季节数据 ==========

    public void saveSeasonData(String currentSeason, int currentDay, int daysPerSubSeason) {
        database.saveSeasonData(currentSeason, currentDay, daysPerSubSeason);
    }

    public Map<String, Object> loadSeasonData() {
        return database.loadSeasonData();
    }
}
