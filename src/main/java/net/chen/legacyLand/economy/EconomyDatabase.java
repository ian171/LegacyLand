package net.chen.legacyLand.economy;

import net.chen.legacyLand.LegacyLand;

import java.sql.*;
import java.util.logging.Logger;

/**
 * 经济系统数据库管理器
 * 独立的 SQLite 数据库，存储货币、国库、银行、汇率等数据
 */
public class EconomyDatabase {
    private static EconomyDatabase instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private Connection connection;

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
     * 连接数据库
     */
    public void connect() {
        try {
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/economy.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            logger.info("经济系统数据库已连接: " + dbPath);
            createTables();
        } catch (SQLException e) {
            logger.severe("连接经济数据库失败: " + e.getMessage());
        }
    }

    /**
     * 创建所有表
     */
    private void createTables() {
        createTreasuryTable();
        createCurrencyTable();
        createBankAccountTable();
        createExchangeRateTable();
        createTransactionTable();
    }

    /**
     * 国库表 - 存储各国国库信息
     */
    private void createTreasuryTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS treasuries (
                nation_name TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                sbc_reserve REAL NOT NULL DEFAULT 0,
                currency_issued REAL NOT NULL DEFAULT 0,
                credit_score REAL NOT NULL DEFAULT 1.0,
                created_at INTEGER NOT NULL,
                last_updated INTEGER NOT NULL
            )
        """;
        executeUpdate(sql, "国库表");
    }

    /**
     * 货币表 - 记录所有发行的货币（用于防伪和追踪）
     */
    private void createCurrencyTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS currencies (
                serial_number TEXT PRIMARY KEY,
                nation_name TEXT NOT NULL,
                denomination REAL NOT NULL,
                issued_at INTEGER NOT NULL,
                issued_by TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'active',
                FOREIGN KEY (nation_name) REFERENCES treasuries(nation_name)
            )
        """;
        executeUpdate(sql, "货币表");
    }

    /**
     * 银行账户表 - 存储玩家的电子余额（M2）
     */
    private void createBankAccountTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS bank_accounts (
                player_uuid TEXT NOT NULL,
                nation_name TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                last_transaction INTEGER NOT NULL,
                PRIMARY KEY (player_uuid, nation_name),
                FOREIGN KEY (nation_name) REFERENCES treasuries(nation_name)
            )
        """;
        executeUpdate(sql, "银行账户表");
    }

    /**
     * 汇率表 - 存储各国货币汇率历史
     */
    private void createExchangeRateTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS exchange_rates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nation_name TEXT NOT NULL,
                rate_to_sbc REAL NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (nation_name) REFERENCES treasuries(nation_name)
            )
        """;
        executeUpdate(sql, "汇率表");
    }

    /**
     * 交易记录表 - 记录所有经济活动
     */
    private void createTransactionTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                from_player TEXT,
                to_player TEXT,
                nation_name TEXT,
                amount REAL NOT NULL,
                description TEXT,
                timestamp INTEGER NOT NULL
            )
        """;
        executeUpdate(sql, "交易记录表");
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
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 断开数据库连接
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("经济系统数据库已断开");
            } catch (SQLException e) {
                logger.severe("断开经济数据库失败: " + e.getMessage());
            }
        }
    }
}
