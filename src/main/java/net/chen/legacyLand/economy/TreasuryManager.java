package net.chen.legacyLand.economy;

import lombok.Getter;
import lombok.Setter;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 国库管理器
 * 管理各国的国库位置、储备金、货币发行
 */
public class TreasuryManager {
    @Getter
    private static TreasuryManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final EconomyDatabase database;
    private final Map<String, Treasury> treasuries = new HashMap<>();

    private TreasuryManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.database = EconomyDatabase.getInstance();
    }

    public static TreasuryManager getInstance(LegacyLand plugin) {
        if (instance == null) {
            instance = new TreasuryManager(plugin);
        }
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        loadAllTreasuries();
        logger.info("国库系统已加载，共 " + treasuries.size() + " 个国库");
    }

    /**
     * 加载所有国库
     */
    private void loadAllTreasuries() {
        String sql = "SELECT * FROM treasuries";
        try (Statement stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String nationName = rs.getString("nation_name");
                Location location = new Location(
                    plugin.getServer().getWorld(rs.getString("world")),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z")
                );
                double sbcReserve = rs.getDouble("sbc_reserve");
                double currencyIssued = rs.getDouble("currency_issued");
                double creditScore = rs.getDouble("credit_score");
                long createdAt = rs.getLong("created_at");
                long lastUpdated = rs.getLong("last_updated");

                Treasury treasury = new Treasury(nationName, location, sbcReserve,
                    currencyIssued, creditScore, createdAt, lastUpdated);
                treasuries.put(nationName, treasury);
            }
        } catch (SQLException e) {
            logger.severe("加载国库数据失败: " + e.getMessage());
        }
    }

    /**
     * 创建国库
     */
    public boolean createTreasury(String nationName, Location location) {
        if (treasuries.containsKey(nationName)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Treasury treasury = new Treasury(nationName, location, 0, 0, 1.0, now, now);

        String sql = """
            INSERT INTO treasuries (nation_name, world, x, y, z, sbc_reserve,
                currency_issued, credit_score, created_at, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ps.setString(2, location.getWorld().getName());
            ps.setInt(3, location.getBlockX());
            ps.setInt(4, location.getBlockY());
            ps.setInt(5, location.getBlockZ());
            ps.setDouble(6, 0);
            ps.setDouble(7, 0);
            ps.setDouble(8, 1.0);
            ps.setLong(9, now);
            ps.setLong(10, now);
            ps.executeUpdate();

            treasuries.put(nationName, treasury);
            logger.info("国库已创建: " + nationName);
            return true;
        } catch (SQLException e) {
            logger.severe("创建国库失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取国库
     */
    public Treasury getTreasury(String nationName) {
        return treasuries.get(nationName);
    }

    /**
     * 获取所有国库
     */
    public Map<String, Treasury> getAllTreasuries() {
        return new HashMap<>(treasuries);
    }

    /**
     * 增加储备金（SBC/金锭）
     */
    public boolean addReserve(String nationName, double amount) {
        Treasury treasury = treasuries.get(nationName);
        if (treasury == null) return false;

        treasury.setSbcReserve(treasury.getSbcReserve() + amount);
        updateTreasury(treasury);
        return true;
    }

    /**
     * 减少储备金
     */
    public boolean removeReserve(String nationName, double amount) {
        Treasury treasury = treasuries.get(nationName);
        if (treasury == null) return false;
        if (treasury.getSbcReserve() < amount) return false;

        treasury.setSbcReserve(treasury.getSbcReserve() - amount);
        updateTreasury(treasury);
        return true;
    }

    /**
     * 发行货币
     * @param nationName 国家名称
     * @param denomination 面值
     * @param issuedBy 发行者 UUID
     * @return 货币物品
     */
    public ItemStack issueCurrency(String nationName, double denomination, String issuedBy) {
        Treasury treasury = treasuries.get(nationName);
        if (treasury == null) return null;

        // 生成序列号
        String serialNumber = generateSerialNumber(nationName);

        // 记录到数据库
        String sql = """
            INSERT INTO currencies (serial_number, nation_name, denomination, issued_at, issued_by, status)
            VALUES (?, ?, ?, ?, ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            ps.setString(2, nationName);
            ps.setDouble(3, denomination);
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, issuedBy);
            ps.executeUpdate();

            // 更新已发行货币总量
            treasury.setCurrencyIssued(treasury.getCurrencyIssued() + denomination);
            updateTreasury(treasury);

            // 创建货币物品
            return CurrencyItem.createCurrency(nationName, denomination, serialNumber);
        } catch (SQLException e) {
            logger.severe("发行货币失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成序列号
     */
    private String generateSerialNumber(String nationName) {
        String prefix = nationName.substring(0, Math.min(3, nationName.length())).toUpperCase();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + uuid;
    }

    /**
     * 验证货币真伪
     */
    public boolean verifyCurrency(ItemStack item) {
        if (!CurrencyItem.isCurrency(item)) return false;

        String serialNumber = CurrencyItem.getSerialNumber(item);
        if (serialNumber == null) return false;

        String sql = "SELECT status FROM currencies WHERE serial_number = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                return "active".equals(status);
            }
            return false;
        } catch (SQLException e) {
            logger.severe("验证货币失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 作废货币
     */
    public boolean revokeCurrency(String serialNumber) {
        String sql = "UPDATE currencies SET status = 'revoked' WHERE serial_number = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.severe("作废货币失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 计算汇率（相对于 SBC/金锭）
     * 公式: ER = (SBC储备 / 流通货币) × 信用系数
     */
    public double calculateExchangeRate(String nationName) {
        Treasury treasury = treasuries.get(nationName);
        if (treasury == null) return 0;

        double sbcReserve = treasury.getSbcReserve();
        double currencyIssued = treasury.getCurrencyIssued();
        double creditScore = treasury.getCreditScore();

        if (currencyIssued == 0) return 0;

        double rate = (sbcReserve / currencyIssued) * creditScore;

        // 记录汇率历史
        recordExchangeRate(nationName, rate);

        return rate;
    }

    /**
     * 记录汇率历史
     */
    private void recordExchangeRate(String nationName, double rate) {
        String sql = "INSERT INTO exchange_rates (nation_name, rate_to_sbc, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ps.setDouble(2, rate);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("记录汇率失败: " + e.getMessage());
        }
    }

    /**
     * 更新国库数据
     */
    private void updateTreasury(Treasury treasury) {
        String sql = """
            UPDATE treasuries SET sbc_reserve = ?, currency_issued = ?,
                credit_score = ?, last_updated = ? WHERE nation_name = ?
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, treasury.getSbcReserve());
            ps.setDouble(2, treasury.getCurrencyIssued());
            ps.setDouble(3, treasury.getCreditScore());
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, treasury.getNationName());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("更新国库失败: " + e.getMessage());
        }
    }

    /**
     * 国库数据类
     */
    @Getter
    public static class Treasury {
        private final String nationName;
        private final Location location;
        @Setter
        private double sbcReserve;
        @Setter
        private double currencyIssued;
        @Setter
        private double creditScore;
        private final long createdAt;
        private final long lastUpdated;

        public Treasury(String nationName, Location location, double sbcReserve,
                       double currencyIssued, double creditScore, long createdAt, long lastUpdated) {
            this.nationName = nationName;
            this.location = location;
            this.sbcReserve = sbcReserve;
            this.currencyIssued = currencyIssued;
            this.creditScore = creditScore;
            this.createdAt = createdAt;
            this.lastUpdated = lastUpdated;
        }

        public double calculateExchangeRate() {
            if (currencyIssued <= 0) {
                return 0;
            }
            return (sbcReserve / currencyIssued) * creditScore;
        }
    }
}
