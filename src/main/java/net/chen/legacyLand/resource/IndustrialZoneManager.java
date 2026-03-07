package net.chen.legacyLand.resource;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工业区域管理器
 * 管理国家熔炉等特殊工业区域
 */
public class IndustrialZoneManager {
    private static IndustrialZoneManager instance;

    // 国家名 -> 工业区域配置
    private final Map<String, IndustrialZone> industrialZones;

    private IndustrialZoneManager() {
        this.industrialZones = new ConcurrentHashMap<>();
    }

    public static IndustrialZoneManager getInstance() {
        if (instance == null) {
            synchronized (IndustrialZoneManager.class) {
                if (instance == null) {
                    instance = new IndustrialZoneManager();
                }
            }
        }
        return instance;
    }

    /**
     * 注册工业区域
     */
    public void registerZone(String nationName, IndustrialZone zone) {
        industrialZones.put(nationName, zone);
    }

    /**
     * 检查位置是否在工业区域内
     */
    public Optional<IndustrialZone> getZoneAt(Location location) {
        return industrialZones.values().stream()
                .filter(zone -> zone.contains(location))
                .findFirst();
    }

    /**
     * 检查是否可以在该位置精炼资源
     */
    public boolean canRefineAt(Location location, ResourceType resourceType) {
        Optional<IndustrialZone> zone = getZoneAt(location);
        return zone.map(z -> z.canRefine(resourceType)).orElse(false);
    }

    /**
     * 工业区域配置
     */
    public static class IndustrialZone {
        private final String nationName;
        private final Location center;
        private final double radius;
        private final ZoneType type;

        public IndustrialZone(String nationName, Location center, double radius, ZoneType type) {
            this.nationName = nationName;
            this.center = center;
            this.radius = radius;
            this.type = type;
        }

        public boolean contains(Location location) {
            if (!location.getWorld().equals(center.getWorld())) {
                return false;
            }
            return location.distance(center) <= radius;
        }

        public boolean canRefine(ResourceType resourceType) {
            return type == ZoneType.NATIONAL_FURNACE;
        }

        public String getNationName() {
            return nationName;
        }

        public ZoneType getType() {
            return type;
        }
    }

    /**
     * 区域类型
     */
    public enum ZoneType {
        NATIONAL_FURNACE("国家熔炉"),
        MINT_FACTORY("国家印钞厂"),
        PROCESSING_PLANT("加工厂");

        private final String displayName;

        ZoneType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
