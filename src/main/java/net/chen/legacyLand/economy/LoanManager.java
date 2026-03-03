package net.chen.legacyLand.economy;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 贷款管理器
 * 实现部分准备金制度、信用扩张、贷款系统
 */
public class LoanManager {
    private static LoanManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final EconomyDatabase database;
    private final BankManager bankManager;

    // 准备金率（10%）
    private static final double RESERVE_RATIO = 0.1;
    // 贷款利率（年化 5%）
    private static final double INTEREST_RATE = 0.05;
    // 贷款期限（天）
    private static final int LOAN_DURATION_DAYS = 30;

    private LoanManager(LegacyLand plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.database = EconomyDatabase.getInstance();
        this.bankManager = bankManager;
    }

    public static LoanManager getInstance(LegacyLand plugin, BankManager bankManager) {
        if (instance == null) {
            instance = new LoanManager(plugin, bankManager);
        }
        return instance;
    }

    public static LoanManager getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        createLoanTable();
        logger.info("贷款系统已加载");
    }

    /**
     * 创建贷款表
     */
    private void createLoanTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                nation_name TEXT NOT NULL,
                amount REAL NOT NULL,
                interest_rate REAL NOT NULL,
                issued_at INTEGER NOT NULL,
                due_at INTEGER NOT NULL,
                repaid_amount REAL NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'active',
                FOREIGN KEY (nation_name) REFERENCES treasuries(nation_name)
            )
        """;

        try (Statement stmt = database.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("贷款表已创建");
        } catch (SQLException e) {
            logger.severe("创建贷款表失败: " + e.getMessage());
        }
    }

    /**
     * 申请贷款
     * @param player 玩家
     * @param nationName 国家名称
     * @param amount 贷款金额
     * @return 是否成功
     */
    public boolean applyLoan(Player player, String nationName, double amount) {
        // 检查银行是否有足够的可贷资金
        double availableFunds = getAvailableLoanFunds(nationName);
        if (availableFunds < amount) {
            return false;
        }

        // 检查玩家是否有未还清的贷款
        if (hasActiveLoan(player.getUniqueId(), nationName)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long dueDate = now + (LOAN_DURATION_DAYS * 24L * 60 * 60 * 1000);

        String sql = """
            INSERT INTO loans (player_uuid, nation_name, amount, interest_rate, issued_at, due_at, status)
            VALUES (?, ?, ?, ?, ?, ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, nationName);
            ps.setDouble(3, amount);
            ps.setDouble(4, INTEREST_RATE);
            ps.setLong(5, now);
            ps.setLong(6, dueDate);
            ps.executeUpdate();

            // 将贷款金额存入玩家账户
            bankManager.deposit(player, nationName, amount);

            return true;
        } catch (SQLException e) {
            logger.severe("申请贷款失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 还款
     * @param player 玩家
     * @param loanId 贷款 ID
     * @param amount 还款金额
     * @return 是否成功
     */
    public boolean repayLoan(Player player, int loanId, double amount) {
        Loan loan = getLoan(loanId);
        if (loan == null || !loan.playerUuid.equals(player.getUniqueId().toString())) {
            return false;
        }

        if (!loan.status.equals("active")) {
            return false;
        }

        // 检查余额
        double balance = bankManager.getBalance(player.getUniqueId(), loan.nationName);
        if (balance < amount) {
            return false;
        }

        // 计算应还总额
        double totalDue = calculateTotalDue(loan);
        double newRepaidAmount = loan.repaidAmount + amount;

        String status = newRepaidAmount >= totalDue ? "repaid" : "active";

        String sql = """
            UPDATE loans
            SET repaid_amount = ?, status = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, newRepaidAmount);
            ps.setString(2, status);
            ps.setInt(3, loanId);
            ps.executeUpdate();

            // 扣除余额（实际上是销毁货币）
            bankManager.withdraw(player, loan.nationName, amount);

            return true;
        } catch (SQLException e) {
            logger.severe("还款失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取可贷资金
     * 根据部分准备金制度，银行可以贷出存款的 90%
     */
    private double getAvailableLoanFunds(String nationName) {
        // 获取该国所有存款总额
        double totalDeposits = getTotalDeposits(nationName);
        // 获取已贷出金额
        double totalLoans = getTotalActiveLoans(nationName);
        // 可贷金额 = 总存款 × (1 - 准备金率) - 已贷出
        return totalDeposits * (1 - RESERVE_RATIO) - totalLoans;
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
            logger.severe("查询总存款失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 获取活跃贷款总额
     */
    private double getTotalActiveLoans(String nationName) {
        String sql = "SELECT SUM(amount - repaid_amount) as total FROM loans WHERE nation_name = ? AND status = 'active'";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.severe("查询活跃贷款失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 检查是否有活跃贷款
     */
    private boolean hasActiveLoan(UUID playerUuid, String nationName) {
        String sql = "SELECT COUNT(*) as count FROM loans WHERE player_uuid = ? AND nation_name = ? AND status = 'active'";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, nationName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            logger.severe("检查活跃贷款失败: " + e.getMessage());
        }

        return false;
    }

    /**
     * 获取贷款信息
     */
    public Loan getLoan(int loanId) {
        String sql = "SELECT * FROM loans WHERE id = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Loan(
                    rs.getInt("id"),
                    rs.getString("player_uuid"),
                    rs.getString("nation_name"),
                    rs.getDouble("amount"),
                    rs.getDouble("interest_rate"),
                    rs.getLong("issued_at"),
                    rs.getLong("due_at"),
                    rs.getDouble("repaid_amount"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            logger.severe("获取贷款信息失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取玩家的所有贷款
     */
    public List<Loan> getPlayerLoans(UUID playerUuid, String nationName) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE player_uuid = ? AND nation_name = ? ORDER BY issued_at DESC";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, nationName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                loans.add(new Loan(
                    rs.getInt("id"),
                    rs.getString("player_uuid"),
                    rs.getString("nation_name"),
                    rs.getDouble("amount"),
                    rs.getDouble("interest_rate"),
                    rs.getLong("issued_at"),
                    rs.getLong("due_at"),
                    rs.getDouble("repaid_amount"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            logger.severe("获取玩家贷款失败: " + e.getMessage());
        }

        return loans;
    }

    /**
     * 计算应还总额（本金 + 利息）
     */
    public double calculateTotalDue(Loan loan) {
        long durationMs = loan.dueAt - loan.issuedAt;
        double durationYears = durationMs / (365.0 * 24 * 60 * 60 * 1000);
        double interest = loan.amount * loan.interestRate * durationYears;
        return loan.amount + interest;
    }

    /**
     * 检查逾期贷款并触发挤兑
     */
    public void checkOverdueLoans() {
        String sql = "SELECT * FROM loans WHERE status = 'active' AND due_at < ?";
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, now);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nationName = rs.getString("nation_name");
                logger.warning("检测到逾期贷款！国家: " + nationName);
                // TODO: 触发挤兑事件
            }
        } catch (SQLException e) {
            logger.severe("检查逾期贷款失败: " + e.getMessage());
        }
    }

    /**
     * 获取玩家的所有贷款
     */
    public List<Loan> getPlayerLoans(UUID playerUuid) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE player_uuid = ? ORDER BY issued_at DESC";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                loans.add(new Loan(
                    rs.getInt("id"),
                    rs.getString("player_uuid"),
                    rs.getString("nation_name"),
                    rs.getDouble("amount"),
                    rs.getDouble("interest_rate"),
                    rs.getLong("issued_at"),
                    rs.getLong("due_at"),
                    rs.getDouble("repaid_amount"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            logger.severe("查询玩家贷款失败: " + e.getMessage());
        }

        return loans;
    }

    /**
     * 贷款数据类
     */
    public static class Loan {
        public final int id;
        public final String playerUuid;
        public final String nationName;
        public final double amount;
        public final double interestRate;
        public final long issuedAt;
        public final long dueAt;
        public final double repaidAmount;
        public final String status;

        public Loan(int id, String playerUuid, String nationName, double amount,
                    double interestRate, long issuedAt, long dueAt, double repaidAmount, String status) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.nationName = nationName;
            this.amount = amount;
            this.interestRate = interestRate;
            this.issuedAt = issuedAt;
            this.dueAt = dueAt;
            this.repaidAmount = repaidAmount;
            this.status = status;
        }
    }
}
