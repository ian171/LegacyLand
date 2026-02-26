package net.chen.legacyLand.market;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 市场管理器 - 处理市场箱子的注册、定价和购买逻辑
 */
public class MarketManager {

    private static MarketManager instance;

    private final LegacyLand plugin;
    private final Logger logger;
    private Connection connection;

    // key = "world,chunkX,chunkZ"
    private final Map<String, Market> marketsByChunk = new ConcurrentHashMap<>();
    private final Map<String, Market> marketsById = new ConcurrentHashMap<>();
    // locationKey -> MarketChest (全局索引，快速查找)
    private final Map<String, MarketChest> chestIndex = new ConcurrentHashMap<>();
    // 玩家等待设置价格的状态 (playerId -> chestLocationKey)
    private final Map<UUID, String> pendingPriceSet = new ConcurrentHashMap<>();

    private MarketManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static MarketManager getInstance(LegacyLand plugin) {
        if (instance == null) {
            instance = new MarketManager(plugin);
        }
        return instance;
    }

    public static MarketManager getInstance() {
        return instance;
    }

    // ===================== 初始化 =====================

    public void init(Connection conn) {
        this.connection = conn;
        createTables();
        loadAll();
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS markets (
                    id TEXT PRIMARY KEY,
                    nation_name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    approved_by TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    UNIQUE(world, chunk_x, chunk_z)
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS market_chests (
                    id TEXT PRIMARY KEY,
                    market_id TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    owner_uuid TEXT NOT NULL,
                    price_per_item REAL NOT NULL DEFAULT 0,
                    price_set INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                )""");
        } catch (SQLException e) {
            logger.severe("[Market] 创建表失败: " + e.getMessage());
        }
    }

    // ===================== 加载 =====================

    private void loadAll() {
        loadMarkets();
        loadChests();
        logger.info("[Market] 加载完成: " + marketsById.size() + " 个市场, " + chestIndex.size() + " 个销售箱");
    }

    private void loadMarkets() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM markets")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String nationName = rs.getString("nation_name");
                String world = rs.getString("world");
                int cx = rs.getInt("chunk_x"), cz = rs.getInt("chunk_z");
                UUID approvedBy = UUID.fromString(rs.getString("approved_by"));

                Market market = new Market(id, nationName, world, cx, cz, approvedBy);
                marketsById.put(id, market);
                marketsByChunk.put(chunkKey(world, cx, cz), market);
            }
        } catch (SQLException e) {
            logger.severe("[Market] 加载市场失败: " + e.getMessage());
        }
    }

    private void loadChests() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM market_chests")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String marketId = rs.getString("market_id");
                String world = rs.getString("world");
                int x = rs.getInt("x"), y = rs.getInt("y"), z = rs.getInt("z");
                UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                double price = rs.getDouble("price_per_item");
                boolean priceSet = rs.getInt("price_set") == 1;
                long createdAt = rs.getLong("created_at");

                var w = Bukkit.getWorld(world);
                if (w == null) continue;
                Location loc = new Location(w, x, y, z);

                MarketChest chest = new MarketChest(id, marketId, loc, ownerUuid);
                chest.setPricePerItem(price);
                chest.setPriceSet(priceSet);

                Market market = marketsById.get(marketId);
                if (market != null) market.addChest(chest);
                chestIndex.put(chest.getLocationKey(), chest);
            }
        } catch (SQLException e) {
            logger.severe("[Market] 加载销售箱失败: " + e.getMessage());
        }
    }

    // ===================== 市场管理 =====================

    /**
     * 在当前地块创建市场（需要城镇权限）
     */
    public MarketCreateResult createMarket(Player player) {
        Location loc = player.getLocation();
        TownBlock tb = TownyAPI.getInstance().getTownBlock(loc);
        if (tb == null) return MarketCreateResult.NOT_IN_TOWN;

        // 必须有该城镇的管理权限
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return MarketCreateResult.NO_PERMISSION;
        try {
            if (!tb.getTown().isMayor(resident) && !resident.isMayor()) {
                return MarketCreateResult.NO_PERMISSION;
            }
        } catch (Exception e) {
            return MarketCreateResult.NO_PERMISSION;
        }

        // 获取所属国家
        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null) return MarketCreateResult.NO_NATION;

        int cx = loc.getChunk().getX(), cz = loc.getChunk().getZ();
        String key = chunkKey(loc.getWorld().getName(), cx, cz);

        if (marketsByChunk.containsKey(key)) return MarketCreateResult.ALREADY_MARKET;

        String id = UUID.randomUUID().toString();
        Market market = new Market(id, nation.getName(), loc.getWorld().getName(), cx, cz, player.getUniqueId());
        marketsById.put(id, market);
        marketsByChunk.put(key, market);
        saveMarket(market);

        return MarketCreateResult.SUCCESS;
    }

    /**
     * 删除当前地块的市场
     */
    public MarketDeleteResult deleteMarket(Player player) {
        Location loc = player.getLocation();
        int cx = loc.getChunk().getX(), cz = loc.getChunk().getZ();
        String key = chunkKey(loc.getWorld().getName(), cx, cz);
        Market market = marketsByChunk.get(key);
        if (market == null) return MarketDeleteResult.NOT_MARKET;

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return MarketDeleteResult.NO_PERMISSION;
        // Leader 或市场创建者可删除
        if (!market.getApprovedBy().equals(player.getUniqueId()) && !resident.isMayor()) {
            return MarketDeleteResult.NO_PERMISSION;
        }

        // 从内存移除
        for (MarketChest chest : market.getAllChests()) {
            chestIndex.remove(chest.getLocationKey());
        }
        marketsById.remove(market.getId());
        marketsByChunk.remove(key);
        deleteMarketFromDb(market.getId());

        return MarketDeleteResult.SUCCESS;
    }

    // ===================== 销售箱管理 =====================

    /**
     * 注册销售箱（玩家在市场地块放置箱子时自动调用）
     */
    public boolean registerChest(Player player, Location loc) {
        Market market = getMarketAt(loc);
        if (market == null) return false;

        String locKey = Market.toLocationKey(loc);
        if (chestIndex.containsKey(locKey)) return false;

        String id = UUID.randomUUID().toString();
        MarketChest chest = new MarketChest(id, market.getId(), loc, player.getUniqueId());
        market.addChest(chest);
        chestIndex.put(locKey, chest);
        saveChest(chest);
        return true;
    }

    /**
     * 注销销售箱（玩家破坏箱子时调用）
     */
    public boolean unregisterChest(Location loc) {
        String locKey = Market.toLocationKey(loc);
        MarketChest chest = chestIndex.remove(locKey);
        if (chest == null) return false;

        Market market = marketsById.get(chest.getMarketId());
        if (market != null) market.removeChest(locKey);

        deleteChestFromDb(chest.getId());
        return true;
    }

    /**
     * 开始设置价格流程：记录玩家等待状态
     */
    public void startPriceSet(Player player) {
        pendingPriceSet.put(player.getUniqueId(), null);
        player.sendMessage("§e请手持要定价的物品，然后右键点击你的销售箱来设置价格。");
        player.sendMessage("§7使用 §f/price set <金额> §7来确认价格。");
    }

    /**
     * 玩家点击箱子时记录目标箱子
     */
    public void setPendingChest(UUID playerId, String locationKey) {
        pendingPriceSet.put(playerId, locationKey);
    }

    public boolean hasPendingPriceSet(UUID playerId) {
        return pendingPriceSet.containsKey(playerId);
    }

    public String getPendingChestKey(UUID playerId) {
        return pendingPriceSet.get(playerId);
    }

    public void clearPendingPriceSet(UUID playerId) {
        pendingPriceSet.remove(playerId);
    }

    /**
     * 执行定价：将玩家手持物品与箱子绑定并设置价格
     */
    public PriceSetResult setPrice(Player player, double price) {
        if (!pendingPriceSet.containsKey(player.getUniqueId())) {
            return PriceSetResult.NOT_PENDING;
        }
        String chestKey = pendingPriceSet.get(player.getUniqueId());
        if (chestKey == null) return PriceSetResult.NO_CHEST_SELECTED;

        MarketChest chest = chestIndex.get(chestKey);
        if (chest == null) return PriceSetResult.CHEST_NOT_FOUND;
        if (!chest.getOwnerUuid().equals(player.getUniqueId())) return PriceSetResult.NOT_OWNER;
        if (price <= 0) return PriceSetResult.INVALID_PRICE;

        chest.setPricePerItem(price);
        chest.setPriceSet(true);
        updateChestPrice(chest);
        clearPendingPriceSet(player.getUniqueId());
        return PriceSetResult.SUCCESS;
    }

    /**
     * 玩家购买箱子内物品（买家点击销售箱）
     */
    public PurchaseResult purchaseFromChest(Player buyer, Location chestLoc) {
        String locKey = Market.toLocationKey(chestLoc);
        MarketChest chest = chestIndex.get(locKey);
        if (chest == null) return PurchaseResult.NOT_SALE_CHEST;
        if (!chest.isActive()) return PurchaseResult.NOT_ACTIVE;
        if (chest.getOwnerUuid().equals(buyer.getUniqueId())) return PurchaseResult.OWN_CHEST;

        // 检查箱子实体内是否有物品
        if (!(chestLoc.getBlock().getState() instanceof org.bukkit.block.Chest chestBlock)) {
            return PurchaseResult.CHEST_EMPTY;
        }
        ItemStack[] contents = chestBlock.getInventory().getContents();
        ItemStack toSell = null;
        int slotIndex = -1;
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != org.bukkit.Material.AIR) {
                toSell = contents[i];
                slotIndex = i;
                break;
            }
        }
        if (toSell == null || slotIndex == -1) return PurchaseResult.CHEST_EMPTY;

        double price = chest.getPricePerItem();

        // 检查买家余额
        if (LegacyLand.getEcon() == null) return PurchaseResult.ECON_ERROR;
        if (LegacyLand.getEcon().getBalance(buyer) < price) return PurchaseResult.INSUFFICIENT_FUNDS;

        // 检查买家背包空间
        if (buyer.getInventory().firstEmpty() == -1) return PurchaseResult.INVENTORY_FULL;

        // 扣钱给卖家
        LegacyLand.getEcon().withdrawPlayer(buyer, price);
        Player seller = Bukkit.getPlayer(chest.getOwnerUuid());
        if (seller != null && seller.isOnline()) {
            LegacyLand.getEcon().depositPlayer(seller, price);
            seller.sendMessage("§a[市场] §f" + buyer.getName() + " §7购买了你的物品，获得 §6" + price + " §7金币。");
        } else {
            // 卖家离线，仍然转账（Vault 支持离线）
            LegacyLand.getEcon().depositPlayer(Bukkit.getOfflinePlayer(chest.getOwnerUuid()), price);
        }

        // 给买家物品（取一个）
        ItemStack purchased = toSell.clone();
        purchased.setAmount(1);
        buyer.getInventory().addItem(purchased);

        // 从箱子中减少
        if (toSell.getAmount() > 1) {
            toSell.setAmount(toSell.getAmount() - 1);
            chestBlock.getInventory().setItem(slotIndex, toSell);
        } else {
            chestBlock.getInventory().setItem(slotIndex, null);
        }

        return PurchaseResult.SUCCESS;
    }

    // ===================== 查询 =====================

    public Market getMarketAt(Location loc) {
        String key = chunkKey(loc.getWorld().getName(), loc.getChunk().getX(), loc.getChunk().getZ());
        return marketsByChunk.get(key);
    }

    public MarketChest getChestAt(Location loc) {
        return chestIndex.get(Market.toLocationKey(loc));
    }

    public boolean isMarketChest(Location loc) {
        return chestIndex.containsKey(Market.toLocationKey(loc));
    }

    // ===================== 数据库持久化 =====================

    private void saveMarket(Market market) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO markets (id, nation_name, world, chunk_x, chunk_z, approved_by, created_at) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, market.getId());
            ps.setString(2, market.getNationName());
            ps.setString(3, market.getWorldName());
            ps.setInt(4, market.getChunkX());
            ps.setInt(5, market.getChunkZ());
            ps.setString(6, market.getApprovedBy().toString());
            ps.setLong(7, market.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Market] 保存市场失败: " + e.getMessage());
        }
    }

    private void deleteMarketFromDb(String marketId) {
        try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM markets WHERE id=?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM market_chests WHERE market_id=?")) {
            ps1.setString(1, marketId); ps1.executeUpdate();
            ps2.setString(1, marketId); ps2.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Market] 删除市场失败: " + e.getMessage());
        }
    }

    private void saveChest(MarketChest chest) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO market_chests (id, market_id, world, x, y, z, owner_uuid, price_per_item, price_set, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, chest.getId());
            ps.setString(2, chest.getMarketId());
            ps.setString(3, chest.getWorldName());
            ps.setInt(4, chest.getX());
            ps.setInt(5, chest.getY());
            ps.setInt(6, chest.getZ());
            ps.setString(7, chest.getOwnerUuid().toString());
            ps.setDouble(8, chest.getPricePerItem());
            ps.setInt(9, chest.isPriceSet() ? 1 : 0);
            ps.setLong(10, chest.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Market] 保存销售箱失败: " + e.getMessage());
        }
    }

    private void updateChestPrice(MarketChest chest) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE market_chests SET price_per_item=?, price_set=1 WHERE id=?")) {
            ps.setDouble(1, chest.getPricePerItem());
            ps.setString(2, chest.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Market] 更新价格失败: " + e.getMessage());
        }
    }

    private void deleteChestFromDb(String chestId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM market_chests WHERE id=?")) {
            ps.setString(1, chestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Market] 删除销售箱失败: " + e.getMessage());
        }
    }

    private String chunkKey(String world, int cx, int cz) {
        return world + "," + cx + "," + cz;
    }

    // ===================== 结果枚举 =====================

    public enum MarketCreateResult { SUCCESS, NOT_IN_TOWN, NO_PERMISSION, NO_NATION, ALREADY_MARKET }
    public enum MarketDeleteResult { SUCCESS, NOT_MARKET, NO_PERMISSION }
    public enum PriceSetResult { SUCCESS, NOT_PENDING, NO_CHEST_SELECTED, CHEST_NOT_FOUND, NOT_OWNER, INVALID_PRICE }
    public enum PurchaseResult {
        SUCCESS, NOT_SALE_CHEST, NOT_ACTIVE, OWN_CHEST, CHEST_EMPTY,
        INSUFFICIENT_FUNDS, INVENTORY_FULL, ECON_ERROR
    }
}
