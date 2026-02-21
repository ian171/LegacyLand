package net.chen.legacyLand.war.siege;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * 攻城战管理器
 */
public class SiegeWarManager {
    private static SiegeWarManager instance;
    private final Map<String, SiegeWar> siegeWars;
    private final Map<String, String> warToSiegeMap;

    private SiegeWarManager() {
        this.siegeWars = new HashMap<>();
        this.warToSiegeMap = new HashMap<>();
    }

    public static SiegeWarManager getInstance() {
        if (instance == null) {
            instance = new SiegeWarManager();
        }
        return instance;
    }

    /**
     * 创建攻城战
     */
    public SiegeWar createSiegeWar(String warId, String attackerTown, String defenderTown) {
        String siegeId = UUID.randomUUID().toString();
        SiegeWar siegeWar = new SiegeWar(siegeId, warId, attackerTown, defenderTown);
        siegeWars.put(siegeId, siegeWar);
        warToSiegeMap.put(warId, siegeId);
        return siegeWar;
    }

    /**
     * 获取攻城战
     */
    public SiegeWar getSiegeWar(String siegeId) {
        return siegeWars.get(siegeId);
    }

    /**
     * 通过战争ID获取攻城战
     */
    public SiegeWar getSiegeWarByWarId(String warId) {
        String siegeId = warToSiegeMap.get(warId);
        return siegeId != null ? siegeWars.get(siegeId) : null;
    }

    /**
     * 建立前哨战
     */
    public boolean establishOutpost(String siegeId, Outpost outpost) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return false;

        // 检查位置是否有信标
        if (outpost.getLocation().getBlock().getType() != Material.BEACON) {
            return false;
        }

        siegeWar.setOutpost(outpost);
        return true;
    }

    /**
     * 创建补给线
     */
    public SupplyLine createSupplyLine(String siegeId, String town1, String town2) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return null;

        SupplyLine supplyLine = new SupplyLine(siegeWar.getWarId(), town1, town2);

        // 计算补给线路径
        calculateSupplyLinePath(supplyLine, town1, town2);

        siegeWar.addSupplyLine(supplyLine);
        return supplyLine;
    }

    /**
     * 计算补给线路径（简化版，直线距离）
     */
    private void calculateSupplyLinePath(SupplyLine supplyLine, String town1Name, String town2Name) {
        TownyAPI townyAPI = TownyAPI.getInstance();

        Town town1 = townyAPI.getTown(town1Name);
        Town town2 = townyAPI.getTown(town2Name);

        if (town1 == null || town2 == null) return;

        try {
            Location loc1 = town1.getSpawn();
            Location loc2 = town2.getSpawn();

            if (loc1 == null || loc2 == null) return;

            // 简化：添加起点和终点
            supplyLine.addPathPoint(loc1);
            supplyLine.addPathPoint(loc2);
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * 创建补给站
     */
    public boolean createSupplyStation(String siegeId, SupplyStation station) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return false;

        // 检查补给站数量限制（最多8个）
        if (siegeWar.getSupplyStationCount(station.getTownName()) >= 8) {
            return false;
        }

        // 检查位置是否有信标
        if (station.getLocation().getBlock().getType() != Material.BEACON) {
            return false;
        }

        siegeWar.addSupplyStation(station);
        return true;
    }

    /**
     * 摧毁补给站
     */
    public boolean destroySupplyStation(String siegeId, Location location) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return false;

        siegeWar.removeSupplyStation(location);
        return true;
    }

    /**
     * 检查两个城镇是否接壤
     */
    public boolean areTownsAdjacent(String town1Name, String town2Name) {
        TownyAPI townyAPI = TownyAPI.getInstance();

        Town town1 = townyAPI.getTown(town1Name);
        Town town2 = townyAPI.getTown(town2Name);

        if (town1 == null || town2 == null) return false;

        try {
            // 简化：检查两个城镇的出生点距离
            Location loc1 = town1.getSpawn();
            Location loc2 = town2.getSpawn();

            if (loc1 == null || loc2 == null) return false;

            // 小于128格视为接壤
            return loc1.distance(loc2) < 128;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加城市核心
     */
    public boolean addCityCore(String siegeId, String coreName, Location location) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return false;

        siegeWar.addCityCore(coreName, location);
        return true;
    }

    /**
     * 添加区块核心
     */
    public boolean addDistrictCore(String siegeId, String coreName, Location location) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return false;

        siegeWar.addDistrictCore(coreName, location);
        return true;
    }

    /**
     * 摧毁核心
     */
    public boolean destroyCore(String siegeId, String coreName) {
        SiegeWar siegeWar = siegeWars.get(siegeId);
        if (siegeWar == null) return false;

        siegeWar.destroyCore(coreName);
        return true;
    }

    /**
     * 获取所有攻城战
     */
    public Collection<SiegeWar> getAllSiegeWars() {
        return Collections.unmodifiableCollection(siegeWars.values());
    }

    /**
     * 删除攻城战
     */
    public void removeSiegeWar(String siegeId) {
        SiegeWar siegeWar = siegeWars.remove(siegeId);
        if (siegeWar != null) {
            warToSiegeMap.remove(siegeWar.getWarId());
        }
    }
}
