package net.chen.legacyLand.database;

import com.google.common.collect.Iterators;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.Profession;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 数据库管理器
 */
public class DatabaseManager {
    private Connection connection;
    private final LegacyLand plugin;

    public DatabaseManager(LegacyLand plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/legacyland.db";
            connection = DriverManager.getConnection(url);
            LegacyLand.logger.info("数据库连接成功！");
            createTables();
        } catch (SQLException e) {
            LegacyLand.logger.severe("数据库连接失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LegacyLand.logger.info("数据库连接已关闭。");
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("关闭数据库连接失败: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        // 国家扩展数据表
        String nationExtTable = "CREATE TABLE IF NOT EXISTS nation_extensions (" +
                "nation_name TEXT PRIMARY KEY," +
                "government_type TEXT NOT NULL" +
                ")";

        // 玩家角色表
        String playerRolesTable = "CREATE TABLE IF NOT EXISTS player_roles (" +
                "nation_name TEXT NOT NULL," +
                "player_id TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "PRIMARY KEY (nation_name, player_id)" +
                ")";

        // 外交关系表
        String diplomacyTable = "CREATE TABLE IF NOT EXISTS diplomacy_relations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nation1 TEXT NOT NULL," +
                "nation2 TEXT NOT NULL," +
                "relation_type TEXT NOT NULL," +
                "established_time BIGINT NOT NULL," +
                "UNIQUE(nation1, nation2)" +
                ")";

        // 玩家数据表
        String playersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "player_id TEXT PRIMARY KEY," +
                "player_name TEXT NOT NULL," +
                "max_health REAL DEFAULT 15.0," +
                "hydration INTEGER DEFAULT 10," +
                "temperature REAL DEFAULT 22.0," +
                "main_profession TEXT," +
                "main_profession_level INTEGER DEFAULT 0," +
                "main_profession_exp INTEGER DEFAULT 0," +
                "sub_profession TEXT," +
                "sub_profession_level INTEGER DEFAULT 0," +
                "sub_profession_exp INTEGER DEFAULT 0," +
                "talent_points INTEGER DEFAULT 10" +
                ")";

        // 战争数据表
        String warsTable = "CREATE TABLE IF NOT EXISTS wars (" +
                "war_name TEXT PRIMARY KEY," +
                "war_type TEXT NOT NULL," +
                "attacker_nation TEXT NOT NULL," +
                "defender_nation TEXT NOT NULL," +
                "attacker_town TEXT NOT NULL," +
                "defender_town TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "start_time BIGINT NOT NULL," +
                "end_time BIGINT," +
                "winner TEXT," +
                "loser TEXT" +
                ")";

        // 战争参与者表
        String warParticipantsTable = "CREATE TABLE IF NOT EXISTS war_participants (" +
                "war_name TEXT NOT NULL," +
                "player_id TEXT NOT NULL," +
                "side TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "supplies INTEGER DEFAULT 0," +
                "PRIMARY KEY (war_name, player_id)" +
                ")";

        // 攻城战数据表
        String siegeWarsTable = "CREATE TABLE IF NOT EXISTS siege_wars (" +
                "siege_id TEXT PRIMARY KEY," +
                "war_name TEXT NOT NULL," +
                "attacker_town TEXT NOT NULL," +
                "defender_town TEXT NOT NULL," +
                "outpost_location TEXT," +
                "outpost_establish_time BIGINT," +
                "outpost_active BOOLEAN DEFAULT 0" +
                ")";

        // 玩家成就表
        String playerAchievementsTable = "CREATE TABLE IF NOT EXISTS player_achievements (" +
                "player_id TEXT NOT NULL," +
                "achievement TEXT NOT NULL," +
                "unlock_time BIGINT NOT NULL," +
                "PRIMARY KEY (player_id, achievement)" +
                ")";

        // 季节数据表
        String seasonTable = "CREATE TABLE IF NOT EXISTS season_data (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                "current_season TEXT NOT NULL," +
                "current_day INTEGER NOT NULL," +
                "days_per_sub_season INTEGER NOT NULL" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(nationExtTable);
            stmt.execute(playerRolesTable);
            stmt.execute(diplomacyTable);
            stmt.execute(playersTable);
            stmt.execute(warsTable);
            stmt.execute(warParticipantsTable);
            stmt.execute(siegeWarsTable);
            stmt.execute(playerAchievementsTable);
            stmt.execute(seasonTable);
            LegacyLand.logger.info("数据库表创建成功！");
        }
    }

    // ========== 国家扩展数据 ==========

    public void saveNationGovernment(String nationName, GovernmentType governmentType) {
        String sql = "INSERT OR REPLACE INTO nation_extensions (nation_name, government_type) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, governmentType.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存国家政体失败: " + e.getMessage());
        }
    }

    public GovernmentType loadNationGovernment(String nationName) {
        String sql = "SELECT government_type FROM nation_extensions WHERE nation_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return GovernmentType.valueOf(rs.getString("government_type"));
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载国家政体失败: " + e.getMessage());
        }
        return null;
    }

    public void savePlayerRole(String nationName, UUID playerId, NationRole role) {
        String sql = "INSERT OR REPLACE INTO player_roles (nation_name, player_id, role) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, playerId.toString());
            pstmt.setString(3, role.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存玩家角色失败: " + e.getMessage());
        }
    }

    public Map<UUID, NationRole> loadNationRoles(String nationName) {
        Map<UUID, NationRole> roles = new HashMap<>();
        String sql = "SELECT player_id, role FROM player_roles WHERE nation_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                NationRole role = NationRole.valueOf(rs.getString("role"));
                roles.put(playerId, role);
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载国家角色失败: " + e.getMessage());
        }
        return roles;
    }

    public void deleteNationData(String nationName) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM nation_extensions WHERE nation_name = '" + nationName + "'");
            stmt.execute("DELETE FROM player_roles WHERE nation_name = '" + nationName + "'");
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除国家数据失败: " + e.getMessage());
        }
    }

    // ========== 外交关系 ==========

    public void saveDiplomacyRelation(DiplomacyRelation relation) {
        String sql = "INSERT OR REPLACE INTO diplomacy_relations (nation1, nation2, relation_type, established_time) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, relation.getNation1());
            pstmt.setString(2, relation.getNation2());
            pstmt.setString(3, relation.getRelationType().name());
            pstmt.setLong(4, relation.getEstablishedTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存外交关系失败: " + e.getMessage());
        }
    }

    public void deleteDiplomacyRelation(String nation1, String nation2) {
        String sql = "DELETE FROM diplomacy_relations WHERE (nation1 = ? AND nation2 = ?) OR (nation1 = ? AND nation2 = ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nation1);
            pstmt.setString(2, nation2);
            pstmt.setString(3, nation2);
            pstmt.setString(4, nation1);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除外交关系失败: " + e.getMessage());
        }
    }

    // ========== 玩家数据 ==========

    public void savePlayerData(PlayerData data) {
        String sql = "INSERT OR REPLACE INTO players (player_id, player_name, max_health, hydration, temperature, " +
                "main_profession, main_profession_level, main_profession_exp, " +
                "sub_profession, sub_profession_level, sub_profession_exp, talent_points) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, data.getPlayerId().toString());
            pstmt.setString(2, data.getPlayerName());
            pstmt.setDouble(3, data.getMaxHealth());
            pstmt.setInt(4, data.getHydration());
            pstmt.setDouble(5, data.getTemperature());
            pstmt.setString(6, data.getMainProfession() != null ? data.getMainProfession().name() : null);
            pstmt.setInt(7, data.getMainProfessionLevel());
            pstmt.setInt(8, data.getMainProfessionExp());
            pstmt.setString(9, data.getSubProfession() != null ? data.getSubProfession().name() : null);
            pstmt.setInt(10, data.getSubProfessionLevel());
            pstmt.setInt(11, data.getSubProfessionExp());
            pstmt.setInt(12, data.getTalentPoints());
            pstmt.executeUpdate();

            // 保存成就数据
            savePlayerAchievements(data);
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存玩家数据失败: " + e.getMessage());
        }
    }

    public PlayerData loadPlayerData(UUID playerId) {
        String sql = "SELECT * FROM players WHERE player_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                PlayerData data = new PlayerData(playerId, rs.getString("player_name"));
                data.setMaxHealth(rs.getDouble("max_health"));
                data.setHydration(rs.getInt("hydration"));
                data.setTemperature(rs.getDouble("temperature"));

                String mainProf = rs.getString("main_profession");
                if (mainProf != null) {
                    data.setMainProfession(Profession.valueOf(mainProf));
                }
                data.setMainProfessionLevel(rs.getInt("main_profession_level"));
                data.setMainProfessionExp(rs.getInt("main_profession_exp"));

                String subProf = rs.getString("sub_profession");
                if (subProf != null) {
                    data.setSubProfession(Profession.valueOf(subProf));
                }
                data.setSubProfessionLevel(rs.getInt("sub_profession_level"));
                data.setSubProfessionExp(rs.getInt("sub_profession_exp"));
                data.setTalentPoints(rs.getInt("talent_points"));

                // 加载成就数据
                loadPlayerAchievements(data);

                return data;
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载玩家数据失败: " + e.getMessage());
        }
        return null;
    }

    public Connection getConnection() {
        return connection;
    }

    // ========== 成就数据 ==========

    /**
     * 保存玩家成就数据
     */
    private void savePlayerAchievements(PlayerData data) {
        UUID playerId = data.getPlayerId();

        // 先删除旧的成就数据
        String deleteSql = "DELETE FROM player_achievements WHERE player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除旧成就数据失败: " + e.getMessage());
            return;
        }

        // 插入新的成就数据
        String insertSql = "INSERT INTO player_achievements (player_id, achievement, unlock_time) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (net.chen.legacyLand.achievements.Achievements achievement : data.getAchievements()) {
                pstmt.setString(1, playerId.toString());
                pstmt.setString(2, achievement.name());
                pstmt.setLong(3, System.currentTimeMillis());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存成就数据失败: " + e.getMessage());
        }
    }

    /**
     * 加载玩家成就数据
     */
    private void loadPlayerAchievements(PlayerData data) {
        String sql = "SELECT achievement FROM player_achievements WHERE player_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, data.getPlayerId().toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String achievementName = rs.getString("achievement");
                try {
                    net.chen.legacyLand.achievements.Achievements achievement =
                            net.chen.legacyLand.achievements.Achievements.valueOf(achievementName);
                    data.addAchievement(achievement);
                } catch (IllegalArgumentException e) {
                    LegacyLand.logger.warning("未知的成就类型: " + achievementName);
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载成就数据失败: " + e.getMessage());
        }
    }

    /**
     * 保存单个成就（用于实时保存）
     */
    public void saveAchievement(UUID playerId, net.chen.legacyLand.achievements.Achievements achievement) {
        String sql = "INSERT OR IGNORE INTO player_achievements (player_id, achievement, unlock_time) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setString(2, achievement.name());
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存成就失败: " + e.getMessage());
        }
    }

    /**
     * 删除玩家成就
     */
    public void deleteAchievement(UUID playerId, net.chen.legacyLand.achievements.Achievements achievement) {
        String sql = "DELETE FROM player_achievements WHERE player_id = ? AND achievement = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setString(2, achievement.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除成就失败: " + e.getMessage());
        }
    }

    // ========== 战争数据 ==========

    public void saveWar(net.chen.legacyLand.war.War war) {
        String sql = "INSERT OR REPLACE INTO wars (war_name, war_type, attacker_nation, defender_nation, " +
                "attacker_town, defender_town, status, start_time, end_time, winner, loser) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, war.getWarName());
            pstmt.setString(2, war.getType().name());
            pstmt.setString(3, war.getAttackerNation());
            pstmt.setString(4, war.getDefenderNation());
            pstmt.setString(5, war.getAttackerTown());
            pstmt.setString(6, war.getDefenderTown());
            pstmt.setString(7, war.getStatus().name());
            pstmt.setLong(8, war.getStartTime());
            pstmt.setObject(9, war.getEndTime() > 0 ? war.getEndTime() : null);
            pstmt.setString(10, war.getWinner());
            pstmt.setString(11, war.getLoser());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存战争数据失败: " + e.getMessage());
        }
    }

    public void saveWarParticipant(String warName, UUID playerId, String side,
                                   String role, int supplies) {
        String sql = "INSERT OR REPLACE INTO war_participants (war_name, player_id, side, role, supplies) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            pstmt.setString(2, playerId.toString());
            pstmt.setString(3, side);
            pstmt.setString(4, role);
            pstmt.setInt(5, supplies);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存战争参与者失败: " + e.getMessage());
        }
    }

    public void deleteWar(String warName) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM wars WHERE war_name = '" + warName + "'");
            stmt.execute("DELETE FROM war_participants WHERE war_name = '" + warName + "'");
            stmt.execute("DELETE FROM siege_wars WHERE war_name = '" + warName + "'");
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除战争数据失败: " + e.getMessage());
        }
    }

    public void saveSiegeWar(net.chen.legacyLand.war.siege.SiegeWar siegeWar) {
        String sql = "INSERT OR REPLACE INTO siege_wars (siege_id, war_name, attacker_town, defender_town, " +
                "outpost_location, outpost_establish_time, outpost_active) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, siegeWar.getSiegeId());
            pstmt.setString(2, siegeWar.getWarId());
            pstmt.setString(3, siegeWar.getAttackerTown());
            pstmt.setString(4, siegeWar.getDefenderTown());

            if (siegeWar.getOutpost() != null) {
                pstmt.setString(5, locationToString(siegeWar.getOutpost().getLocation()));
                pstmt.setLong(6, siegeWar.getOutpost().getEstablishTime());
                pstmt.setBoolean(7, siegeWar.getOutpost().isActive());
            } else {
                pstmt.setString(5, null);
                pstmt.setLong(6, 0);
                pstmt.setBoolean(7, false);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存攻城战数据失败: " + e.getMessage());
        }
    }

    private String locationToString(org.bukkit.Location location) {
        if (location == null) return null;
        return location.getWorld().getName() + "," +
               location.getX() + "," +
               location.getY() + "," +
               location.getZ();
    }

    private org.bukkit.Location stringToLocation(String str) {
        if (str == null) return null;
        String[] parts = str.split(",");
        return new org.bukkit.Location(
            org.bukkit.Bukkit.getWorld(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3])
        );
    }

    // ========== 季节数据 ==========

    /**
     * 保存季节数据
     */
    public void saveSeasonData(String currentSeason, int currentDay, int daysPerSubSeason) {
        String sql = "INSERT OR REPLACE INTO season_data (id, current_season, current_day, days_per_sub_season) VALUES (1, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currentSeason);
            pstmt.setInt(2, currentDay);
            pstmt.setInt(3, daysPerSubSeason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存季节数据失败: " + e.getMessage());
        }
    }

    /**
     * 加载季节数据
     * @return Map包含 current_season, current_day, days_per_sub_season，如果没有数据返回null
     */
    public Map<String, Object> loadSeasonData() {
        String sql = "SELECT current_season, current_day, days_per_sub_season FROM season_data WHERE id = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("current_season", rs.getString("current_season"));
                data.put("current_day", rs.getInt("current_day"));
                data.put("days_per_sub_season", rs.getInt("days_per_sub_season"));
                return data;
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载季节数据失败: " + e.getMessage());
        }
        return null;
    }
}
