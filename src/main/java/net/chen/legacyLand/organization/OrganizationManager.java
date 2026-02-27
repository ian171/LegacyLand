package net.chen.legacyLand.organization;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationPermission;
import net.chen.legacyLand.organization.outpost.Outpost;
import net.chen.legacyLand.organization.outpost.OutpostGoods;
import net.chen.legacyLand.organization.outpost.OutpostStatus;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 组织管理器 - 处理组织和据点的所有逻辑与持久化
 */
public class OrganizationManager {

    @Getter
    private static OrganizationManager instance;

    private final LegacyLand plugin;
    private final Logger logger;
    private Connection connection;

    // 内存缓存
    private final Map<String, Organization> organizationsByName = new ConcurrentHashMap<>();
    private final Map<String, Organization> organizationsById = new ConcurrentHashMap<>();
    // playerId -> orgId
    private final Map<UUID, String> playerOrgMap = new ConcurrentHashMap<>();
    // outpostId -> Outpost
    private final Map<String, Outpost> outpostsById = new ConcurrentHashMap<>();
    // orgId -> List<outpostId>
    private final Map<String, List<String>> orgOutpostMap = new ConcurrentHashMap<>();

    private OrganizationManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static OrganizationManager getInstance(LegacyLand plugin) {
        if (instance == null) {
            instance = new OrganizationManager(plugin);
        }
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
                CREATE TABLE IF NOT EXISTS organizations (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    leader_uuid TEXT NOT NULL,
                    nation_name TEXT,
                    created_at INTEGER NOT NULL
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS organization_members (
                    org_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    role TEXT NOT NULL,
                    permissions TEXT NOT NULL DEFAULT '',
                    joined_at INTEGER NOT NULL,
                    PRIMARY KEY (org_id, player_uuid)
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS outposts (
                    id TEXT PRIMARY KEY,
                    org_id TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    radius INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OPEN',
                    created_at INTEGER NOT NULL
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS outpost_goods (
                    id TEXT PRIMARY KEY,
                    outpost_id TEXT NOT NULL,
                    item_data TEXT NOT NULL,
                    price REAL NOT NULL,
                    quantity INTEGER NOT NULL,
                    added_at INTEGER NOT NULL
                )""");
        } catch (SQLException e) {
            logger.severe("[Organization] 创建表失败: " + e.getMessage());
        }
    }

    // ===================== 加载 =====================

    private void loadAll() {
        loadOrganizations();
        loadMembers();
        loadOutposts();
        loadOutpostGoods();
        logger.info("[Organization] 加载完成");
        if (LegacyLand.getInstance().isDev) {
            logger.info(organizationsById.size() + " 个组织, " + outpostsById.size() + " 个据点");
        }
    }

    private void loadOrganizations() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM organizations")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                UUID leaderId = UUID.fromString(rs.getString("leader_uuid"));
                String nationName = rs.getString("nation_name");
                Organization org = new Organization(id, name, leaderId, nationName);
                organizationsById.put(id, org);
                organizationsByName.put(name.toLowerCase(), org);
            }
        } catch (SQLException e) {
            logger.severe("[Organization] 加载组织失败: " + e.getMessage());
        }
    }

    private void loadMembers() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM organization_members")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String orgId = rs.getString("org_id");
                UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                OrganizationRole role = OrganizationRole.valueOf(rs.getString("role"));
                String permStr = rs.getString("permissions");

                Organization org = organizationsById.get(orgId);
                if (org == null) continue;

                // Leader 已在构造时添加，跳过
                if (org.isLeader(playerId)) {
                    playerOrgMap.put(playerId, orgId);
                    continue;
                }

                org.addMember(playerId, role);
                OrganizationMember member = org.getMember(playerId);

                // 恢复自定义权限
                if (permStr != null && !permStr.isBlank()) {
                    for (String p : permStr.split(",")) {
                        try {
                            member.addPermission(OrganizationPermission.valueOf(p.trim()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                playerOrgMap.put(playerId, orgId);
            }
        } catch (SQLException e) {
            logger.severe("[Organization] 加载成员失败: " + e.getMessage());
        }
    }

    private void loadOutposts() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM outposts")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String orgId = rs.getString("org_id");
                String world = rs.getString("world");
                double x = rs.getDouble("x"), y = rs.getDouble("y"), z = rs.getDouble("z");
                int radius = rs.getInt("radius");
                OutpostStatus status = OutpostStatus.valueOf(rs.getString("status"));

                var w = plugin.getServer().getWorld(world);
                if (w == null) {
                    logger.warning("[Organization] 据点 " + id + " 所在世界不存在: " + world);
                    continue;
                }
                Location loc = new Location(w, x, y, z);
                Outpost outpost = new Outpost(id, orgId, loc, radius);
                outpost.setStatus(status);

                outpostsById.put(id, outpost);
                orgOutpostMap.computeIfAbsent(orgId, k -> new ArrayList<>()).add(id);
            }
        } catch (SQLException e) {
            logger.severe("[Organization] 加载据点失败: " + e.getMessage());
        }
    }

    private void loadOutpostGoods() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM outpost_goods")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String outpostId = rs.getString("outpost_id");
                String itemData = rs.getString("item_data");
                double price = rs.getDouble("price");
                int quantity = rs.getInt("quantity");
                long addedAt = rs.getLong("added_at");

                Outpost outpost = outpostsById.get(outpostId);
                if (outpost == null) continue;

                var item = deserializeItem(itemData);
                if (item == null) continue;

                OutpostGoods goods = new OutpostGoods(id, outpostId, item, price, quantity);
                outpost.addGoods(goods);
            }
        } catch (SQLException e) {
            logger.severe("[Organization] 加载据点货物失败: " + e.getMessage());
        }
    }

    // ===================== 组织 CRUD =====================

    /**
     * 创建组织
     */
    public OrganizationCreateResult createOrganization(Player player, String name) {
        // 检查名称是否已存在
        if (organizationsByName.containsKey(name.toLowerCase())) {
            return OrganizationCreateResult.NAME_EXISTS;
        }
        // 检查玩家是否已在组织中
        if (playerOrgMap.containsKey(player.getUniqueId())) {
            return OrganizationCreateResult.ALREADY_IN_ORG;
        }

        // 判断类型（国家 or 非国家）
        Resident resident = TownyAPI.getInstance().getResident(player);
        Nation nation = resident != null ? TownyAPI.getInstance().getResidentNationOrNull(resident) : null;
        String nationName = nation != null ? nation.getName() : null;

        // 国家组织需要 MANAGE_ORGANIZATION 权限
        if (nationName != null && !NationManager.getInstance().hasPermission(player, NationPermission.MANAGE_ORGANIZATION)) {
            return OrganizationCreateResult.NO_NATION_PERMISSION;
        }

        // 扣费
        double cost = plugin.getConfig().getDouble("organization.create-cost", 500.0);
        if (LegacyLand.getEcon() != null) {
            if (LegacyLand.getEcon().getBalance(player) < cost) {
                return OrganizationCreateResult.INSUFFICIENT_FUNDS;
            }
            LegacyLand.getEcon().withdrawPlayer(player, cost);
        }

        String id = UUID.randomUUID().toString();
        Organization org = new Organization(id, name, player.getUniqueId(), nationName);
        organizationsById.put(id, org);
        organizationsByName.put(name.toLowerCase(), org);
        playerOrgMap.put(player.getUniqueId(), id);

        saveOrganization(org);
        saveMember(id, org.getMember(player.getUniqueId()));

        return OrganizationCreateResult.SUCCESS;
    }

    /**
     * 解散组织（Leader 专属）
     */
    public boolean disbandOrganization(Player player) {
        Organization org = getPlayerOrganization(player.getUniqueId());
        if (org == null || !org.isLeader(player.getUniqueId())) return false;

        // 国家组织需要 MANAGE_ORGANIZATION 权限
        if (org.isNationalOrganization() && !NationManager.getInstance().hasPermission(player, NationPermission.MANAGE_ORGANIZATION)) {
            return false;
        }

        // 清理所有据点
        List<String> outpostIds = orgOutpostMap.getOrDefault(org.getId(), List.of());
        for (String oid : new ArrayList<>(outpostIds)) {
            deleteOutpost(org, oid);
        }

        // 从内存移除
        for (UUID uid : org.getMembers().keySet()) {
            playerOrgMap.remove(uid);
        }
        organizationsById.remove(org.getId());
        organizationsByName.remove(org.getName().toLowerCase());
        orgOutpostMap.remove(org.getId());

        // 从数据库删除
        deleteOrganizationFromDb(org.getId());
        return true;
    }

    /**
     * 邀请玩家加入
     */
    public boolean inviteMember(Organization org, Player target, OrganizationRole role) {
        if (org.isMember(target.getUniqueId())) return false;
        if (playerOrgMap.containsKey(target.getUniqueId())) return false;

        // 国家组织：只能邀请同国成员
        if (org.isNationalOrganization()) {
            Resident resident = TownyAPI.getInstance().getResident(target);
            Nation nation = resident != null ? TownyAPI.getInstance().getResidentNationOrNull(resident) : null;
            if (nation == null || !nation.getName().equals(org.getNationName())) return false;
        }

        org.addMember(target.getUniqueId(), role);
        playerOrgMap.put(target.getUniqueId(), org.getId());
        saveMember(org.getId(), org.getMember(target.getUniqueId()));
        return true;
    }

    /**
     * 踢出成员
     */
    public boolean kickMember(Organization org, UUID targetId) {
        if (!org.isMember(targetId) || org.isLeader(targetId)) return false;
        org.removeMember(targetId);
        playerOrgMap.remove(targetId);
        deleteMemberFromDb(org.getId(), targetId);
        return true;
    }

    /**
     * 设置成员角色
     */
    public boolean setMemberRole(Organization org, UUID targetId, OrganizationRole role) {
        OrganizationMember member = org.getMember(targetId);
        if (member == null || org.isLeader(targetId)) return false;
        member.setRole(role);
        // 重置为默认权限
        member.getPermissions().clear();
        member.getPermissions().addAll(Set.of(OrganizationPermission.getDefaultPermissions(role)));
        saveMember(org.getId(), member);
        return true;
    }

    /**
     * 设置成员权限
     */
    public boolean setMemberPermission(Organization org, UUID targetId, OrganizationPermission perm, boolean grant) {
        OrganizationMember member = org.getMember(targetId);
        if (member == null || org.isLeader(targetId)) return false;
        if (grant) member.addPermission(perm);
        else member.removePermission(perm);
        saveMember(org.getId(), member);
        return true;
    }

    // ===================== 据点 CRUD =====================

    /**
     * 创建据点
     */
    public OutpostCreateResult createOutpost(Player player, Organization org) {
        if (!org.hasPermission(player.getUniqueId(), OrganizationPermission.CREATE_OUTPOST)
                && !org.isLeader(player.getUniqueId())) {
            return OutpostCreateResult.NO_PERMISSION;
        }

        Location loc = player.getLocation();

        // 必须在荒野（非 Towny 领地）
        if (TownyAPI.getInstance().getTownBlock(loc) != null) {
            return OutpostCreateResult.NOT_WILDERNESS;
        }

        // 检查是否与其他据点重叠
        int radius = plugin.getConfig().getInt("organization.outpost-radius", 16);
        for (Outpost existing : outpostsById.values()) {
            if (existing.isInRange(loc)) {
                return OutpostCreateResult.OVERLAPPING;
            }
        }

        // 扣费
        double cost = plugin.getConfig().getDouble("organization.outpost-cost", 1000.0);
        if (LegacyLand.getEcon() != null) {
            if (LegacyLand.getEcon().getBalance(player) < cost) {
                return OutpostCreateResult.INSUFFICIENT_FUNDS;
            }
            LegacyLand.getEcon().withdrawPlayer(player, cost);
        }

        String id = UUID.randomUUID().toString();
        Outpost outpost = new Outpost(id, org.getId(), loc, radius);
        outpostsById.put(id, outpost);
        orgOutpostMap.computeIfAbsent(org.getId(), k -> new ArrayList<>()).add(id);

        saveOutpost(outpost);
        return OutpostCreateResult.SUCCESS;
    }

    /**
     * 删除据点
     */
    public boolean deleteOutpost(Organization org, String outpostId) {
        Outpost outpost = outpostsById.get(outpostId);
        if (outpost == null || !outpost.getOrganizationId().equals(org.getId())) return false;

        outpostsById.remove(outpostId);
        List<String> list = orgOutpostMap.get(org.getId());
        if (list != null) list.remove(outpostId);

        deleteOutpostFromDb(outpostId);
        return true;
    }

    /**
     * 开/关据点
     */
    public boolean toggleOutpost(Organization org, String outpostId, boolean open) {
        Outpost outpost = outpostsById.get(outpostId);
        if (outpost == null || !outpost.getOrganizationId().equals(org.getId())) return false;
        if (open) outpost.open(); else outpost.close();
        saveOutpost(outpost);
        return true;
    }

    /**
     * 添加货物到据点
     */
    public boolean addGoods(Outpost outpost, org.bukkit.inventory.ItemStack item, double price, int quantity) {
        String id = UUID.randomUUID().toString();
        OutpostGoods goods = new OutpostGoods(id, outpost.getId(), item, price, quantity);
        outpost.addGoods(goods);
        saveOutpostGoods(goods);
        return true;
    }

    /**
     * 更新货物数量（保存到数据库）
     */
    public void updateGoodsQuantity(OutpostGoods goods) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE outpost_goods SET quantity=? WHERE id=?")) {
            ps.setInt(1, goods.getQuantity());
            ps.setString(2, goods.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 更新货物数量失败: " + e.getMessage());
        }
    }

    /**
     * 删除货物
     */
    public boolean removeGoods(Outpost outpost, String goodsId) {
        OutpostGoods goods = outpost.getGoods(goodsId);
        if (goods == null) return false;
        outpost.removeGoods(goodsId);
        deleteGoodsFromDb(goodsId);
        return true;
    }

    // ===================== 查询 =====================

    public Organization getOrganizationByName(String name) {
        return organizationsByName.get(name.toLowerCase());
    }

    public Organization getOrganizationById(String id) {
        return organizationsById.get(id);
    }

    public Organization getPlayerOrganization(UUID playerId) {
        String orgId = playerOrgMap.get(playerId);
        return orgId != null ? organizationsById.get(orgId) : null;
    }

    public List<Outpost> getOrganizationOutposts(String orgId) {
        List<String> ids = orgOutpostMap.getOrDefault(orgId, List.of());
        List<Outpost> result = new ArrayList<>();
        for (String id : ids) {
            Outpost o = outpostsById.get(id);
            if (o != null) result.add(o);
        }
        return result;
    }

    public Outpost getOutpostById(String id) {
        return outpostsById.get(id);
    }

    /**
     * 检查玩家是否在某个组织的据点范围内
     */
    public Outpost getOutpostAt(Location location) {
        for (Outpost outpost : outpostsById.values()) {
            if (outpost.isInRange(location)) return outpost;
        }
        return null;
    }

    public Collection<Organization> getAllOrganizations() {
        return organizationsById.values();
    }

    // ===================== 数据库持久化 =====================

    private void saveOrganization(Organization org) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO organizations (id, name, leader_uuid, nation_name, created_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, org.getId());
            ps.setString(2, org.getName());
            ps.setString(3, org.getLeaderId().toString());
            ps.setString(4, org.getNationName());
            ps.setLong(5, org.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 保存组织失败: " + e.getMessage());
        }
    }

    private void saveMember(String orgId, OrganizationMember member) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO organization_members (org_id, player_uuid, role, permissions, joined_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, orgId);
            ps.setString(2, member.getPlayerId().toString());
            ps.setString(3, member.getRole().name());
            // 序列化自定义权限
            StringBuilder sb = new StringBuilder();
            for (OrganizationPermission p : member.getPermissions()) {
                if (!sb.isEmpty()) sb.append(",");
                sb.append(p.name());
            }
            ps.setString(4, sb.toString());
            ps.setLong(5, member.getJoinedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 保存成员失败: " + e.getMessage());
        }
    }

    private void saveOutpost(Outpost outpost) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO outposts (id, org_id, world, x, y, z, radius, status, created_at) VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, outpost.getId());
            ps.setString(2, outpost.getOrganizationId());
            ps.setString(3, outpost.getCenter().getWorld().getName());
            ps.setDouble(4, outpost.getCenter().getX());
            ps.setDouble(5, outpost.getCenter().getY());
            ps.setDouble(6, outpost.getCenter().getZ());
            ps.setInt(7, outpost.getRadius());
            ps.setString(8, outpost.getStatus().name());
            ps.setLong(9, outpost.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 保存据点失败: " + e.getMessage());
        }
    }

    private void saveOutpostGoods(OutpostGoods goods) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO outpost_goods (id, outpost_id, item_data, price, quantity, added_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, goods.getId());
            ps.setString(2, goods.getOutpostId());
            ps.setString(3, serializeItem(goods.getItem()));
            ps.setDouble(4, goods.getPrice());
            ps.setInt(5, goods.getQuantity());
            ps.setLong(6, goods.getAddedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 保存货物失败: " + e.getMessage());
        }
    }

    private void deleteOrganizationFromDb(String orgId) {
        try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM organizations WHERE id=?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM organization_members WHERE org_id=?")) {
            ps1.setString(1, orgId); ps1.executeUpdate();
            ps2.setString(1, orgId); ps2.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 删除组织失败: " + e.getMessage());
        }
    }

    private void deleteMemberFromDb(String orgId, UUID playerId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM organization_members WHERE org_id=? AND player_uuid=?")) {
            ps.setString(1, orgId);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 删除成员失败: " + e.getMessage());
        }
    }

    private void deleteOutpostFromDb(String outpostId) {
        try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM outposts WHERE id=?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM outpost_goods WHERE outpost_id=?")) {
            ps1.setString(1, outpostId); ps1.executeUpdate();
            ps2.setString(1, outpostId); ps2.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 删除据点失败: " + e.getMessage());
        }
    }

    private void deleteGoodsFromDb(String goodsId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM outpost_goods WHERE id=?")) {
            ps.setString(1, goodsId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Organization] 删除货物失败: " + e.getMessage());
        }
    }

    // ===================== 序列化 =====================

    private String serializeItem(org.bukkit.inventory.ItemStack item) {
        return java.util.Base64.getEncoder().encodeToString(
                item.serializeAsBytes());
    }

    private org.bukkit.inventory.ItemStack deserializeItem(String data) {
        try {
            return org.bukkit.inventory.ItemStack.deserializeBytes(
                    java.util.Base64.getDecoder().decode(data));
        } catch (Exception e) {
            logger.warning("[Organization] 物品反序列化失败: " + e.getMessage());
            return null;
        }
    }

    // ===================== 结果枚举 =====================

    public enum OrganizationCreateResult {
        SUCCESS, NAME_EXISTS, ALREADY_IN_ORG, INSUFFICIENT_FUNDS, NO_NATION_PERMISSION
    }

    public enum OutpostCreateResult {
        SUCCESS, NO_PERMISSION, NOT_WILDERNESS, OVERLAPPING, INSUFFICIENT_FUNDS
    }
}
