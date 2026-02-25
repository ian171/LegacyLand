package net.chen.legacyLand.nation;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import lombok.Setter;
import net.chen.legacyLand.database.DatabaseManager;
import net.chen.legacyLand.nation.politics.PoliticalSystem;
import net.chen.legacyLand.nation.politics.PoliticalSystemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * 国家扩展数据管理器 - 存储 Towny 国家的扩展信息
 */
public class NationManager {
    private static NationManager instance;
    @Deprecated
    private final Map<String, GovernmentType> nationGovernments; // 国家名 -> 政体（旧）
    private final Map<String, String> nationPoliticalSystems; // 国家名 -> 政体ID（新，配置驱动）
    private final Map<String, Map<UUID, NationRole>> nationRoles; // 国家名 -> (玩家UUID -> 角色)
    private final Map<String, Location> nationTreasury; // 国家名 -> 国库位置
    private final Map<String, List<TreasuryRequest>> treasuryRequests; // 国家名 -> 待审批申请列表
    @Setter
    private DatabaseManager database;
    private final TownyAPI townyAPI;

    private NationManager() {
        this.nationGovernments = new HashMap<>();
        this.nationPoliticalSystems = new HashMap<>();
        this.nationRoles = new HashMap<>();
        this.townyAPI = TownyAPI.getInstance();
        this.nationTreasury = new HashMap<>();
        this.treasuryRequests = new HashMap<>();
    }

    public static NationManager getInstance() {
        if (instance == null) {
            instance = new NationManager();
        }
        return instance;
    }

    /**
     * 设置国家国库箱子位置
     * @param nationName 国家名
     * @param location 箱子位置
     * @return 是否设置成功
     */
    public boolean setTreasury(@NotNull String nationName, @NotNull Location location) {
        if (location.getBlock().getType() != Material.CHEST) {
            return false;
        }

        // 校验箱子是否在国家首都城镇领土内
        TownBlock townBlock = townyAPI.getTownBlock(location);
        if (townBlock == null || !townBlock.hasTown()) {
            return false;
        }

        Nation nation = townyAPI.getNation(nationName);
        if (nation == null || !nation.hasCapital()) {
            return false;
        }

        Town capital = nation.getCapital();
        if (!townBlock.getTownOrNull().equals(capital)) {
            return false;
        }
        Chest chest = (Chest) location.getBlock();
        chest.customName(Component.text("国库"));
        nationTreasury.put(nationName, location);
        return true;
    }

    /**
     * 获取国家国库箱子
     * @param nationName 国家名
     * @return 国库箱子，不存在或方块已变更则返回 null
     */
    public Chest getTreasuryChest(String nationName) {
        Location location = nationTreasury.get(nationName);
        if (location == null) {
            return null;
        }
        if (location.getBlock().getType() != Material.CHEST) {
            return null;
        }
        return (Chest) location.getBlock().getState();
    }

    /**
     * 捐赠物品到国库
     * @param player 捐赠者
     * @param item 要捐赠的物品
     */
    public void donateToTreasury(Player player, ItemStack item) {
        Nation nation = getPlayerNation(player);
        if (nation == null) {
            player.sendMessage("§c你不属于任何国家。");
            return;
        }

        Chest chest = getTreasuryChest(nation.getName());
        if (chest == null) {
            player.sendMessage("§c国库箱子未设置或已被破坏。");
            return;
        }

        Inventory chestInv = chest.getInventory();
        HashMap<Integer, ItemStack> remaining = chestInv.addItem(item.clone());
        if (!remaining.isEmpty()) {
            player.sendMessage("§c国库箱子已满，无法捐赠。");
            return;
        }

        player.getInventory().removeItem(item);
        player.sendMessage("§a成功向国库捐赠了 " + item.getAmount() + " 个 " + item.getType().name() + "。");
    }
    /**
     * 向指定国家的国库存入物品（用于交易等系统调用）
     * @param nation 目标国家
     * @param item 要存入的物品
     * @return 是否存入成功
     */
    public boolean donateToTreasury(@NotNull Nation nation, ItemStack item) {
        Chest chest = getTreasuryChest(nation.getName());
        if (chest == null) {
            return false;
        }

        Inventory chestInv = chest.getInventory();
        HashMap<Integer, ItemStack> remaining = chestInv.addItem(item.clone());
        return remaining.isEmpty();
    }

    /**
     * 申请从国库取物
     * @param player 申请人
     * @param item 要取的物品
     */
    public void requestWithdraw(Player player, ItemStack item) {
        Nation nation = getPlayerNation(player);
        if (nation == null) {
            player.sendMessage("§c你不属于任何国家。");
            return;
        }

        if (!hasPermission(player, NationPermission.WITHDRAW_TREASURY)) {
            player.sendMessage("§c你没有从国库取物的权限。");
            return;
        }

        Chest chest = getTreasuryChest(nation.getName());
        if (chest == null) {
            player.sendMessage("§c国库箱子未设置或已被破坏。");
            return;
        }

        NationRole role = getPlayerRole(nation.getName(), player.getUniqueId());
        if (role.isLeader()) {
            // 领导人直接取物
            if (!chest.getInventory().containsAtLeast(item, item.getAmount())) {
                player.sendMessage("§c国库中没有足够的该物品。");
                return;
            }
            chest.getInventory().removeItem(item);
            player.getInventory().addItem(item);
            player.sendMessage("§a已从国库取出 " + item.getAmount() + " 个 " + item.getType().name() + "。");
        } else {
            // 非领导人创建审批申请
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            TreasuryRequest request = new TreasuryRequest(
                    requestId,
                    player.getUniqueId(),
                    nation.getName(),
                    item,
                    System.currentTimeMillis()
            );
            treasuryRequests.computeIfAbsent(nation.getName(), k -> new ArrayList<>()).add(request);
            player.sendMessage("§a取物申请已提交，申请编号: " + requestId + "，等待审批。");

            // 通知有审批权限的在线成员
            for (Resident resident : nation.getResidents()) {
                Player online = Bukkit.getPlayer(resident.getUUID());
                if (online != null && online.isOnline()) {
                    NationRole onlineRole = getPlayerRole(nation.getName(), online.getUniqueId());
                    if (onlineRole.hasPermission(NationPermission.APPROVE_TREASURY_REQUEST)) {
                        online.sendMessage("§e[国库] " + player.getName() + " 申请取出 "
                                + item.getAmount() + " 个 " + item.getType().name()
                                + "，申请编号: " + requestId);
                    }
                }
            }
        }
    }
    public void forceWithdraw(Player player, ItemStack item) {
        Nation nation = getPlayerNation(player);
        if (nation == null) {
            player.sendMessage("§c你不属于任何国家。");
            return;
        }

        if (!hasPermission(player, NationPermission.WITHDRAW_TREASURY)) {
            player.sendMessage("§c你没有从国库取物的权限。");
            return;
        }

        if (!withdrawFromNationTreasury(nation.getName(), item)) {
            player.sendMessage("§c国库箱子未设置、已被破坏或物品不足。");
            return;
        }
        player.getInventory().addItem(item);
        player.sendMessage("§a已从国库强制取出 " + item.getAmount() + " 个 " + item.getType().name() + "。");
    }

    /**
     * 从指定国家的国库取出物品（用于交易等系统调用）
     * @param nationName 国家名
     * @param item 要取出的物品
     * @return 是否取出成功
     */
    public boolean withdrawFromNationTreasury(String nationName, ItemStack item) {
        Chest chest = getTreasuryChest(nationName);
        if (chest == null) {
            return false;
        }
        if (!chest.getInventory().containsAtLeast(item, item.getAmount())) {
            return false;
        }
        chest.getInventory().removeItem(item);
        return true;
    }

    /**
     * 审批通过取物申请
     * @param approver 审批人
     * @param request 申请
     */
    public void approveRequest(Player approver, TreasuryRequest request) {
        if (!hasPermission(approver, NationPermission.APPROVE_TREASURY_REQUEST)) {
            approver.sendMessage("§c你没有审批国库申请的权限。");
            return;
        }

        Chest chest = getTreasuryChest(request.getNationName());
        if (chest == null) {
            approver.sendMessage("§c国库箱子未设置或已被破坏。");
            return;
        }

        ItemStack item = request.getRequestedItem();
        if (!chest.getInventory().containsAtLeast(item, item.getAmount())) {
            approver.sendMessage("§c国库中没有足够的该物品。");
            return;
        }

        Player requester = Bukkit.getPlayer(request.getPlayerId());
        if (requester == null || !requester.isOnline()) {
            approver.sendMessage("§c申请人不在线，无法转交物品。");
            return;
        }

        chest.getInventory().removeItem(item);
        requester.getInventory().addItem(item);

        // 移除已处理的申请
        List<TreasuryRequest> requests = treasuryRequests.get(request.getNationName());
        if (requests != null) {
            requests.remove(request);
        }

        approver.sendMessage("§a已批准申请 " + request.getRequestId() + "，物品已转交给 " + requester.getName() + "。");
        requester.sendMessage("§a你的国库取物申请 " + request.getRequestId() + " 已被批准。");
    }

    /**
     * 拒绝取物申请
     * @param approver 审批人
     * @param request 申请
     */
    public void denyRequest(Player approver, TreasuryRequest request) {
        if (!hasPermission(approver, NationPermission.APPROVE_TREASURY_REQUEST)) {
            approver.sendMessage("§c你没有审批国库申请的权限。");
            return;
        }

        List<TreasuryRequest> requests = treasuryRequests.get(request.getNationName());
        if (requests != null) {
            requests.remove(request);
        }

        approver.sendMessage("§a已拒绝申请 " + request.getRequestId() + "。");

        Player requester = Bukkit.getPlayer(request.getPlayerId());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§c你的国库取物申请 " + request.getRequestId() + " 已被拒绝。");
        }
    }

    /**
     * 获取国家待审批的国库申请列表
     * @param nationName 国家名
     * @return 待审批申请列表
     */
    public List<TreasuryRequest> getRequests(String nationName) {
        return treasuryRequests.getOrDefault(nationName, Collections.emptyList());
    }

    /**
     * 设置国家政体
     */
    public void setGovernmentType(String nationName, GovernmentType governmentType) {
        nationGovernments.put(nationName, governmentType);

        if (database != null) {
            database.saveNationGovernment(nationName, governmentType);
        }
    }

    /**
     * 获取国家政体
     */
    public GovernmentType getGovernmentType(String nationName) {
        return nationGovernments.getOrDefault(nationName, GovernmentType.FEUDAL);
    }

    /**
     * 设置玩家角色
     */
    public void setPlayerRole(String nationName, UUID playerId, NationRole role) {
        nationRoles.computeIfAbsent(nationName, k -> new HashMap<>()).put(playerId, role);

        if (database != null) {
            database.savePlayerRole(nationName, playerId, role);
        }
    }

    /**
     * 获取玩家角色
     */
    public NationRole getPlayerRole(String nationName, UUID playerId) {
        Map<UUID, NationRole> roles = nationRoles.get(nationName);
        if (roles != null && roles.containsKey(playerId)) {
            return roles.get(playerId);
        }

        // 检查是否是 Towny 的国家领袖
        Nation nation = townyAPI.getNation(nationName);
        if (nation != null) {
            Resident king = nation.getKing();
            if (king != null && king.getUUID().equals(playerId)) {
                // 优先使用新政体系统获取领袖角色
                PoliticalSystem system = getPoliticalSystem(nationName);
                if (system != null) {
                    return system.getLeaderRole();
                }
                GovernmentType govType = getGovernmentType(nationName);
                return govType == GovernmentType.FEUDAL ? NationRole.KINGDOM : NationRole.GOVERNOR;
            }
        }

        return NationRole.CITIZEN;
    }

    /**
     * 检查玩家是否有权限
     */
    public boolean hasPermission(Player player, NationPermission permission) {
        Resident resident = townyAPI.getResident(player);
        if (resident == null || !resident.hasNation()) {
            return false;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            return false;
        }

        NationRole role = getPlayerRole(nation.getName(), player.getUniqueId());
        return role.hasPermission(permission);
    }

    /**
     * 获取玩家所在国家
     */
    public Nation getPlayerNation(Player player) {
        Resident resident = townyAPI.getResident(player);
        if (resident != null && resident.hasNation()) {
            return resident.getNationOrNull();
        }
        return null;
    }

    /**
     * 加载国家扩展数据
     */
    public void loadNationData(String nationName) {
        if (database != null) {
            GovernmentType govType = database.loadNationGovernment(nationName);
            if (govType != null) {
                nationGovernments.put(nationName, govType);
            }

            String systemId = database.loadNationPoliticalSystem(nationName);
            if (systemId != null) {
                nationPoliticalSystems.put(nationName, systemId);
            }

            Map<UUID, NationRole> roles = database.loadNationRoles(nationName);
            if (roles != null && !roles.isEmpty()) {
                nationRoles.put(nationName, roles);
            }
        }
    }

    /**
     * 移除国家扩展数据
     */
    public void removeNationData(String nationName) {
        nationGovernments.remove(nationName);
        nationPoliticalSystems.remove(nationName);
        nationRoles.remove(nationName);

        if (database != null) {
            database.deleteNationData(nationName);
        }
    }

    // ========== 政治体制（配置驱动） ==========

    /**
     * 设置国家政治体制
     *
     * @param nationName 国家名
     * @param systemId   政体ID（对应 politics.yml 中的 key）
     */
    public void setPoliticalSystem(String nationName, String systemId) {
        String oldSystemId = nationPoliticalSystems.get(nationName);
        nationPoliticalSystems.put(nationName, systemId);

        if (database != null) {
            database.saveNationPoliticalSystem(nationName, systemId);
        }

        // 触发效果变更
        Nation nation = townyAPI.getNation(nationName);
        if (nation != null) {
            PoliticalSystemManager.getInstance().applySystemChange(nation, oldSystemId, systemId);
        }
    }

    /**
     * 获取国家政治体制ID
     */
    public String getPoliticalSystemId(String nationName) {
        return nationPoliticalSystems.getOrDefault(nationName, "FEUDAL");
    }

    /**
     * 获取国家政治体制对象
     */
    public PoliticalSystem getPoliticalSystem(String nationName) {
        String systemId = getPoliticalSystemId(nationName);
        return PoliticalSystemManager.getInstance().getSystem(systemId);
    }
}
