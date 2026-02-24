package net.chen.legacyLand.nation;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import lombok.Setter;
import net.chen.legacyLand.database.DatabaseManager;
import net.chen.legacyLand.nation.politics.PoliticalSystem;
import net.chen.legacyLand.nation.politics.PoliticalSystemManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 国家扩展数据管理器 - 存储 Towny 国家的扩展信息
 */
public class NationManager {
    private static NationManager instance;
    private final Map<String, GovernmentType> nationGovernments; // 国家名 -> 政体（旧）
    private final Map<String, String> nationPoliticalSystems; // 国家名 -> 政体ID（新，配置驱动）
    private final Map<String, Map<UUID, NationRole>> nationRoles; // 国家名 -> (玩家UUID -> 角色)
    @Setter
    private DatabaseManager database;
    private final TownyAPI townyAPI;

    private NationManager() {
        this.nationGovernments = new HashMap<>();
        this.nationPoliticalSystems = new HashMap<>();
        this.nationRoles = new HashMap<>();
        this.townyAPI = TownyAPI.getInstance();
    }

    public static NationManager getInstance() {
        if (instance == null) {
            instance = new NationManager();
        }
        return instance;
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
