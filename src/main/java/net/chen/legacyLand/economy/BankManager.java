package net.chen.legacyLand.economy;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 银行管理器
 * 管理玩家的电子存款（M2）、贷款、跨国交易
 */
public class BankManager {
    private static BankManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final EconomyDatabase database;
    private final TreasuryManager treasuryManager;

    // 缓存玩家账户余额
    private final Map<String, Double> balanceCache = new HashMap<>();

    private BankManager(LegacyLand plugin, TreasuryManager treasuryManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.database = EconomyDatabase.getInstance();
        this.treasuryManager = treasuryManager;
    }

    public static BankManager getInstance(LegacyLand plugin, TreasuryManager treasuryManager) {
        if (instance == null) {
            instance = new BankManager(plugin, treasuryManager);
        }
        return instance;
    }

    public static BankManager getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        logger.info("银行系统已加载");
    }

    /**
     * 存款 - 将实体货币转换为电子余额
     * @param player 玩家
     * @param nationName 国家名称
     * @param amount 金额
     * @return 是否成功
     */
    public boolean deposit(Player player, String nationName, double amount) {
        String cacheKey = getCacheKey(player.getUniqueId(), nationName);
        double currentBalance = getBalance(player.getUniqueId(), nationName);
        double newBalance = currentBalance + amount;

        String sql = """
            INSERT INTO bank_accounts (player_uuid, nation_name, balance, created_at, last_transaction)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, nation_name) DO UPDATE SET
                balance = ?,
                last_transaction = ?
        """;

        long now = System.currentTimeMillis();

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, nationName);
            ps.setDouble(3, newBalance);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.setDouble(6, newBalance);
            ps.setLong(7, now);
            ps.executeUpdate();

            // 更新缓存
            balanceCache.put(cacheKey, newBalance);

            // 记录交易
            recordTransaction("DEPOSIT", player.getUniqueId().toString(), null, nationName, amount, "存款");

            return true;
        } catch (SQLException e) {
            logger.severe("存款失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 取款 - 将电子余额转换为实体货币
     * @param player 玩家
     * @param nationName 国家名称
     * @param amount 金额
     * @return 是否成功
     */
    public boolean withdraw(Player player, String nationName, double amount) {
        double currentBalance = getBalance(player.getUniqueId(), nationName);

        if (currentBalance < amount) {
            return false;
        }

        String cacheKey = getCacheKey(player.getUniqueId(), nationName);
        double newBalance = currentBalance - amount;

        String sql = """
            UPDATE bank_accounts
            SET balance = ?, last_transaction = ?
            WHERE player_uuid = ? AND nation_name = ?
        """;

        long now = System.currentTimeMillis();

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setLong(2, now);
            ps.setString(3, player.getUniqueId().toString());
            ps.setString(4, nationName);
            ps.executeUpdate();

            // 更新缓存
            balanceCache.put(cacheKey, newBalance);

            // 记录交易
            recordTransaction("WITHDRAW", player.getUniqueId().toString(), null, nationName, amount, "取款");

            return true;
        } catch (SQLException e) {
            logger.severe("取款失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 转账
     * @param from 转出玩家
     * @param to 转入玩家
     * @param nationName 国家名称
     * @param amount 金额
     * @return 是否成功
     */
    public boolean transfer(Player from, UUID toUuid, String nationName, double amount) {
        double fromBalance = getBalance(from.getUniqueId(), nationName);

        if (fromBalance < amount) {
            return false;
        }

        // 扣除转出方余额
        String fromKey = getCacheKey(from.getUniqueId(), nationName);
        double newFromBalance = fromBalance - amount;

        // 增加转入方余额
        double toBalance = getBalance(toUuid, nationName);
        String toKey = getCacheKey(toUuid, nationName);
        double newToBalance = toBalance + amount;

        long now = System.currentTimeMillis();

        try {
            database.getConnection().setAutoCommit(false);

            // 更新转出方
            String sql1 = """
                UPDATE bank_accounts
                SET balance = ?, last_transaction = ?
                WHERE player_uuid = ? AND nation_name = ?
            """;
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql1)) {
                ps.setDouble(1, newFromBalance);
                ps.setLong(2, now);
                ps.setString(3, from.getUniqueId().toString());
                ps.setString(4, nationName);
                ps.executeUpdate();
            }

            // 更新转入方
            String sql2 = """
                INSERT INTO bank_accounts (player_uuid, nation_name, balance, created_at, last_transaction)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid, nation_name) DO UPDATE SET
                    balance = ?,
                    last_transaction = ?
            """;
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql2)) {
                ps.setString(1, toUuid.toString());
                ps.setString(2, nationName);
                ps.setDouble(3, newToBalance);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.setDouble(6, newToBalance);
                ps.setLong(7, now);
                ps.executeUpdate();
            }

            database.getConnection().commit();
            database.getConnection().setAutoCommit(true);

            // 更新缓存
            balanceCache.put(fromKey, newFromBalance);
            balanceCache.put(toKey, newToBalance);

            // 记录交易
            recordTransaction("TRANSFER", from.getUniqueId().toString(), toUuid.toString(),
                nationName, amount, "转账");

            return true;
        } catch (SQLException e) {
            try {
                database.getConnection().rollback();
                database.getConnection().setAutoCommit(true);
            } catch (SQLException ex) {
                logger.severe("回滚失败: " + ex.getMessage());
            }
            logger.severe("转账失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取余额
     */
    public double getBalance(UUID playerUuid, String nationName) {
        String cacheKey = getCacheKey(playerUuid, nationName);

        // 先查缓存
        if (balanceCache.containsKey(cacheKey)) {
            return balanceCache.get(cacheKey);
        }

        // 查数据库
        String sql = "SELECT balance FROM bank_accounts WHERE player_uuid = ? AND nation_name = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                balanceCache.put(cacheKey, balance);
                return balance;
            }
        } catch (SQLException e) {
            logger.severe("查询余额失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 跨国兑换
     * @param player 玩家
     * @param fromNation 源国家
     * @param toNation 目标国家
     * @param amount 源货币金额
     * @param tariffRate 关税率（0-1）
     * @return 是否成功
     */
    public boolean exchange(Player player, String fromNation, String toNation, double amount, double tariffRate) {
        // 检查余额
        double fromBalance = getBalance(player.getUniqueId(), fromNation);
        if (fromBalance < amount) {
            return false;
        }

        // 获取汇率
        TreasuryManager.Treasury fromTreasury = treasuryManager.getTreasury(fromNation);
        TreasuryManager.Treasury toTreasury = treasuryManager.getTreasury(toNation);

        if (fromTreasury == null || toTreasury == null) {
            return false;
        }

        double fromRate = fromTreasury.calculateExchangeRate();
        double toRate = toTreasury.calculateExchangeRate();

        if (fromRate == 0 || toRate == 0) {
            return false;
        }

        // 计算兑换金额
        double sbcAmount = amount * fromRate; // 先转换为 SBC
        double tariff = sbcAmount * tariffRate; // 计算关税
        double afterTariff = sbcAmount - tariff; // 扣除关税
        double toAmount = afterTariff / toRate; // 转换为目标货币

        // 扣除源货币
        String fromKey = getCacheKey(player.getUniqueId(), fromNation);
        double newFromBalance = fromBalance - amount;

        // 增加目标货币
        double toBalance = getBalance(player.getUniqueId(), toNation);
        String toKey = getCacheKey(player.getUniqueId(), toNation);
        double newToBalance = toBalance + toAmount;

        long now = System.currentTimeMillis();

        try {
            database.getConnection().setAutoCommit(false);

            // 更新源货币账户
            String sql1 = """
                UPDATE bank_accounts
                SET balance = ?, last_transaction = ?
                WHERE player_uuid = ? AND nation_name = ?
            """;
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql1)) {
                ps.setDouble(1, newFromBalance);
                ps.setLong(2, now);
                ps.setString(3, player.getUniqueId().toString());
                ps.setString(4, fromNation);
                ps.executeUpdate();
            }

            // 更新目标货币账户
            String sql2 = """
                INSERT INTO bank_accounts (player_uuid, nation_name, balance, created_at, last_transaction)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid, nation_name) DO UPDATE SET
                    balance = ?,
                    last_transaction = ?
            """;
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql2)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, toNation);
                ps.setDouble(3, newToBalance);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.setDouble(6, newToBalance);
                ps.setLong(7, now);
                ps.executeUpdate();
            }

            database.getConnection().commit();
            database.getConnection().setAutoCommit(true);

            // 更新缓存
            balanceCache.put(fromKey, newFromBalance);
            balanceCache.put(toKey, newToBalance);

            // 记录交易
            recordTransaction("EXCHANGE", player.getUniqueId().toString(), null,
                fromNation + "->" + toNation, amount,
                String.format("兑换: %.2f %s -> %.2f %s (关税: %.2f%%)",
                    amount, fromNation, toAmount, toNation, tariffRate * 100));

            return true;
        } catch (SQLException e) {
            try {
                database.getConnection().rollback();
                database.getConnection().setAutoCommit(true);
            } catch (SQLException ex) {
                logger.severe("回滚失败: " + ex.getMessage());
            }
            logger.severe("兑换失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 记录交易
     */
    private void recordTransaction(String type, String from, String to, String nation, double amount, String description) {
        String sql = """
            INSERT INTO transactions (type, from_player, to_player, nation_name, amount, description, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, from);
            ps.setString(3, to);
            ps.setString(4, nation);
            ps.setDouble(5, amount);
            ps.setString(6, description);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("记录交易失败: " + e.getMessage());
        }
    }

    /**
     * 获取玩家所有国家的余额
     */
    public Map<String, Double> getAllBalances(UUID playerUuid) {
        Map<String, Double> balances = new HashMap<>();
        String sql = "SELECT nation_name, balance FROM bank_accounts WHERE player_uuid = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nation = rs.getString("nation_name");
                double balance = rs.getDouble("balance");
                balances.put(nation, balance);
                // 更新缓存
                balanceCache.put(getCacheKey(playerUuid, nation), balance);
            }
        } catch (SQLException e) {
            logger.severe("查询所有余额失败: " + e.getMessage());
        }

        return balances;
    }

    /**
     * 跨国兑换货币（简化版，返回兑换后的金额）
     */
    public double exchangeCurrency(Player player, String fromNation, String toNation, double amount) {
        // 使用默认关税率 15%
        boolean success = exchange(player, fromNation, toNation, amount, 0.15);
        if (!success) {
            return 0;
        }

        // 计算兑换后的金额
        TreasuryManager.Treasury fromTreasury = treasuryManager.getTreasury(fromNation);
        TreasuryManager.Treasury toTreasury = treasuryManager.getTreasury(toNation);

        if (fromTreasury == null || toTreasury == null) {
            return 0;
        }

        double fromRate = fromTreasury.calculateExchangeRate();
        double toRate = toTreasury.calculateExchangeRate();

        if (fromRate == 0 || toRate == 0) {
            return 0;
        }

        double sbcAmount = amount * fromRate;
        double afterTariff = sbcAmount * 0.85; // 扣除 15% 关税
        return afterTariff / toRate;
    }

    /**
     * 生成缓存键
     */
    private String getCacheKey(UUID playerUuid, String nationName) {
        return playerUuid.toString() + ":" + nationName;
    }
}
