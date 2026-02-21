package net.chen.legacyLand.war.siege;

import lombok.Data;
import org.bukkit.Location;

import java.util.*;

/**
 * 攻城战数据模型
 */
@Data
public class SiegeWar {
    private final String siegeId;
    private final String warId;
    private final String attackerTown;
    private final String defenderTown;

    // 前哨战
    private Outpost outpost;

    // 补给线
    private final List<SupplyLine> supplyLines;

    // 补给站
    private final Map<Location, SupplyStation> supplyStations;

    // 城市核心位置
    private final Map<String, Location> cityCores;

    // 区块核心位置（科技区、工业区等）
    private final Map<String, Location> districtCores;

    // 被摧毁的核心
    private final Set<String> destroyedCores;

    // 战争区域
    private final List<Location> warZone;

    // 前线区域
    private final List<Location> frontline;

    public SiegeWar(String siegeId, String warId, String attackerTown, String defenderTown) {
        this.siegeId = siegeId;
        this.warId = warId;
        this.attackerTown = attackerTown;
        this.defenderTown = defenderTown;
        this.supplyLines = new ArrayList<>();
        this.supplyStations = new HashMap<>();
        this.cityCores = new HashMap<>();
        this.districtCores = new HashMap<>();
        this.destroyedCores = new HashSet<>();
        this.warZone = new ArrayList<>();
        this.frontline = new ArrayList<>();
    }

    /**
     * 设置前哨战
     */
    public void setOutpost(Outpost outpost) {
        this.outpost = outpost;
    }

    /**
     * 添加补给线
     */
    public void addSupplyLine(SupplyLine supplyLine) {
        supplyLines.add(supplyLine);
    }

    /**
     * 添加补给站
     */
    public void addSupplyStation(SupplyStation station) {
        supplyStations.put(station.getLocation(), station);
    }

    /**
     * 移除补给站
     */
    public void removeSupplyStation(Location location) {
        supplyStations.remove(location);
    }

    /**
     * 添加城市核心
     */
    public void addCityCore(String coreName, Location location) {
        cityCores.put(coreName, location);
    }

    /**
     * 添加区块核心
     */
    public void addDistrictCore(String coreName, Location location) {
        districtCores.put(coreName, location);
    }

    /**
     * 摧毁核心
     */
    public void destroyCore(String coreName) {
        destroyedCores.add(coreName);
    }

    /**
     * 检查所有核心是否被摧毁
     */
    public boolean areAllCoresDestroyed() {
        int totalCores = cityCores.size() + districtCores.size();
        return destroyedCores.size() >= totalCores;
    }

    /**
     * 检查补给线是否被切断
     */
    public boolean isSupplyLineCut() {
        return supplyLines.stream().allMatch(SupplyLine::isCut);
    }

    /**
     * 获取补给站数量
     */
    public int getSupplyStationCount(String townName) {
        return (int) supplyStations.values().stream()
                .filter(s -> s.getTownName().equals(townName))
                .filter(SupplyStation::isActive)
                .count();
    }

    /**
     * 检查位置是否在战争区
     */
    public boolean isInWarZone(Location location) {
        return warZone.stream().anyMatch(loc ->
                loc.getWorld().equals(location.getWorld()) &&
                loc.distance(location) <= 16); // 一个区块的范围
    }

    /**
     * 检查位置是否在前线
     */
    public boolean isInFrontline(Location location) {
        return frontline.stream().anyMatch(loc ->
                loc.getWorld().equals(location.getWorld()) &&
                loc.distance(location) <= 16);
    }
}
