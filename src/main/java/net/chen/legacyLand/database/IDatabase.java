package net.chen.legacyLand.database;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.war.War;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 数据库接口
 * 定义所有数据库操作
 */
public interface IDatabase {
    default Logger getLogger(){
        return LegacyLand.logger;
    }

    /**
     * 连接数据库
     */
    void connect();

    /**
     * 断开数据库连接
     */
    void disconnect();

    /**
     * 创建表结构
     */
    void createTables();

    // ========== 国家扩展数据 ==========

    void saveNationGovernment(String nationName, GovernmentType governmentType);

    GovernmentType loadNationGovernment(String nationName);

    /**
     * 保存国家政治体制（配置驱动）
     */
    void saveNationPoliticalSystem(String nationName, String systemId);

    /**
     * 加载国家政治体制ID
     */
    String loadNationPoliticalSystem(String nationName);

    void savePlayerRole(String nationName, UUID playerId, NationRole role);

    Map<UUID, NationRole> loadNationRoles(String nationName);

    void removePlayerRole(String nationName, UUID playerId);

    void deleteNationData(String nationName);

    // ========== 外交关系 ==========

    void saveDiplomacyRelation(DiplomacyRelation relation);

    List<DiplomacyRelation> loadAllDiplomacyRelations();

    void deleteDiplomacyRelation(String nation1, String nation2);

    // ========== 玩家数据 ==========

    void savePlayerData(PlayerData data);

    PlayerData loadPlayerData(UUID playerId, String playerName);

    // ========== 战争数据 ==========

    void saveWar(War war);

    void saveWarData(String warName, Map<String, Object> warData);

    Map<String, Object> loadWarData(String warName);

    void deleteWarData(String warName);

    void saveWarParticipant(String warName, UUID playerId, String role);

    Map<UUID, String> loadWarParticipants(String warName);

    void saveSiegeWar(Map<String, Object> siegeData);

    Map<String, Object> loadSiegeWar(String siegeId);

    // ========== 成就数据 ==========

    void savePlayerAchievement(UUID playerId, String achievementId);

    List<String> loadPlayerAchievements(UUID playerId);

    // ========== 季节数据 ==========

    void saveSeasonData(String currentSeason, int currentDay, int daysPerSubSeason);

    Map<String, Object> loadSeasonData();

    // ========== FlagWar 数据 ==========

    void saveFlagWar(net.chen.legacyLand.war.flagwar.FlagWarData flagWar);

    List<net.chen.legacyLand.war.flagwar.FlagWarData> loadActiveFlagWars();

    void deleteFlagWar(String flagWarId);
}
