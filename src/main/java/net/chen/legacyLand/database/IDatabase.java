package net.chen.legacyLand.database;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.war.War;

import java.sql.Connection;
import java.sql.SQLException;
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
     * 获取底层 JDBC 连接（用于扩展模块直接操作，仅 SQL 类型数据库支持）
     */
    default Connection getConnection() throws SQLException { return null; }

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

    // ========== 市场数据 ==========

    /**
     * 保存市场数据
     */
    void saveMarket(net.chen.legacyLand.market.Market market);

    /**
     * 加载所有市场数据
     */
    List<net.chen.legacyLand.market.Market> loadAllMarkets();

    /**
     * 删除市场数据
     */
    void deleteMarket(String marketId);

    /**
     * 保存市场箱子数据
     */
    void saveMarketChest(net.chen.legacyLand.market.MarketChest chest);

    /**
     * 加载指定市场的所有箱子
     */
    List<net.chen.legacyLand.market.MarketChest> loadMarketChests(String marketId);

    /**
     * 删除市场箱子数据
     */
    void deleteMarketChest(String chestId);

    // ========== 外交保卫关系 ==========

    /**
     * 保存保卫关系
     */
    default void saveGuaranteeRelation(net.chen.legacyLand.nation.diplomacy.GuaranteeRelation relation) {
        // 默认实现：不做任何操作
    }

    /**
     * 加载所有保卫关系
     */
    default Map<String, List<net.chen.legacyLand.nation.diplomacy.GuaranteeRelation>> loadAllGuarantees() {
        // 默认实现：返回空 Map
        return new java.util.HashMap<>();
    }

    /**
     * 更新保卫关系
     */
    default void updateGuaranteeRelation(net.chen.legacyLand.nation.diplomacy.GuaranteeRelation relation) {
        // 默认实现：不做任何操作
    }

    /**
     * 删除保卫关系
     */
    default void deleteGuaranteeRelation(String guarantorNation, String protectedNation) {
        // 默认实现：不做任何操作
    }

    /**
     * 删除国家的所有保卫关系
     */
    default void deleteNationGuarantees(String nationName) {
        // 默认实现：不做任何操作
    }
}
