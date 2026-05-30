package net.chen.legacyLand.economy;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.database.DatabaseManager;

import java.sql.*;
import java.util.logging.Logger;

/**
 * 经济系统数据库管理器
 * 支持跟随主数据库配置使用 SQLite 或 MySQL，独立 SQLite 作为 fallback
 */
public class EconomyDatabase {
    private static EconomyDatabase instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private Connection connection;
    private DatabaseManager databaseManager; // 共享模式时引用主数据库管理器
    private String dbType = "sqlite"; // "mysql" or "sqlite"

    private EconomyDatabase(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static EconomyDatabase getInstance(LegacyLand plugin) {
        if (instance == null) {
            instance = new EconomyDatabase(plugin);
        }
        return instance;
    }

    public static EconomyDatabase getInstance() {
        return instance;
    }

    /**
     * 使用主数据库连接（跟随 database.type 配置）
     */
    public void connect(DatabaseManager dbManager, String dbType) {
        this.databaseManager = dbManager;
        this.dbType = dbType != null ? dbType.toLowerCase() : "sqlite";
        logger.info("经济系统使用共享数据库连接 (" + this.dbType + ")");
        // 通过共享连接创建表
        try (Connection conn = dbManager.getConnection()) {
            this.connection = conn; // 临时用于 createTables
            createTables();
        } catch (SQLException e) {
            logger.severe("经济系统创建表失败: " + e.getMessage());
        } finally {
            this.connection = null; // 清除临时引用，后续 getConnection() 走共享路径
        }
    }

    /**
     * 独立 SQLite 连接（fallback）
     */
    public void connect() {
        try {
            this.dbType = "sqlite";
            this.databaseManager = null;
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/economy.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            logger.info("经济系统数据库已连接 (独立SQLite): " + dbPath);
            createTables();
        } catch (SQLException e) {
            logger.severe("连接经济数据库失败: " + e.getMessage());
        }
    }

    /**
     * 创建所有表（适配 SQLite 和 MySQL）
     */
    private void createTables() {
        String tt = textType();
        String rt = realType();
        String bt = "BIGINT";
        String ec = engineCharset();
        String ut = uuidType();

        executeUpdate(
            "CREATE TABLE IF NOT EXISTS treasuries (" +
            "nation_name " + tt + " PRIMARY KEY," +
            "world " + tt + " NOT NULL," +
            "x INTEGER NOT NULL," +
            "y INTEGER NOT NULL," +
            "z INTEGER NOT NULL," +
            "sbc_reserve " + rt + " NOT NULL DEFAULT 0," +
            "currency_issued " + rt + " NOT NULL DEFAULT 0," +
            "credit_score " + rt + " NOT NULL DEFAULT 1.0," +
            "created_at " + bt + " NOT NULL," +
            "last_updated " + bt + " NOT NULL" +
            ")" + ec, "国库表");

        executeUpdate(
            "CREATE TABLE IF NOT EXISTS currencies (" +
            "serial_number " + tt + " PRIMARY KEY," +
            "nation_name " + tt + " NOT NULL," +
            "denomination " + rt + " NOT NULL," +
            "issued_at " + bt + " NOT NULL," +
            "issued_by " + tt + " NOT NULL," +
            "status " + tt + " NOT NULL DEFAULT 'active'" +
            ")" + ec, "货币表");

        executeUpdate(
            "CREATE TABLE IF NOT EXISTS bank_accounts (" +
            "player_uuid " + ut + " NOT NULL," +
            "nation_name " + tt + " NOT NULL," +
            "balance " + rt + " NOT NULL DEFAULT 0," +
            "created_at " + bt + " NOT NULL," +
            "last_transaction " + bt + " NOT NULL," +
            "PRIMARY KEY (player_uuid, nation_name)" +
            ")" + ec, "银行账户表");

        executeUpdate(
            "CREATE TABLE IF NOT EXISTS exchange_rates (" +
            "id " + pkIntType() + "," +
            "nation_name " + tt + " NOT NULL," +
            "rate_to_sbc " + rt + " NOT NULL," +
            "timestamp " + bt + " NOT NULL" +
            ")" + ec, "汇率表");

        executeUpdate(
            "CREATE TABLE IF NOT EXISTS transactions (" +
            "id " + pkIntType() + "," +
            "type " + tt + " NOT NULL," +
            "from_player " + ut + "," +
            "to_player " + ut + "," +
            "nation_name " + tt + "," +
            "amount " + rt + " NOT NULL," +
            "description " + tt + "," +
            "timestamp " + bt + " NOT NULL" +
            ")" + ec, "交易记录表");
    }

    /**
     * 执行更新语句
     */
    private void executeUpdate(String sql, String tableName) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("经济系统表已创建: " + tableName);
        } catch (SQLException e) {
            logger.severe("创建" + tableName + "失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据库连接
     * 共享模式：从 DatabaseManager 获取（连接池）
     * 独立模式：返回自己的 SQLite 连接
     */
    public Connection getConnection() {
        if (databaseManager != null) {
            try {
                return databaseManager.getConnection();
            } catch (SQLException e) {
                logger.severe("获取数据库连接失败: " + e.getMessage());
                return null;
            }
        }
        return connection;
    }

    /**
     * 获取数据库类型
     */
    public String getDbType() {
        return dbType;
    }

    /** 是否为 MySQL */
    public boolean isMysql() {
        return "mysql".equals(dbType);
    }

    /** 数据库适配：文本类型 */
    public String textType() {
        return isMysql() ? "VARCHAR(255)" : "TEXT";
    }

    /** 数据库适配：UUID 列类型 */
    public String uuidType() {
        return isMysql() ? "VARCHAR(36)" : "TEXT";
    }

    /** 数据库适配：自增整数主键 */
    public String pkIntType() {
        return isMysql() ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    /** 数据库适配：浮点数类型 */
    public String realType() {
        return isMysql() ? "DOUBLE" : "REAL";
    }

    /** 数据库适配：表引擎/字符集后缀 */
    public String engineCharset() {
        return isMysql() ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : "";
    }

    /**
     * 断开数据库连接（仅独立 SQLite 模式才关闭）
     */
    public void disconnect() {
        if (databaseManager == null && connection != null) {
            try {
                connection.close();
                logger.info("经济系统数据库已断开");
            } catch (SQLException e) {
                logger.severe("断开经济数据库失败: " + e.getMessage());
            }
        }
    }
}
