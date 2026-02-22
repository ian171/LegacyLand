package net.chen.legacyLand.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.*;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.Profession;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB 数据库实现
 */
public class MongoDatabase implements IDatabase {

    private final LegacyLand plugin;
    private MongoClient mongoClient;
    private com.mongodb.client.MongoDatabase database;

    public MongoDatabase(LegacyLand plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try {
            String host = plugin.getConfig().getString("database.mongodb.host", "localhost");
            int port = plugin.getConfig().getInt("database.mongodb.port", 27017);
            String dbName = plugin.getConfig().getString("database.mongodb.database", "legacyland");
            String username = plugin.getConfig().getString("database.mongodb.username", "");
            String password = plugin.getConfig().getString("database.mongodb.password", "");
            String authDatabase = plugin.getConfig().getString("database.mongodb.auth-database", "admin");

            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();

            // 构建连接字符串
            String connectionString;
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + dbName + "?authSource=" + authDatabase;
            } else {
                connectionString = "mongodb://" + host + ":" + port + "/" + dbName;
            }

            settingsBuilder.applyConnectionString(new ConnectionString(connectionString));

            // 连接池配置
            settingsBuilder.applyToConnectionPoolSettings(builder -> {
                builder.maxSize(plugin.getConfig().getInt("database.mongodb.pool.max-pool-size", 10));
                builder.minSize(plugin.getConfig().getInt("database.mongodb.pool.min-pool-size", 2));
                builder.maxConnectionIdleTime(plugin.getConfig().getLong("database.mongodb.pool.max-connection-idle-time", 60000), TimeUnit.MILLISECONDS);
                builder.maxConnectionLifeTime(plugin.getConfig().getLong("database.mongodb.pool.max-connection-life-time", 1800000), TimeUnit.MILLISECONDS);
            });

            mongoClient = MongoClients.create(settingsBuilder.build());
            database = mongoClient.getDatabase(dbName);

            LegacyLand.logger.info("MongoDB 数据库连接成功！");
            createTables();
        } catch (Exception e) {
            LegacyLand.logger.severe("MongoDB 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            LegacyLand.logger.info("MongoDB 数据库连接已关闭。");
        }
    }

    @Override
    public void createTables() {
        // MongoDB 不需要预先创建集合，但可以创建索引
        try {
            // 为国家扩展数据创建索引
            database.getCollection("nation_extensions").createIndex(new Document("nation_name", 1));

            // 为玩家角色创建复合索引
            database.getCollection("player_roles").createIndex(new Document("nation_name", 1).append("player_id", 1));

            // 为外交关系创建索引
            database.getCollection("diplomacy_relations").createIndex(new Document("nation1", 1).append("nation2", 1));

            // 为玩家数据创建索引
            database.getCollection("players").createIndex(new Document("player_id", 1));

            // 为战争数据创建索引
            database.getCollection("wars").createIndex(new Document("war_name", 1));

            // 为战争参与者创建索引
            database.getCollection("war_participants").createIndex(new Document("war_name", 1).append("player_id", 1));

            // 为攻城战创建索引
            database.getCollection("siege_wars").createIndex(new Document("siege_id", 1));

            // 为玩家成就创建索引
            database.getCollection("player_achievements").createIndex(new Document("player_id", 1).append("achievement_id", 1));

            LegacyLand.logger.info("MongoDB 索引创建成功！");
        } catch (Exception e) {
            LegacyLand.logger.severe("创建 MongoDB 索引失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveNationGovernment(String nationName, GovernmentType governmentType) {
        MongoCollection<Document> collection = database.getCollection("nation_extensions");
        Document filter = new Document("nation_name", nationName);
        Document update = new Document("$set", new Document("government_type", governmentType.name()));
        collection.updateOne(filter, update, new com.mongodb.client.model.UpdateOptions().upsert(true));
    }

    @Override
    public GovernmentType loadNationGovernment(String nationName) {
        MongoCollection<Document> collection = database.getCollection("nation_extensions");
        Document filter = new Document("nation_name", nationName);
        Document result = collection.find(filter).first();
        if (result != null) {
            return GovernmentType.valueOf(result.getString("government_type"));
        }
        return null;
    }

    @Override
    public void savePlayerRole(String nationName, UUID playerId, NationRole role) {
        MongoCollection<Document> collection = database.getCollection("player_roles");
        Document filter = new Document("nation_name", nationName).append("player_id", playerId.toString());
        Document update = new Document("$set", new Document("role", role.name()));
        collection.updateOne(filter, update, new com.mongodb.client.model.UpdateOptions().upsert(true));
    }

    @Override
    public Map<UUID, NationRole> loadNationRoles(String nationName) {
        Map<UUID, NationRole> roles = new HashMap<>();
        MongoCollection<Document> collection = database.getCollection("player_roles");
        Document filter = new Document("nation_name", nationName);

        try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                UUID playerId = UUID.fromString(doc.getString("player_id"));
                NationRole role = NationRole.valueOf(doc.getString("role"));
                roles.put(playerId, role);
            }
        }
        return roles;
    }

    @Override
    public void removePlayerRole(String nationName, UUID playerId) {
        MongoCollection<Document> collection = database.getCollection("player_roles");
        Document filter = new Document("nation_name", nationName).append("player_id", playerId.toString());
        collection.deleteOne(filter);
    }

    @Override
    public void deleteNationData(String nationName) {
        database.getCollection("nation_extensions").deleteOne(new Document("nation_name", nationName));
        database.getCollection("player_roles").deleteMany(new Document("nation_name", nationName));
    }

    @Override
    public void saveDiplomacyRelation(DiplomacyRelation relation) {
        MongoCollection<Document> collection = database.getCollection("diplomacy_relations");
        Document filter = new Document("nation1", relation.getNation1()).append("nation2", relation.getNation2());
        Document doc = new Document("nation1", relation.getNation1())
                .append("nation2", relation.getNation2())
                .append("relation_type", relation.getRelationType().name())
                .append("established_time", relation.getEstablishedTime());
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public List<DiplomacyRelation> loadAllDiplomacyRelations() {
        List<DiplomacyRelation> relations = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("diplomacy_relations");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String nation1 = doc.getString("nation1");
                String nation2 = doc.getString("nation2");
                RelationType type = RelationType.valueOf(doc.getString("relation_type"));
                long time = doc.getLong("established_time");
                relations.add(new DiplomacyRelation(nation1, nation2, type));
            }
        }
        return relations;
    }

    @Override
    public void deleteDiplomacyRelation(String nation1, String nation2) {
        MongoCollection<Document> collection = database.getCollection("diplomacy_relations");
        Document filter = new Document("$or", Arrays.asList(
                new Document("nation1", nation1).append("nation2", nation2),
                new Document("nation1", nation2).append("nation2", nation1)
        ));
        collection.deleteOne(filter);
    }

    @Override
    public void savePlayerData(PlayerData data) {
        MongoCollection<Document> collection = database.getCollection("players");
        Document filter = new Document("player_id", data.getPlayerId().toString());

        Document doc = new Document("player_id", data.getPlayerId().toString())
                .append("player_name", data.getPlayerName())
                .append("max_health", data.getMaxHealth())
                .append("hydration", data.getHydration())
                .append("temperature", data.getTemperature())
                .append("main_profession", data.getMainProfession() != null ? data.getMainProfession().name() : null)
                .append("main_profession_level", data.getMainProfessionLevel())
                .append("main_profession_exp", data.getMainProfessionExp())
                .append("sub_profession", data.getSubProfession() != null ? data.getSubProfession().name() : null)
                .append("sub_profession_level", data.getSubProfessionLevel())
                .append("sub_profession_exp", data.getSubProfessionExp())
                .append("talent_points", data.getTalentPoints());

        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public PlayerData loadPlayerData(UUID playerId, String playerName) {
        MongoCollection<Document> collection = database.getCollection("players");
        Document filter = new Document("player_id", playerId.toString());
        Document doc = collection.find(filter).first();

        if (doc != null) {
            PlayerData data = new PlayerData(playerId, doc.getString("player_name"));
            data.setMaxHealth(doc.getDouble("max_health"));
            data.setHydration(doc.getInteger("hydration"));
            data.setTemperature(doc.getDouble("temperature"));

            String mainProf = doc.getString("main_profession");
            if (mainProf != null) {
                data.setMainProfession(Profession.valueOf(mainProf));
            }
            data.setMainProfessionLevel(doc.getInteger("main_profession_level"));
            data.setMainProfessionExp(doc.getInteger("main_profession_exp"));

            String subProf = doc.getString("sub_profession");
            if (subProf != null) {
                data.setSubProfession(Profession.valueOf(subProf));
            }
            data.setSubProfessionLevel(doc.getInteger("sub_profession_level"));
            data.setSubProfessionExp(doc.getInteger("sub_profession_exp"));
            data.setTalentPoints(doc.getInteger("talent_points"));

            return data;
        }

        return new PlayerData(playerId, playerName);
    }

    @Override
    public void saveWar(net.chen.legacyLand.war.War war) {
        MongoCollection<Document> collection = database.getCollection("wars");
        Document filter = new Document("war_name", war.getWarName());
        Document doc = new Document("war_name", war.getWarName())
                .append("war_type", war.getType().name())
                .append("attacker_nation", war.getAttackerNation())
                .append("defender_nation", war.getDefenderNation())
                .append("attacker_town", war.getAttackerTown())
                .append("defender_town", war.getDefenderTown())
                .append("status", war.getStatus().name())
                .append("start_time", war.getStartTime())
                .append("end_time", war.getEndTime())
                .append("attacker_supplies", 10)
                .append("defender_supplies", 10);
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public void saveWarData(String warName, Map<String, Object> warData) {
        MongoCollection<Document> collection = database.getCollection("wars");
        Document filter = new Document("war_name", warName);
        Document doc = new Document(warData);
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public Map<String, Object> loadWarData(String warName) {
        MongoCollection<Document> collection = database.getCollection("wars");
        Document filter = new Document("war_name", warName);
        Document doc = collection.find(filter).first();
        return doc != null ? new HashMap<>(doc) : new HashMap<>();
    }

    @Override
    public void deleteWarData(String warName) {
        database.getCollection("wars").deleteOne(new Document("war_name", warName));
    }

    @Override
    public void saveWarParticipant(String warName, UUID playerId, String role) {
        MongoCollection<Document> collection = database.getCollection("war_participants");
        Document filter = new Document("war_name", warName).append("player_id", playerId.toString());
        Document doc = new Document("war_name", warName)
                .append("player_id", playerId.toString())
                .append("role", role);
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public Map<UUID, String> loadWarParticipants(String warName) {
        Map<UUID, String> participants = new HashMap<>();
        MongoCollection<Document> collection = database.getCollection("war_participants");
        Document filter = new Document("war_name", warName);

        try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                UUID playerId = UUID.fromString(doc.getString("player_id"));
                String role = doc.getString("role");
                participants.put(playerId, role);
            }
        }
        return participants;
    }

    @Override
    public void saveSiegeWar(Map<String, Object> siegeData) {
        MongoCollection<Document> collection = database.getCollection("siege_wars");
        String siegeId = (String) siegeData.get("siege_id");
        Document filter = new Document("siege_id", siegeId);
        Document doc = new Document(siegeData);
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public Map<String, Object> loadSiegeWar(String siegeId) {
        MongoCollection<Document> collection = database.getCollection("siege_wars");
        Document filter = new Document("siege_id", siegeId);
        Document doc = collection.find(filter).first();
        return doc != null ? new HashMap<>(doc) : new HashMap<>();
    }

    @Override
    public void savePlayerAchievement(UUID playerId, String achievementId) {
        MongoCollection<Document> collection = database.getCollection("player_achievements");
        Document filter = new Document("player_id", playerId.toString()).append("achievement_id", achievementId);
        Document doc = new Document("player_id", playerId.toString()).append("achievement_id", achievementId);
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public List<String> loadPlayerAchievements(UUID playerId) {
        List<String> achievements = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("player_achievements");
        Document filter = new Document("player_id", playerId.toString());

        try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                achievements.add(doc.getString("achievement_id"));
            }
        }
        return achievements;
    }

    @Override
    public void saveSeasonData(String currentSeason, int currentDay, int daysPerSubSeason) {
        MongoCollection<Document> collection = database.getCollection("season_data");
        Document filter = new Document("id", 1);
        Document doc = new Document("id", 1)
                .append("current_season", currentSeason)
                .append("current_day", currentDay)
                .append("days_per_sub_season", daysPerSubSeason);
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public Map<String, Object> loadSeasonData() {
        MongoCollection<Document> collection = database.getCollection("season_data");
        Document filter = new Document("id", 1);
        Document doc = collection.find(filter).first();

        if (doc != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("current_season", doc.getString("current_season"));
            data.put("current_day", doc.getInteger("current_day"));
            data.put("days_per_sub_season", doc.getInteger("days_per_sub_season"));
            return data;
        }
        return null;
    }
}
