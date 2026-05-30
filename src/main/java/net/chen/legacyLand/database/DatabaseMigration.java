package net.chen.legacyLand.database;

import net.chen.legacyLand.LegacyLand;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * 数据库自动迁移系统
 * 当新版本增加了表或列时，自动更新旧数据库结构，无需手动操作
 */
public class DatabaseMigration {

    private final Connection connection;
    private final String dbType; // "mysql" or "sqlite"
    private final Logger logger;

    /**
     * 列定义
     */
    public static class ColumnDef {
        public final String name;
        public final String type;   // 数据库无关的类型名（如 TEXT/VARCHAR, INTEGER, REAL/DOUBLE, BIGINT）
        public final boolean nullable;
        public final String defaultValue; // 可为 null 表示无默认值

        public ColumnDef(String name, String type, boolean nullable, String defaultValue) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
        }

        public ColumnDef(String name, String type) {
            this(name, type, true, null);
        }
    }

    /**
     * 表定义
     */
    public static class TableDef {
        public final String name;
        public final List<ColumnDef> columns;
        public final String primaryKey;      // 主键列名（复合主键用逗号分隔）
        public final List<String> uniqueKeys; // UNIQUE 约束列组
        public final List<String> indexes;    // 需要创建的索引列/表达式

        public TableDef(String name, String primaryKey) {
            this.name = name;
            this.columns = new ArrayList<>();
            this.primaryKey = primaryKey;
            this.uniqueKeys = new ArrayList<>();
            this.indexes = new ArrayList<>();
        }

        public TableDef addColumn(String colName, String colType, boolean nullable, String defaultValue) {
            columns.add(new ColumnDef(colName, colType, nullable, defaultValue));
            return this;
        }

        public TableDef addColumn(String colName, String colType) {
            return addColumn(colName, colType, true, null);
        }

        public TableDef addUniqueKey(String columns) {
            uniqueKeys.add(columns);
            return this;
        }

        public TableDef addIndex(String column) {
            indexes.add(column);
            return this;
        }
    }

    public DatabaseMigration(Connection connection, String dbType) {
        this.connection = connection;
        this.dbType = dbType != null ? dbType.toLowerCase() : "sqlite";
        this.logger = LegacyLand.logger;
    }

    /**
     * 定义所有预期表结构（当前版本）
     * 新增表只需在此添加定义即可自动创建
     */
    private List<TableDef> getExpectedTables() {
        List<TableDef> tables = new ArrayList<>();

        boolean mysql = "mysql".equals(dbType);
        String textType = mysql ? "VARCHAR(255)" : "TEXT";
        String uuidType = mysql ? "VARCHAR(36)" : "TEXT";
        String intType = "INTEGER";
        String bigintType = "BIGINT";
        String doubleType = mysql ? "DOUBLE" : "REAL";
        String boolType = mysql ? "TINYINT" : "INTEGER";

        // ---- nation_extensions ----
        tables.add(new TableDef("nation_extensions", "nation_name")
                .addColumn("nation_name", textType, false, null)
                .addColumn("government_type", textType, false, null));

        // ---- player_roles ----
        tables.add(new TableDef("player_roles", "nation_name, player_id")
                .addColumn("nation_name", textType, false, null)
                .addColumn("player_id", uuidType, false, null)
                .addColumn("role", textType, false, null));

        // ---- diplomacy_relations ----
        tables.add(new TableDef("diplomacy_relations", "id")
                .addColumn("id", intType, false, null)
                .addColumn("nation1", textType, false, null)
                .addColumn("nation2", textType, false, null)
                .addColumn("relation_type", textType, false, null)
                .addColumn("established_time", bigintType, false, null)
                .addUniqueKey("nation1, nation2"));

        // ---- players ----
        tables.add(new TableDef("players", "player_id")
                .addColumn("player_id", uuidType, false, null)
                .addColumn("player_name", mysql ? "VARCHAR(16)" : "TEXT", false, null)
                .addColumn("max_health", doubleType, true, "15.0")
                .addColumn("hydration", intType, true, "10")
                .addColumn("temperature", doubleType, true, "22.0")
                .addColumn("main_profession", textType, true, null)
                .addColumn("main_profession_level", intType, true, "0")
                .addColumn("main_profession_exp", intType, true, "0")
                .addColumn("sub_profession", textType, true, null)
                .addColumn("sub_profession_level", intType, true, "0")
                .addColumn("sub_profession_exp", intType, true, "0")
                .addColumn("talent_points", intType, true, "10"));

        // ---- wars ----
        tables.add(new TableDef("wars", "war_name")
                .addColumn("war_name", textType, false, null)
                .addColumn("war_type", textType, false, null)
                .addColumn("attacker_nation", textType, false, null)
                .addColumn("defender_nation", textType, false, null)
                .addColumn("attacker_town", textType, false, null)
                .addColumn("defender_town", textType, false, null)
                .addColumn("status", textType, false, null)
                .addColumn("start_time", bigintType, false, null)
                .addColumn("end_time", bigintType, true, null)
                .addColumn("attacker_supplies", intType, true, "10")
                .addColumn("defender_supplies", intType, true, "10"));

        // ---- war_participants ----
        tables.add(new TableDef("war_participants", "war_name, player_id")
                .addColumn("war_name", textType, false, null)
                .addColumn("player_id", uuidType, false, null)
                .addColumn("role", textType, false, null));

        // ---- siege_wars ----
        tables.add(new TableDef("siege_wars", "siege_id")
                .addColumn("siege_id", textType, false, null)
                .addColumn("war_name", textType, false, null)
                .addColumn("attacker_town", textType, false, null)
                .addColumn("defender_town", textType, false, null)
                .addColumn("outpost_location", "TEXT", true, null)
                .addColumn("outpost_establish_time", bigintType, true, null)
                .addColumn("outpost_active", boolType, true, "0"));

        // ---- player_achievements ----
        tables.add(new TableDef("player_achievements", "player_id, achievement_id")
                .addColumn("player_id", uuidType, false, null)
                .addColumn("achievement_id", textType, false, null));

        // ---- season_data ----
        tables.add(new TableDef("season_data", "id")
                .addColumn("id", intType, false, "1")
                .addColumn("current_season", textType, false, null)
                .addColumn("current_day", intType, false, null)
                .addColumn("days_per_sub_season", intType, false, null));

        // ---- flag_wars ----
        tables.add(new TableDef("flag_wars", "flag_war_id")
                .addColumn("flag_war_id", uuidType, false, null)
                .addColumn("attacker_id", uuidType, false, null)
                .addColumn("attacker_nation", textType, false, null)
                .addColumn("attacker_town", textType, false, null)
                .addColumn("defender_nation", textType, false, null)
                .addColumn("defender_town", textType, false, null)
                .addColumn("flag_location", textType, false, null)
                .addColumn("timer_block_location", textType, false, null)
                .addColumn("beacon_location", textType, false, null)
                .addColumn("start_time", bigintType, false, null)
                .addColumn("end_time", bigintType, true, null)
                .addColumn("status", textType, false, null)
                .addColumn("timer_progress", intType, true, "0")
                .addColumn("staking_fee", doubleType, true, "0")
                .addColumn("defense_break_fee", doubleType, true, "0")
                .addColumn("victory_cost", doubleType, true, "0")
                .addColumn("town_block_coords", "TEXT", true, null)
                .addColumn("is_home_block", boolType, true, "0"));

        // ---- guarantee_relations ----
        tables.add(new TableDef("guarantee_relations", "id")
                .addColumn("id", intType, false, null)
                .addColumn("guarantor_nation", textType, false, null)
                .addColumn("protected_nation", textType, false, null)
                .addColumn("established_time", bigintType, false, null)
                .addColumn("last_maintenance_time", bigintType, false, null)
                .addColumn("active", boolType, true, "1")
                .addUniqueKey("guarantor_nation, protected_nation"));

        // ---- chunk_resources ----
        tables.add(new TableDef("chunk_resources", "world, chunk_x, chunk_z")
                .addColumn("world", mysql ? "VARCHAR(64)" : "TEXT", false, null)
                .addColumn("chunk_x", intType, false, null)
                .addColumn("chunk_z", intType, false, null)
                .addColumn("biome", mysql ? "VARCHAR(64)" : "TEXT", true, null)
                .addColumn("initial_value", doubleType, false, null)
                .addColumn("current_value", doubleType, false, null)
                .addColumn("biome_factor", doubleType, false, null)
                .addColumn("last_scan", bigintType, false, null));

        // ---- markets ----
        tables.add(new TableDef("markets", "id")
                .addColumn("id", uuidType, false, null)
                .addColumn("nation_name", textType, false, null)
                .addColumn("world", mysql ? "VARCHAR(64)" : "TEXT", false, null)
                .addColumn("chunk_x", intType, false, null)
                .addColumn("chunk_z", intType, false, null)
                .addColumn("approved_by", uuidType, false, null)
                .addColumn("created_at", bigintType, false, null));

        // ---- market_chests ----
        tables.add(new TableDef("market_chests", "id")
                .addColumn("id", uuidType, false, null)
                .addColumn("market_id", uuidType, false, null)
                .addColumn("world", mysql ? "VARCHAR(64)" : "TEXT", false, null)
                .addColumn("x", intType, false, null)
                .addColumn("y", intType, false, null)
                .addColumn("z", intType, false, null)
                .addColumn("owner_uuid", uuidType, false, null)
                .addColumn("price_per_item", doubleType, true, "0")
                .addColumn("price_set", intType, true, "0")
                .addColumn("created_at", bigintType, false, null)
                .addIndex("market_id"));

        return tables;
    }

    /**
     * 执行完整迁移流程
     */
    public void migrate() {
        try {
            logger.info("开始检查数据库结构迁移...");
            createVersionTable();
            int currentVersion = getCurrentVersion();
            logger.info("数据库当前版本: " + currentVersion);

            // 处理缺失的表和列（idempotent）
            ensureAllTablesAndColumns();

            // 后续版本迁移（按版本号递增）
            applyPendingMigrations(currentVersion);

            logger.info("数据库结构迁移检查完成");
        } catch (SQLException e) {
            logger.severe("数据库迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建版本跟踪表
     */
    private void createVersionTable() throws SQLException {
        String sql;
        if ("mysql".equals(dbType)) {
            sql = "CREATE TABLE IF NOT EXISTS schema_version (" +
                  "id INT PRIMARY KEY AUTO_INCREMENT," +
                  "version INT NOT NULL," +
                  "applied_at BIGINT NOT NULL" +
                  ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS schema_version (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                  "version INTEGER NOT NULL," +
                  "applied_at BIGINT NOT NULL" +
                  ")";
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 获取当前 schema 版本号
     */
    private int getCurrentVersion() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) AS v FROM schema_version")) {
            if (rs.next()) {
                int v = rs.getInt("v");
                return rs.wasNull() ? 0 : v;
            }
        } catch (SQLException e) {
            // 表可能不存在，返回 0
        }
        return 0;
    }

    /**
     * 记录版本号
     */
    private void recordVersion(int version) throws SQLException {
        String sql = "INSERT INTO schema_version (version, applied_at) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, version);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
        logger.info("数据库迁移到版本 " + version);
    }

    /**
     * 确保所有预期表存在，且所有预期列存在
     */
    private void ensureAllTablesAndColumns() throws SQLException {
        Set<String> existingTables = getExistingTables();

        for (TableDef table : getExpectedTables()) {
            if (!existingTables.contains(table.name.toLowerCase())) {
                createTable(table);
            } else {
                ensureColumns(table);
            }
        }
    }

    /**
     * 获取数据库中已存在的表名集合
     */
    private Set<String> getExistingTables() throws SQLException {
        Set<String> tables = new HashSet<>();
        if ("mysql".equals(dbType)) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                while (rs.next()) {
                    tables.add(rs.getString(1).toLowerCase());
                }
            }
        } else {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT name FROM sqlite_master WHERE type='table'")) {
                while (rs.next()) {
                    tables.add(rs.getString("name").toLowerCase());
                }
            }
        }
        return tables;
    }

    /**
     * 创建全新表
     */
    private void createTable(TableDef table) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(table.name).append(" (");

        List<String> colDefs = new ArrayList<>();
        for (ColumnDef col : table.columns) {
            StringBuilder colDef = new StringBuilder(col.name + " " + col.type);
            if (!col.nullable) {
                colDef.append(" NOT NULL");
            }
            if (col.defaultValue != null) {
                colDef.append(" DEFAULT ").append(col.defaultValue);
            }
            colDefs.add(colDef.toString());
        }
        colDefs.add("PRIMARY KEY (" + table.primaryKey + ")");

        sql.append(String.join(", ", colDefs));
        sql.append(")");

        if ("mysql".equals(dbType)) {
            sql.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
            logger.info("已创建表: " + table.name);
        }

        // 创建索引
        for (String indexCol : table.indexes) {
            String indexName = "idx_" + table.name + "_" + indexCol.replaceAll("[,\\s]+", "_");
            String indexSql;
            if ("mysql".equals(dbType)) {
                indexSql = "CREATE INDEX " + indexName + " ON " + table.name + " (" + indexCol + ")";
            } else {
                indexSql = "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + table.name + " (" + indexCol + ")";
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(indexSql);
            } catch (SQLException e) {
                // 索引可能已存在（MySQL 不支持 IF NOT EXISTS）
                if (!"mysql".equals(dbType) || !e.getMessage().contains("Duplicate")) {
                    logger.warning("创建索引 " + indexName + " 失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 确保现有表包含所有预期列，缺失的通过 ALTER TABLE ADD COLUMN 补充
     */
    private void ensureColumns(TableDef table) throws SQLException {
        Set<String> existingCols = getTableColumns(table.name);

        for (ColumnDef col : table.columns) {
            if (!existingCols.contains(col.name.toLowerCase())) {
                addColumn(table.name, col);
            }
        }
    }

    /**
     * 获取表的现有列名集合
     */
    private Set<String> getTableColumns(String tableName) throws SQLException {
        Set<String> cols = new HashSet<>();
        if ("mysql".equals(dbType)) {
            String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tableName.toLowerCase());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        cols.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        } else {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
                while (rs.next()) {
                    cols.add(rs.getString("name").toLowerCase());
                }
            }
        }
        return cols;
    }

    /**
     * 为现有表添加缺失列
     */
    private void addColumn(String tableName, ColumnDef col) throws SQLException {
        StringBuilder sql = new StringBuilder("ALTER TABLE " + tableName + " ADD COLUMN ");
        sql.append(col.name).append(" ").append(col.type);
        if (!col.nullable) {
            sql.append(" NOT NULL");
        }
        if (col.defaultValue != null) {
            sql.append(" DEFAULT ").append(col.defaultValue);
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
            logger.info("已添加列: " + tableName + "." + col.name);
        }
    }

    /**
     * 按版本号应用增量迁移
     * 新增版本迁移在此按版本号 case 添加即可
     */
    private void applyPendingMigrations(int currentVersion) throws SQLException {
        // 版本 1: 基准版本（包含所有当前表结构）
        if (currentVersion < 1) {
            logger.info("应用基准 schema (version 1)...");
            recordVersion(1);
            currentVersion = 1;
        }

        // 未来版本迁移示例:
        // if (currentVersion < 2) {
        //     // 版本 2: 例如为 players 表新增 email 列
        //     ensureColumn("players", new ColumnDef("email", "TEXT", true, null));
        //     recordVersion(2);
        // }
    }
}
