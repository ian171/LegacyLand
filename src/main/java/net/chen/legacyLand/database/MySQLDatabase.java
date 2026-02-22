package net.chen.legacyLand.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.Profession;

import java.sql.*;
import java.util.*;

/**
 * MySQL 数据库实现
 * 使用 HikariCP 连接池
 */
public class MySQLDatabase implements IDatabase {

    private final LegacyLand plugin;
    private HikariDataSource dataSource;

    public MySQLDatabase(LegacyLand plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try {
            HikariConfig config = new HikariConfig();

            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "legacyland");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // 连接池配置
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 2));
            config.setConnectionTimeout(plugin.getConfig().getLong("database.mysql.pool.connection-timeout", 30000));
            config.setIdleTimeout(plugin.getConfig().getLong("database.mysql.pool.idle-timeout", 600000));
            config.setMaxLifetime(plugin.getConfig().getLong("database.mysql.pool.max-lifetime", 1800000));

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            LegacyLand.logger.info("MySQL 数据库连接成功！");
            createTables();
        } catch (Exception e) {
            LegacyLand.logger.severe("MySQL 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LegacyLand.logger.info("MySQL 数据库连接已关闭。");
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void createTables() {
        try (Connection conn = getConnection()) {
            // 国家扩展数据表
            String nationExtTable = "CREATE TABLE IF NOT EXISTS nation_extensions (" +
                    "nation_name VARCHAR(255) PRIMARY KEY," +
                    "government_type VARCHAR(50) NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 玩家角色表
            String playerRolesTable = "CREATE TABLE IF NOT EXISTS player_roles (" +
                    "nation_name VARCHAR(255) NOT NULL," +
                    "player_id VARCHAR(36) NOT NULL," +
                    "role VARCHAR(50) NOT NULL," +
                    "PRIMARY KEY (nation_name, player_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 外交关系表
            String diplomacyTable = "CREATE TABLE IF NOT EXISTS diplomacy_relations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nation1 VARCHAR(255) NOT NULL," +
                    "nation2 VARCHAR(255) NOT NULL," +
                    "relation_type VARCHAR(50) NOT NULL," +
                    "established_time BIGINT NOT NULL," +
                    "UNIQUE KEY unique_relation (nation1, nation2)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 玩家数据表
            String playersTable = "CREATE TABLE IF NOT EXISTS players (" +
                    "player_id VARCHAR(36) PRIMARY KEY," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "max_health DOUBLE DEFAULT 15.0," +
                    "hydration INT DEFAULT 10," +
                    "temperature DOUBLE DEFAULT 22.0," +
                    "main_profession VARCHAR(50)," +
                    "main_profession_level INT DEFAULT 0," +
                    "main_profession_exp INT DEFAULT 0," +
                    "sub_profession VARCHAR(50)," +
                    "sub_profession_level INT DEFAULT 0," +
                    "sub_profession_exp INT DEFAULT 0," +
                    "talent_points INT DEFAULT 10" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 战争数据表
            String warsTable = "CREATE TABLE IF NOT EXISTS wars (" +
                    "war_name VARCHAR(255) PRIMARY KEY," +
                    "war_type VARCHAR(50) NOT NULL," +
                    "attacker_nation VARCHAR(255) NOT NULL," +
                    "defender_nation VARCHAR(255) NOT NULL," +
                    "attacker_town VARCHAR(255) NOT NULL," +
                    "defender_town VARCHAR(255) NOT NULL," +
                    "status VARCHAR(50) NOT NULL," +
                    "start_time BIGINT NOT NULL," +
                    "end_time BIGINT," +
                    "attacker_supplies INT DEFAULT 10," +
                    "defender_supplies INT DEFAULT 10" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 战争参与者表
            String warParticipantsTable = "CREATE TABLE IF NOT EXISTS war_participants (" +
                    "war_name VARCHAR(255) NOT NULL," +
                    "player_id VARCHAR(36) NOT NULL," +
                    "role VARCHAR(50) NOT NULL," +
                    "PRIMARY KEY (war_name, player_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 攻城战表
            String siegeWarsTable = "CREATE TABLE IF NOT EXISTS siege_wars (" +
                    "siege_id VARCHAR(255) PRIMARY KEY," +
                    "war_name VARCHAR(255) NOT NULL," +
                    "attacker_town VARCHAR(255) NOT NULL," +
                    "defender_town VARCHAR(255) NOT NULL," +
                    "outpost_location TEXT," +
                    "outpost_establish_time BIGINT," +
                    "outpost_active BOOLEAN DEFAULT FALSE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 玩家成就表
            String playerAchievementsTable = "CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "achievement_id VARCHAR(100) NOT NULL," +
                    "PRIMARY KEY (player_id, achievement_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            // 季节数据表
            String seasonTable = "CREATE TABLE IF NOT EXISTS season_data (" +
                    "id INT PRIMARY KEY DEFAULT 1," +
                    "current_season VARCHAR(50) NOT NULL," +
                    "current_day INT NOT NULL," +
                    "days_per_sub_season INT NOT NULL," +
                    "CHECK (id = 1)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(nationExtTable);
                stmt.execute(playerRolesTable);
                stmt.execute(diplomacyTable);
                stmt.execute(playersTable);
                stmt.execute(warsTable);
                stmt.execute(warParticipantsTable);
                stmt.execute(siegeWarsTable);
                stmt.execute(playerAchievementsTable);
                stmt.execute(seasonTable);
                LegacyLand.logger.info("MySQL 数据库表创建成功！");
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("创建 MySQL 表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 实现接口方法 - 使用与 SQLite 相同的逻辑，但使用 HikariCP 连接池
    // 以下方法与 DatabaseManager 中的实现类似，只是使用 getConnection() 获取连接

    @Override
    public void saveNationGovernment(String nationName, GovernmentType governmentType) {
        String sql = "INSERT INTO nation_extensions (nation_name, government_type) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE government_type = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, governmentType.name());
            pstmt.setString(3, governmentType.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存国家政体失败: " + e.getMessage());
        }
    }

    @Override
    public GovernmentType loadNationGovernment(String nationName) {
        String sql = "SELECT government_type FROM nation_extensions WHERE nation_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return GovernmentType.valueOf(rs.getString("government_type"));
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载国家政体失败: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void savePlayerRole(String nationName, UUID playerId, NationRole role) {
        String sql = "INSERT INTO player_roles (nation_name, player_id, role) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE role = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, playerId.toString());
            pstmt.setString(3, role.name());
            pstmt.setString(4, role.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存玩家角色失败: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, NationRole> loadNationRoles(String nationName) {
        Map<UUID, NationRole> roles = new HashMap<>();
        String sql = "SELECT player_id, role FROM player_roles WHERE nation_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_id"));
                    NationRole role = NationRole.valueOf(rs.getString("role"));
                    roles.put(playerId, role);
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载国家角色失败: " + e.getMessage());
        }
        return roles;
    }

    @Override
    public void removePlayerRole(String nationName, UUID playerId) {
        String sql = "DELETE FROM player_roles WHERE nation_name = ? AND player_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, playerId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除玩家角色失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteNationData(String nationName) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM nation_extensions WHERE nation_name = '" + nationName + "'");
            stmt.execute("DELETE FROM player_roles WHERE nation_name = '" + nationName + "'");
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除国家数据失败: " + e.getMessage());
        }
    }

    @Override
    public void saveDiplomacyRelation(DiplomacyRelation relation) {
        String sql = "INSERT INTO diplomacy_relations (nation1, nation2, relation_type, established_time) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE relation_type = ?, established_time = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, relation.getNation1());
            pstmt.setString(2, relation.getNation2());
            pstmt.setString(3, relation.getRelationType().name());
            pstmt.setLong(4, relation.getEstablishedTime());
            pstmt.setString(5, relation.getRelationType().name());
            pstmt.setLong(6, relation.getEstablishedTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存外交关系失败: " + e.getMessage());
        }
    }

    @Override
    public List<DiplomacyRelation> loadAllDiplomacyRelations() {
        List<DiplomacyRelation> relations = new ArrayList<>();
        String sql = "SELECT nation1, nation2, relation_type, established_time FROM diplomacy_relations";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String nation1 = rs.getString("nation1");
                String nation2 = rs.getString("nation2");
                RelationType type = RelationType.valueOf(rs.getString("relation_type"));
                long time = rs.getLong("established_time");
                relations.add(new DiplomacyRelation(nation1, nation2, type));
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载外交关系失败: " + e.getMessage());
        }
        return relations;
    }

    @Override
    public void deleteDiplomacyRelation(String nation1, String nation2) {
        String sql = "DELETE FROM diplomacy_relations WHERE (nation1 = ? AND nation2 = ?) OR (nation1 = ? AND nation2 = ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nation1);
            pstmt.setString(2, nation2);
            pstmt.setString(3, nation2);
            pstmt.setString(4, nation1);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除外交关系失败: " + e.getMessage());
        }
    }

    @Override
    public void savePlayerData(PlayerData data) {
        String sql = "INSERT INTO players (player_id, player_name, max_health, hydration, temperature, " +
                "main_profession, main_profession_level, main_profession_exp, " +
                "sub_profession, sub_profession_level, sub_profession_exp, talent_points) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?, max_health = ?, hydration = ?, temperature = ?, " +
                "main_profession = ?, main_profession_level = ?, main_profession_exp = ?, " +
                "sub_profession = ?, sub_profession_level = ?, sub_profession_exp = ?, talent_points = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

            // ON DUPLICATE KEY UPDATE
            pstmt.setString(13, data.getPlayerName());
            pstmt.setDouble(14, data.getMaxHealth());
            pstmt.setInt(15, data.getHydration());
            pstmt.setDouble(16, data.getTemperature());
            pstmt.setString(17, data.getMainProfession() != null ? data.getMainProfession().name() : null);
            pstmt.setInt(18, data.getMainProfessionLevel());
            pstmt.setInt(19, data.getMainProfessionExp());
            pstmt.setString(20, data.getSubProfession() != null ? data.getSubProfession().name() : null);
            pstmt.setInt(21, data.getSubProfessionLevel());
            pstmt.setInt(22, data.getSubProfessionExp());
            pstmt.setInt(23, data.getTalentPoints());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存玩家数据失败: " + e.getMessage());
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID playerId, String playerName) {
        String sql = "SELECT * FROM players WHERE player_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
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

                    return data;
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载玩家数据失败: " + e.getMessage());
        }

        // 如果没有数据，创建新的
        return new PlayerData(playerId, playerName);
    }

    // 其他方法实现类似，使用相同的模式
    // 由于篇幅限制，这里省略战争、成就等方法的实现
    // 它们的实现与 DatabaseManager 中的逻辑相同

    @Override
    public void saveWar(net.chen.legacyLand.war.War war) {
        String sql = "INSERT INTO wars (war_name, war_type, attacker_nation, defender_nation, " +
                "attacker_town, defender_town, status, start_time, end_time, attacker_supplies, defender_supplies) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = ?, end_time = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, war.getWarName());
            pstmt.setString(2, war.getType().name());
            pstmt.setString(3, war.getAttackerNation());
            pstmt.setString(4, war.getDefenderNation());
            pstmt.setString(5, war.getAttackerTown());
            pstmt.setString(6, war.getDefenderTown());
            pstmt.setString(7, war.getStatus().name());
            pstmt.setLong(8, war.getStartTime());
            pstmt.setLong(9, war.getEndTime());
            pstmt.setInt(10, 10);
            pstmt.setInt(11, 10);
            pstmt.setString(12, war.getStatus().name());
            pstmt.setLong(13, war.getEndTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存战争数据失败: " + e.getMessage());
        }
    }

    @Override
    public void saveWarData(String warName, Map<String, Object> warData) {
        String sql = "INSERT INTO wars (war_name, war_type, attacker_nation, defender_nation, " +
                "attacker_town, defender_town, status, start_time, end_time, attacker_supplies, defender_supplies) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = ?, start_time = ?, end_time = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            pstmt.setString(2, (String) warData.getOrDefault("war_type", "NATION"));
            pstmt.setString(3, (String) warData.getOrDefault("attacker_nation", ""));
            pstmt.setString(4, (String) warData.getOrDefault("defender_nation", ""));
            pstmt.setString(5, (String) warData.getOrDefault("attacker_town", ""));
            pstmt.setString(6, (String) warData.getOrDefault("defender_town", ""));
            pstmt.setString(7, (String) warData.getOrDefault("status", "PREPARING"));
            pstmt.setLong(8, (Long) warData.getOrDefault("start_time", 0L));
            pstmt.setLong(9, (Long) warData.getOrDefault("end_time", 0L));
            pstmt.setInt(10, (Integer) warData.getOrDefault("attacker_supplies", 10));
            pstmt.setInt(11, (Integer) warData.getOrDefault("defender_supplies", 10));
            pstmt.setString(12, (String) warData.getOrDefault("status", "PREPARING"));
            pstmt.setLong(13, (Long) warData.getOrDefault("start_time", 0L));
            pstmt.setLong(14, (Long) warData.getOrDefault("end_time", 0L));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存战争数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> loadWarData(String warName) {
        Map<String, Object> warData = new HashMap<>();
        String sql = "SELECT * FROM wars WHERE war_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    warData.put("war_name", rs.getString("war_name"));
                    warData.put("war_type", rs.getString("war_type"));
                    warData.put("attacker_nation", rs.getString("attacker_nation"));
                    warData.put("defender_nation", rs.getString("defender_nation"));
                    warData.put("attacker_town", rs.getString("attacker_town"));
                    warData.put("defender_town", rs.getString("defender_town"));
                    warData.put("status", rs.getString("status"));
                    warData.put("start_time", rs.getLong("start_time"));
                    warData.put("end_time", rs.getLong("end_time"));
                    warData.put("attacker_supplies", rs.getInt("attacker_supplies"));
                    warData.put("defender_supplies", rs.getInt("defender_supplies"));
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载战争数据失败: " + e.getMessage());
        }
        return warData;
    }

    @Override
    public void deleteWarData(String warName) {
        String sql = "DELETE FROM wars WHERE war_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("删除战争数据失败: " + e.getMessage());
        }
    }

    @Override
    public void saveWarParticipant(String warName, UUID playerId, String role) {
        String sql = "INSERT INTO war_participants (war_name, player_id, role) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE role = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            pstmt.setString(2, playerId.toString());
            pstmt.setString(3, role);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存战争参与者失败: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, String> loadWarParticipants(String warName) {
        Map<UUID, String> participants = new HashMap<>();
        String sql = "SELECT player_id, role FROM war_participants WHERE war_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_id"));
                    String role = rs.getString("role");
                    participants.put(playerId, role);
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载战争参与者失败: " + e.getMessage());
        }
        return participants;
    }

    @Override
    public void saveSiegeWar(Map<String, Object> siegeData) {
        String sql = "INSERT INTO siege_wars (siege_id, war_name, attacker_town, defender_town, " +
                "outpost_location, outpost_establish_time, outpost_active) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE outpost_location = ?, outpost_establish_time = ?, outpost_active = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, (String) siegeData.get("siege_id"));
            pstmt.setString(2, (String) siegeData.get("war_name"));
            pstmt.setString(3, (String) siegeData.get("attacker_town"));
            pstmt.setString(4, (String) siegeData.get("defender_town"));
            pstmt.setString(5, (String) siegeData.get("outpost_location"));
            pstmt.setLong(6, (Long) siegeData.getOrDefault("outpost_establish_time", 0L));
            pstmt.setBoolean(7, (Boolean) siegeData.getOrDefault("outpost_active", false));
            pstmt.setString(8, (String) siegeData.get("outpost_location"));
            pstmt.setLong(9, (Long) siegeData.getOrDefault("outpost_establish_time", 0L));
            pstmt.setBoolean(10, (Boolean) siegeData.getOrDefault("outpost_active", false));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存攻城战数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> loadSiegeWar(String siegeId) {
        Map<String, Object> siegeData = new HashMap<>();
        String sql = "SELECT * FROM siege_wars WHERE siege_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, siegeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    siegeData.put("siege_id", rs.getString("siege_id"));
                    siegeData.put("war_name", rs.getString("war_name"));
                    siegeData.put("attacker_town", rs.getString("attacker_town"));
                    siegeData.put("defender_town", rs.getString("defender_town"));
                    siegeData.put("outpost_location", rs.getString("outpost_location"));
                    siegeData.put("outpost_establish_time", rs.getLong("outpost_establish_time"));
                    siegeData.put("outpost_active", rs.getBoolean("outpost_active"));
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载攻城战数据失败: " + e.getMessage());
        }
        return siegeData;
    }

    @Override
    public void savePlayerAchievement(UUID playerId, String achievementId) {
        String sql = "INSERT IGNORE INTO player_achievements (player_id, achievement_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setString(2, achievementId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存玩家成就失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> loadPlayerAchievements(UUID playerId) {
        List<String> achievements = new ArrayList<>();
        String sql = "SELECT achievement_id FROM player_achievements WHERE player_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    achievements.add(rs.getString("achievement_id"));
                }
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载玩家成就失败: " + e.getMessage());
        }
        return achievements;
    }

    @Override
    public void saveSeasonData(String currentSeason, int currentDay, int daysPerSubSeason) {
        String sql = "INSERT INTO season_data (id, current_season, current_day, days_per_sub_season) " +
                "VALUES (1, ?, ?, ?) ON DUPLICATE KEY UPDATE current_season = ?, current_day = ?, days_per_sub_season = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentSeason);
            pstmt.setInt(2, currentDay);
            pstmt.setInt(3, daysPerSubSeason);
            pstmt.setString(4, currentSeason);
            pstmt.setInt(5, currentDay);
            pstmt.setInt(6, daysPerSubSeason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LegacyLand.logger.severe("保存季节数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> loadSeasonData() {
        String sql = "SELECT current_season, current_day, days_per_sub_season FROM season_data WHERE id = 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
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
