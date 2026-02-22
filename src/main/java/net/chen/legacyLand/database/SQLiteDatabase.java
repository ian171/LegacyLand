package net.chen.legacyLand.database;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.Profession;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * SQLite 数据库实现
 */
public class SQLiteDatabase implements IDatabase {

    private final LegacyLand plugin;
    private Connection connection;

    public SQLiteDatabase(LegacyLand plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String filename = plugin.getConfig().getString("database.sqlite.filename", "legacyland.db");
            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/" + filename;
            connection = DriverManager.getConnection(url);
            LegacyLand.logger.info("SQLite 数据库连接成功！");
            createTables();
        } catch (SQLException e) {
            LegacyLand.logger.severe("SQLite 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LegacyLand.logger.info("SQLite 数据库连接已关闭。");
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("关闭 SQLite 数据库连接失败: " + e.getMessage());
        }
    }

    @Override
    public void createTables() {
        try {
            Statement stmt = connection.createStatement();

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
                    "attacker_supplies INTEGER DEFAULT 10," +
                    "defender_supplies INTEGER DEFAULT 10" +
                    ")";

            // 战争参与者表
            String warParticipantsTable = "CREATE TABLE IF NOT EXISTS war_participants (" +
                    "war_name TEXT NOT NULL," +
                    "player_id TEXT NOT NULL," +
                    "role TEXT NOT NULL," +
                    "PRIMARY KEY (war_name, player_id)" +
                    ")";

            // 攻城战表
            String siegeWarsTable = "CREATE TABLE IF NOT EXISTS siege_wars (" +
                    "siege_id TEXT PRIMARY KEY," +
                    "war_name TEXT NOT NULL," +
                    "attacker_town TEXT NOT NULL," +
                    "defender_town TEXT NOT NULL," +
                    "outpost_location TEXT," +
                    "outpost_establish_time BIGINT," +
                    "outpost_active INTEGER DEFAULT 0" +
                    ")";

            // 玩家成就表
            String playerAchievementsTable = "CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "player_id TEXT NOT NULL," +
                    "achievement_id TEXT NOT NULL," +
                    "PRIMARY KEY (player_id, achievement_id)" +
                    ")";

            // 季节数据表
            String seasonTable = "CREATE TABLE IF NOT EXISTS season_data (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                    "current_season TEXT NOT NULL," +
                    "current_day INTEGER NOT NULL," +
                    "days_per_sub_season INTEGER NOT NULL" +
                    ")";

            // FlagWar 数据表
            String flagWarsTable = "CREATE TABLE IF NOT EXISTS flag_wars (" +
                    "flag_war_id TEXT PRIMARY KEY," +
                    "attacker_id TEXT NOT NULL," +
                    "attacker_nation TEXT NOT NULL," +
                    "attacker_town TEXT NOT NULL," +
                    "defender_nation TEXT NOT NULL," +
                    "defender_town TEXT NOT NULL," +
                    "flag_location TEXT NOT NULL," +
                    "timer_block_location TEXT NOT NULL," +
                    "beacon_location TEXT NOT NULL," +
                    "start_time INTEGER NOT NULL," +
                    "end_time INTEGER," +
                    "status TEXT NOT NULL," +
                    "timer_progress INTEGER DEFAULT 0," +
                    "staking_fee REAL DEFAULT 0," +
                    "defense_break_fee REAL DEFAULT 0," +
                    "victory_cost REAL DEFAULT 0," +
                    "town_block_coords TEXT," +
                    "is_home_block INTEGER DEFAULT 0" +
                    ")";

            stmt.execute(nationExtTable);
            stmt.execute(playerRolesTable);
            stmt.execute(diplomacyTable);
            stmt.execute(playersTable);
            stmt.execute(warsTable);
            stmt.execute(warParticipantsTable);
            stmt.execute(siegeWarsTable);
            stmt.execute(playerAchievementsTable);
            stmt.execute(seasonTable);
            stmt.execute(flagWarsTable);

            stmt.close();
            LegacyLand.logger.info("SQLite 数据库表创建成功！");
        } catch (SQLException e) {
            LegacyLand.logger.severe("创建 SQLite 表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveNationGovernment(String nationName, GovernmentType governmentType) {
        String sql = "INSERT OR REPLACE INTO nation_extensions (nation_name, government_type) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, governmentType.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存国家政体失败: " + e.getMessage());
        }
    }

    @Override
    public GovernmentType loadNationGovernment(String nationName) {
        String sql = "SELECT government_type FROM nation_extensions WHERE nation_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return GovernmentType.valueOf(rs.getString("government_type"));
            }
        } catch (SQLException e) {
           getLogger().severe("加载国家政体失败: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void savePlayerRole(String nationName, UUID playerId, NationRole role) {
        String sql = "INSERT OR REPLACE INTO player_roles (nation_name, player_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, playerId.toString());
            pstmt.setString(3, role.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存玩家角色失败: " + e.getMessage());
        }
    }

    @Override
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
            getLogger().severe("加载国家角色失败: " + e.getMessage());
        }
        return roles;
    }

    @Override
    public void removePlayerRole(String nationName, UUID playerId) {
        String sql = "DELETE FROM player_roles WHERE nation_name = ? AND player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nationName);
            pstmt.setString(2, playerId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("删除玩家角色失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteNationData(String nationName) {
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("DELETE FROM nation_extensions WHERE nation_name = '" + nationName + "'");
            stmt.execute("DELETE FROM player_roles WHERE nation_name = '" + nationName + "'");
            stmt.close();
        } catch (SQLException e) {
            getLogger().severe("删除国家数据失败: " + e.getMessage());
        }
    }

    @Override
    public void saveDiplomacyRelation(DiplomacyRelation relation) {
        String sql = "INSERT OR REPLACE INTO diplomacy_relations (nation1, nation2, relation_type, established_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, relation.getNation1());
            pstmt.setString(2, relation.getNation2());
            pstmt.setString(3, relation.getRelationType().name());
            pstmt.setLong(4, relation.getEstablishedTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存外交关系失败: " + e.getMessage());
        }
    }

    @Override
    public List<DiplomacyRelation> loadAllDiplomacyRelations() {
        List<DiplomacyRelation> relations = new ArrayList<>();
        String sql = "SELECT nation1, nation2, relation_type, established_time FROM diplomacy_relations";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String nation1 = rs.getString("nation1");
                String nation2 = rs.getString("nation2");
                RelationType type = RelationType.valueOf(rs.getString("relation_type"));
                long time = rs.getLong("established_time");
                relations.add(new DiplomacyRelation(nation1, nation2, type));
            }
        } catch (SQLException e) {
            getLogger().severe("加载外交关系失败: " + e.getMessage());
        }
        return relations;
    }

    @Override
    public void deleteDiplomacyRelation(String nation1, String nation2) {
        String sql = "DELETE FROM diplomacy_relations WHERE (nation1 = ? AND nation2 = ?) OR (nation1 = ? AND nation2 = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nation1);
            pstmt.setString(2, nation2);
            pstmt.setString(3, nation2);
            pstmt.setString(4, nation1);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("删除外交关系失败: " + e.getMessage());
        }
    }

    @Override
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
        } catch (SQLException e) {
            getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID playerId, String playerName) {
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

                return data;
            }
        } catch (SQLException e) {
            getLogger().severe("加载玩家数据失败: " + e.getMessage());
        }

        return new PlayerData(playerId, playerName);
    }

    @Override
    public void saveWar(net.chen.legacyLand.war.War war) {
        String sql = "INSERT OR REPLACE INTO wars (war_name, war_type, attacker_nation, defender_nation, " +
                "attacker_town, defender_town, status, start_time, end_time, attacker_supplies, defender_supplies) " +
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
            pstmt.setLong(9, war.getEndTime());
            pstmt.setInt(10, 10); // 默认补给
            pstmt.setInt(11, 10); // 默认补给
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存战争数据失败: " + e.getMessage());
        }
    }

    @Override
    public void saveWarData(String warName, Map<String, Object> warData) {
        String sql = "INSERT OR REPLACE INTO wars (war_name, war_type, attacker_nation, defender_nation, " +
                "attacker_town, defender_town, status, start_time, end_time, attacker_supplies, defender_supplies) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存战争数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> loadWarData(String warName) {
        Map<String, Object> warData = new HashMap<>();
        String sql = "SELECT * FROM wars WHERE war_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            ResultSet rs = pstmt.executeQuery();
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
        } catch (SQLException e) {
            getLogger().severe("加载战争数据失败: " + e.getMessage());
        }
        return warData;
    }

    @Override
    public void deleteWarData(String warName) {
        String sql = "DELETE FROM wars WHERE war_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("删除战争数据失败: " + e.getMessage());
        }
    }

    @Override
    public void saveWarParticipant(String warName, UUID playerId, String role) {
        String sql = "INSERT OR REPLACE INTO war_participants (war_name, player_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            pstmt.setString(2, playerId.toString());
            pstmt.setString(3, role);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存战争参与者失败: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, String> loadWarParticipants(String warName) {
        Map<UUID, String> participants = new HashMap<>();
        String sql = "SELECT player_id, role FROM war_participants WHERE war_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                String role = rs.getString("role");
                participants.put(playerId, role);
            }
        } catch (SQLException e) {
            getLogger().severe("加载战争参与者失败: " + e.getMessage());
        }
        return participants;
    }

    @Override
    public void saveSiegeWar(Map<String, Object> siegeData) {
        String sql = "INSERT OR REPLACE INTO siege_wars (siege_id, war_name, attacker_town, defender_town, " +
                "outpost_location, outpost_establish_time, outpost_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, (String) siegeData.get("siege_id"));
            pstmt.setString(2, (String) siegeData.get("war_name"));
            pstmt.setString(3, (String) siegeData.get("attacker_town"));
            pstmt.setString(4, (String) siegeData.get("defender_town"));
            pstmt.setString(5, (String) siegeData.get("outpost_location"));
            pstmt.setLong(6, (Long) siegeData.getOrDefault("outpost_establish_time", 0L));
            pstmt.setBoolean(7, (Boolean) siegeData.getOrDefault("outpost_active", false));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存攻城战数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> loadSiegeWar(String siegeId) {
        Map<String, Object> siegeData = new HashMap<>();
        String sql = "SELECT * FROM siege_wars WHERE siege_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, siegeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                siegeData.put("siege_id", rs.getString("siege_id"));
                siegeData.put("war_name", rs.getString("war_name"));
                siegeData.put("attacker_town", rs.getString("attacker_town"));
                siegeData.put("defender_town", rs.getString("defender_town"));
                siegeData.put("outpost_location", rs.getString("outpost_location"));
                siegeData.put("outpost_establish_time", rs.getLong("outpost_establish_time"));
                siegeData.put("outpost_active", rs.getBoolean("outpost_active"));
            }
        } catch (SQLException e) {
            getLogger().severe("加载攻城战数据失败: " + e.getMessage());
        }
        return siegeData;
    }

    @Override
    public void savePlayerAchievement(UUID playerId, String achievementId) {
        String sql = "INSERT OR IGNORE INTO player_achievements (player_id, achievement_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                achievements.add(rs.getString("achievement_id"));
            }
        } catch (SQLException e) {
            LegacyLand.logger.severe("加载玩家成就失败: " + e.getMessage());
        }
        return achievements;
    }

    @Override
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

    @Override
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

    @Override
    public void saveFlagWar(net.chen.legacyLand.war.flagwar.FlagWarData flagWar) {
        String sql = "INSERT OR REPLACE INTO flag_wars (" +
                "flag_war_id, attacker_id, attacker_nation, attacker_town, defender_nation, defender_town, " +
                "flag_location, timer_block_location, beacon_location, start_time, end_time, status, " +
                "timer_progress, staking_fee, defense_break_fee, victory_cost, town_block_coords, is_home_block" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, flagWar.getFlagWarId());
            pstmt.setString(2, flagWar.getAttackerId().toString());
            pstmt.setString(3, flagWar.getAttackerNation());
            pstmt.setString(4, flagWar.getAttackerTown());
            pstmt.setString(5, flagWar.getDefenderNation());
            pstmt.setString(6, flagWar.getDefenderTown());
            pstmt.setString(7, serializeLocation(flagWar.getFlagLocation()));
            pstmt.setString(8, serializeLocation(flagWar.getTimerBlockLocation()));
            pstmt.setString(9, serializeLocation(flagWar.getBeaconLocation()));
            pstmt.setLong(10, flagWar.getStartTime());
            pstmt.setLong(11, flagWar.getEndTime());
            pstmt.setString(12, flagWar.getStatus().name());
            pstmt.setInt(13, flagWar.getTimerProgress());
            pstmt.setDouble(14, flagWar.getStakingFee());
            pstmt.setDouble(15, flagWar.getDefenseBreakFee());
            pstmt.setDouble(16, flagWar.getVictoryCost());
            pstmt.setString(17, flagWar.getTownBlockCoords());
            pstmt.setInt(18, flagWar.isHomeBlock() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("保存 FlagWar 数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<net.chen.legacyLand.war.flagwar.FlagWarData> loadActiveFlagWars() {
        List<net.chen.legacyLand.war.flagwar.FlagWarData> result = new ArrayList<>();
        String sql = "SELECT * FROM flag_wars WHERE status = 'ACTIVE'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                org.bukkit.Location flagLoc = deserializeLocation(rs.getString("flag_location"));
                if (flagLoc == null) continue;
                net.chen.legacyLand.war.flagwar.FlagWarData flagWar = new net.chen.legacyLand.war.flagwar.FlagWarData(
                    rs.getString("flag_war_id"),
                    UUID.fromString(rs.getString("attacker_id")),
                    rs.getString("attacker_nation"),
                    rs.getString("attacker_town"),
                    rs.getString("defender_nation"),
                    rs.getString("defender_town"),
                    flagLoc
                );
                flagWar.setStatus(net.chen.legacyLand.war.flagwar.FlagWarStatus.valueOf(rs.getString("status")));
                flagWar.setTimerProgress(rs.getInt("timer_progress"));
                flagWar.setStakingFee(rs.getDouble("staking_fee"));
                flagWar.setDefenseBreakFee(rs.getDouble("defense_break_fee"));
                flagWar.setVictoryCost(rs.getDouble("victory_cost"));
                flagWar.setTownBlockCoords(rs.getString("town_block_coords"));
                flagWar.setHomeBlock(rs.getInt("is_home_block") == 1);
                result.add(flagWar);
            }
        } catch (SQLException e) {
            getLogger().severe("加载 FlagWar 数据失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void deleteFlagWar(String flagWarId) {
        String sql = "DELETE FROM flag_wars WHERE flag_war_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, flagWarId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("删除 FlagWar 数据失败: " + e.getMessage());
        }
    }

    private String serializeLocation(org.bukkit.Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private org.bukkit.Location deserializeLocation(String str) {
        if (str == null) return null;
        try {
            String[] parts = str.split(",");
            return new org.bukkit.Location(org.bukkit.Bukkit.getWorld(parts[0]),
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            getLogger().severe("反序列化 Location 失败: " + str);
            return null;
        }
    }
}
