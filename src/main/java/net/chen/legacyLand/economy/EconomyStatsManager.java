package net.chen.legacyLand.economy;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.util.FoliaSchedule;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 经济统计管理器
 * 实现 GDP 统计、通胀率监控、熔断机制
 */
public class EconomyStatsManager {
    private static EconomyStatsManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final EconomyDatabase database;
    private final TreasuryManager treasuryManager;
    private final BankManager bankManager;

    // 熔断阈值
    private static final double INFLATION_THRESHOLD = 0.5; // 50% 通胀率触发熔断
    private static final double DEFLATION_THRESHOLD = -0.3; // -30% 通缩率触发熔断

    private EconomyStatsManager(LegacyLand plugin, TreasuryManager treasuryManager, BankManager bankManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.database = EconomyDatabase.getInstance();
        this.treasuryManager = treasuryManager;
        this.bankManager = bankManager;
    }

    public static EconomyStatsManager getInstance(LegacyLand plugin, TreasuryManager treasuryManager, BankManager bankManager) {
        if (instance == null) {
            synchronized (EconomyStatsManager.class) {
                if (instance == null) {
                    instance = new EconomyStatsManager(plugin, treasuryManager, bankManager);
                }
            }
        }
        return instance;
    }

    public static EconomyStatsManager getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        createStatsTable();
        startMonitoring();
        logger.info("经济统计系统已加载");
    }

    /**
     * 创建统计表
     */
    private void createStatsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS economy_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nation_name TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                m0 REAL NOT NULL,
                m1 REAL NOT NULL,
                m2 REAL NOT NULL,
                gdp REAL NOT NULL,
                inflation_rate REAL NOT NULL,
                exchange_rate REAL NOT NULL,
                FOREIGN KEY (nation_name) REFERENCES treasuries(nation_name)
            )
        """;

        try (Statement stmt = database.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("经济统计表已创建");
        } catch (SQLException e) {
            logger.severe("创建经济统计表失败: " + e.getMessage());
        }
    }

    /**
     * 启动监控任务（每小时统计一次）
     */
    private void startMonitoring() {
//        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
//            for (TreasuryManager.Treasury treasury : treasuryManager.getAllTreasuries().values()) {
//                recordStats(treasury.getNationName());
//                checkCircuitBreaker(treasury.getNationName());
//            }
//        }, 20L * 60 * 60, 20L * 60 * 60); // 每小时执行一次
        FoliaSchedule.runAsyncRepeating(plugin, () -> {
            for (TreasuryManager.Treasury treasury : treasuryManager.getAllTreasuries().values()) {
                recordStats(treasury.getNationName());
                checkCircuitBreaker(treasury.getNationName());
            }
        }, 20L * 60 * 60, 20L * 60 * 60);
    }

    /**
     * 记录经济统计数据
     */
    public void recordStats(String nationName) {
        TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nationName);
        if (treasury == null) {
            return;
        }

        // M0: 实体货币（已发行货币）
        double m0 = treasury.getCurrencyIssued();

        // M1: M0 + 活期存款（银行余额）
        double deposits = getTotalDeposits(nationName);
        double m1 = m0 + deposits;

        // M2: M1 + 贷款（信用扩张）
        double loans = getTotalLoans(nationName);
        double m2 = m1 + loans;

        // GDP: 交易总额（过去24小时）
        double gdp = calculateGDP(nationName);

        // 通胀率: (当前M2 - 上次M2) / 上次M2
        double inflationRate = calculateInflationRate(nationName, m2);

        // 汇率
        double exchangeRate = treasury.calculateExchangeRate();

        String sql = """
            INSERT INTO economy_stats (nation_name, timestamp, m0, m1, m2, gdp, inflation_rate, exchange_rate)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ps.setLong(2, System.currentTimeMillis());
            ps.setDouble(3, m0);
            ps.setDouble(4, m1);
            ps.setDouble(5, m2);
            ps.setDouble(6, gdp);
            ps.setDouble(7, inflationRate);
            ps.setDouble(8, exchangeRate);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("记录经济统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取总存款
     */
    private double getTotalDeposits(String nationName) {
        String sql = "SELECT SUM(balance) as total FROM bank_accounts WHERE nation_name = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.warning("查询总存款失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 获取总贷款
     */
    private double getTotalLoans(String nationName) {
        String sql = "SELECT SUM(amount - repaid_amount) as total FROM loans WHERE nation_name = ? AND status = 'active'";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.warning("查询总贷款失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 计算 GDP（过去24小时交易总额）
     */
    private double calculateGDP(String nationName) {
        long dayAgo = System.currentTimeMillis() - (24L * 60 * 60 * 1000);
        String sql = "SELECT SUM(amount) as total FROM transactions WHERE nation_name = ? AND timestamp > ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ps.setLong(2, dayAgo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.warning("计算GDP失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 计算通胀率
     */
    private double calculateInflationRate(String nationName, double currentM2) {
        String sql = "SELECT m2 FROM economy_stats WHERE nation_name = ? ORDER BY timestamp DESC LIMIT 1";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double lastM2 = rs.getDouble("m2");
                if (lastM2 > 0) {
                    return (currentM2 - lastM2) / lastM2;
                }
            }
        } catch (SQLException e) {
            logger.warning("计算通胀率失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 检查熔断机制
     */
    private void checkCircuitBreaker(String nationName) {
        double inflationRate = getLatestInflationRate(nationName);

        if (inflationRate > INFLATION_THRESHOLD) {
            triggerCircuitBreaker(nationName, "恶性通胀", inflationRate);
        } else if (inflationRate < DEFLATION_THRESHOLD) {
            triggerCircuitBreaker(nationName, "严重通缩", inflationRate);
        }
    }

    /**
     * 获取最新通胀率
     */
    private double getLatestInflationRate(String nationName) {
        String sql = "SELECT inflation_rate FROM economy_stats WHERE nation_name = ? ORDER BY timestamp DESC LIMIT 1";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("inflation_rate");
            }
        } catch (SQLException e) {
            logger.warning("查询通胀率失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 触发熔断机制
     */
    private void triggerCircuitBreaker(String nationName, String reason, double rate) {
        logger.warning("【熔断警告】" + nationName + " 触发熔断: " + reason + " (" + String.format("%.2f%%", rate * 100) + ")");

        // 暂停该国的所有金融交易
        // TODO: 实现交易暂停逻辑

        // 通知所有在线玩家
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            player.sendMessage("§c§l【经济熔断】");
            player.sendMessage("§e" + nationName + " 经济出现异常: " + reason);
            player.sendMessage("§7通胀率: §c" + String.format("%.2f%%", rate * 100));
            player.sendMessage("§7所有金融交易已暂停，等待管理员处理");
        });
    }

    /**
     * 获取经济统计数据
     */
    public EconomyStats getStats(String nationName) {
        String sql = "SELECT * FROM economy_stats WHERE nation_name = ? ORDER BY timestamp DESC LIMIT 1";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new EconomyStats(
                    nationName,
                    rs.getLong("timestamp"),
                    rs.getDouble("m0"),
                    rs.getDouble("m1"),
                    rs.getDouble("m2"),
                    rs.getDouble("gdp"),
                    rs.getDouble("inflation_rate"),
                    rs.getDouble("exchange_rate")
                );
            }
        } catch (SQLException e) {
            logger.warning("查询经济统计失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取最新统计数据
     */
    public EconomyStats getLatestStats(String nationName) {
        String sql = "SELECT * FROM economy_stats WHERE nation_name = ? ORDER BY timestamp DESC LIMIT 1";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new EconomyStats(
                    nationName,
                    rs.getLong("timestamp"),
                    rs.getDouble("m0"),
                    rs.getDouble("m1"),
                    rs.getDouble("m2"),
                    rs.getDouble("gdp"),
                    rs.getDouble("inflation_rate"),
                    rs.getDouble("exchange_rate")
                );
            }
        } catch (SQLException e) {
            logger.warning("查询最新统计失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取 GDP 排行榜
     */
    public Map<String, Double> getGDPRanking() {
        Map<String, Double> ranking = new HashMap<>();
        String sql = """
            SELECT nation_name, gdp FROM economy_stats
            WHERE id IN (
                SELECT MAX(id) FROM economy_stats GROUP BY nation_name
            )
            ORDER BY gdp DESC
        """;

        try (Statement stmt = database.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                ranking.put(rs.getString("nation_name"), rs.getDouble("gdp"));
            }
        } catch (SQLException e) {
            logger.warning("查询 GDP 排行失败: " + e.getMessage());
        }

        return ranking;
    }

    /**
     * 经济统计数据类
     */
    public static class EconomyStats {
        public final String nationName;
        public final long timestamp;
        public final double m0;
        public final double m1;
        public final double m2;
        public final double gdp;
        public final double inflationRate;
        public final double exchangeRate;

        public EconomyStats(String nationName, long timestamp, double m0, double m1, double m2,
                           double gdp, double inflationRate, double exchangeRate) {
            this.nationName = nationName;
            this.timestamp = timestamp;
            this.m0 = m0;
            this.m1 = m1;
            this.m2 = m2;
            this.gdp = gdp;
            this.inflationRate = inflationRate;
            this.exchangeRate = exchangeRate;
        }
    }
}
