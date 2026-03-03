package net.chen.legacyLand.economy;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 期货管理器
 * 实现资源期货交易、远期合约
 */
public class FuturesManager {
    private static FuturesManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final EconomyDatabase database;
    private final BankManager bankManager;

    private FuturesManager(LegacyLand plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.database = EconomyDatabase.getInstance();
        this.bankManager = bankManager;
    }

    public static FuturesManager getInstance(LegacyLand plugin, BankManager bankManager) {
        if (instance == null) {
            instance = new FuturesManager(plugin, bankManager);
        }
        return instance;
    }

    public static FuturesManager getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        createFuturesTable();
        logger.info("期货系统已加载");
    }

    /**
     * 创建期货表
     */
    private void createFuturesTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS futures (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                seller_uuid TEXT NOT NULL,
                buyer_uuid TEXT,
                nation_name TEXT NOT NULL,
                material TEXT NOT NULL,
                amount INTEGER NOT NULL,
                price REAL NOT NULL,
                delivery_date INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'open',
                FOREIGN KEY (nation_name) REFERENCES treasuries(nation_name)
            )
        """;

        try (Statement stmt = database.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("期货表已创建");
        } catch (SQLException e) {
            logger.severe("创建期货表失败: " + e.getMessage());
        }
    }

    /**
     * 创建期货合约
     * @param seller 卖方
     * @param nationName 国家名称
     * @param material 物品类型
     * @param amount 数量
     * @param price 价格
     * @param deliveryDays 交割天数
     * @return 合约 ID，失败返回 -1
     */
    public int createFuture(Player seller, String nationName, Material material, int amount, double price, int deliveryDays) {
        long now = System.currentTimeMillis();
        long deliveryDate = now + (deliveryDays * 24L * 60 * 60 * 1000);

        String sql = """
            INSERT INTO futures (seller_uuid, nation_name, material, amount, price, delivery_date, created_at, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'open')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.getUniqueId().toString());
            ps.setString(2, nationName);
            ps.setString(3, material.name());
            ps.setInt(4, amount);
            ps.setDouble(5, price);
            ps.setLong(6, deliveryDate);
            ps.setLong(7, now);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.severe("创建期货合约失败: " + e.getMessage());
        }

        return -1;
    }

    /**
     * 购买期货合约
     * @param buyer 买方
     * @param futureId 合约 ID
     * @return 是否成功
     */
    public boolean buyFuture(Player buyer, int futureId) {
        Future future = getFuture(futureId);
        if (future == null || !future.status.equals("open")) {
            return false;
        }

        // 检查余额
        double balance = bankManager.getBalance(buyer.getUniqueId(), future.nationName);
        if (balance < future.price) {
            return false;
        }

        // 转账给卖方
        UUID sellerUuid = UUID.fromString(future.sellerUuid);
        boolean success = bankManager.transfer(buyer, sellerUuid, future.nationName, future.price);
        if (!success) {
            return false;
        }

        // 更新合约状态
        String sql = """
            UPDATE futures
            SET buyer_uuid = ?, status = 'sold'
            WHERE id = ?
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, buyer.getUniqueId().toString());
            ps.setInt(2, futureId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.severe("购买期货失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 交割期货
     * @param player 玩家（卖方）
     * @param futureId 合约 ID
     * @return 是否成功
     */
    public boolean deliverFuture(Player player, int futureId) {
        Future future = getFuture(futureId);
        if (future == null || !future.status.equals("sold")) {
            return false;
        }

        if (!future.sellerUuid.equals(player.getUniqueId().toString())) {
            return false;
        }

        // 检查是否到交割日期
        if (System.currentTimeMillis() < future.deliveryDate) {
            return false;
        }

        // 检查卖方是否有足够的物品
        Material material = Material.valueOf(future.material);
        if (!player.getInventory().contains(material, future.amount)) {
            return false;
        }

        // 扣除物品
        player.getInventory().removeItem(new ItemStack(material, future.amount));

        // 更新合约状态
        String sql = """
            UPDATE futures
            SET status = 'delivered'
            WHERE id = ?
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, futureId);
            ps.executeUpdate();

            // 通知买方领取物品
            Player buyer = plugin.getServer().getPlayer(UUID.fromString(future.buyerUuid));
            if (buyer != null && buyer.isOnline()) {
                buyer.sendMessage("§a期货合约 #" + futureId + " 已交割！请使用 /futures claim " + futureId + " 领取物品");
            }

            return true;
        } catch (SQLException e) {
            logger.severe("交割期货失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 领取期货物品
     * @param player 玩家（买方）
     * @param futureId 合约 ID
     * @return 是否成功
     */
    public boolean claimFuture(Player player, int futureId) {
        Future future = getFuture(futureId);
        if (future == null || !future.status.equals("delivered")) {
            return false;
        }

        if (!future.buyerUuid.equals(player.getUniqueId().toString())) {
            return false;
        }

        // 给予物品
        Material material = Material.valueOf(future.material);
        player.getInventory().addItem(new ItemStack(material, future.amount));

        // 更新合约状态
        String sql = """
            UPDATE futures
            SET status = 'completed'
            WHERE id = ?
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, futureId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.severe("领取期货失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 取消期货合约（仅限未售出的）
     */
    public boolean cancelFuture(Player player, int futureId) {
        Future future = getFuture(futureId);
        if (future == null || !future.status.equals("open")) {
            return false;
        }

        if (!future.sellerUuid.equals(player.getUniqueId().toString())) {
            return false;
        }

        String sql = "UPDATE futures SET status = 'cancelled' WHERE id = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, futureId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.severe("取消期货失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取期货合约
     */
    public Future getFuture(int futureId) {
        String sql = "SELECT * FROM futures WHERE id = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, futureId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Future(
                    rs.getInt("id"),
                    rs.getString("seller_uuid"),
                    rs.getString("buyer_uuid"),
                    rs.getString("nation_name"),
                    rs.getString("material"),
                    rs.getInt("amount"),
                    rs.getDouble("price"),
                    rs.getLong("delivery_date"),
                    rs.getLong("created_at"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            logger.severe("获取期货失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取所有开放的期货合约
     */
    public List<Future> getOpenFutures(String nationName) {
        List<Future> futures = new ArrayList<>();
        String sql = "SELECT * FROM futures WHERE nation_name = ? AND status = 'open' ORDER BY created_at DESC";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                futures.add(new Future(
                    rs.getInt("id"),
                    rs.getString("seller_uuid"),
                    rs.getString("buyer_uuid"),
                    rs.getString("nation_name"),
                    rs.getString("material"),
                    rs.getInt("amount"),
                    rs.getDouble("price"),
                    rs.getLong("delivery_date"),
                    rs.getLong("created_at"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            logger.severe("获取期货列表失败: " + e.getMessage());
        }

        return futures;
    }

    /**
     * 获取玩家的期货合约
     */
    public List<Future> getPlayerFutures(UUID playerUuid) {
        List<Future> futures = new ArrayList<>();
        String sql = "SELECT * FROM futures WHERE seller_uuid = ? OR buyer_uuid = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            String uuidStr = playerUuid.toString();
            ps.setString(1, uuidStr);
            ps.setString(2, uuidStr);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                futures.add(new Future(
                    rs.getInt("id"),
                    rs.getString("seller_uuid"),
                    rs.getString("buyer_uuid"),
                    rs.getString("nation_name"),
                    rs.getString("material"),
                    rs.getInt("amount"),
                    rs.getDouble("price"),
                    rs.getLong("delivery_date"),
                    rs.getLong("created_at"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            logger.severe("获取玩家期货失败: " + e.getMessage());
        }

        return futures;
    }

    /**
     * 获取所有开放的期货合约
     */
    public List<Future> getOpenFutures() {
        List<Future> futures = new ArrayList<>();
        String sql = "SELECT * FROM futures WHERE status = 'open' ORDER BY created_at DESC";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                futures.add(new Future(
                    rs.getInt("id"),
                    rs.getString("seller_uuid"),
                    rs.getString("buyer_uuid"),
                    rs.getString("nation_name"),
                    rs.getString("material"),
                    rs.getInt("amount"),
                    rs.getDouble("price"),
                    rs.getLong("delivery_date"),
                    rs.getLong("created_at"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            logger.severe("查询开放期货失败: " + e.getMessage());
        }

        return futures;
    }

    /**
     * 期货合约数据类
     */
    public static class Future {
        public final int id;
        public final String sellerUuid;
        public final String buyerUuid;
        public final String nationName;
        public final String material;
        public final int amount;
        public final double price;
        public final long deliveryDate;
        public final long createdAt;
        public final String status;

        public Future(int id, String sellerUuid, String buyerUuid, String nationName,
                     String material, int amount, double price, long deliveryDate,
                     long createdAt, String status) {
            this.id = id;
            this.sellerUuid = sellerUuid;
            this.buyerUuid = buyerUuid;
            this.nationName = nationName;
            this.material = material;
            this.amount = amount;
            this.price = price;
            this.deliveryDate = deliveryDate;
            this.createdAt = createdAt;
            this.status = status;
        }
    }
}
