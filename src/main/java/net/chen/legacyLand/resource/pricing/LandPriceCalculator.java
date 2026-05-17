package net.chen.legacyLand.resource.pricing;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;

/**
 * 地价计算器（P3）。
 * <p>
 * V(chunk) = base × (α·R(t) + β·biomeFactor + γ·locationFactor)
 * <ul>
 *   <li>R(t)：当前剩余储量（{@link ChunkResourceData#getCurrentValue}）。</li>
 *   <li>biomeFactor：群系系数（{@link ChunkResourceData#getBiomeFactor}），首扫时确定。</li>
 *   <li>locationFactor：距离所属城镇 spawn 的衰减；无城镇时为 0。</li>
 * </ul>
 */
public final class LandPriceCalculator {

    private LandPriceCalculator() {}

    /**
     * 计算并返回地价，若区块未普查则返回 {@code -1}。
     */
    public static double valuate(String world, int chunkX, int chunkZ, ResourcePricingConfig config) {
        ChunkResourceManager mgr = ChunkResourceManager.getInstance();
        if (mgr == null) return -1.0;

        ChunkResourceData data = mgr.get(world, chunkX, chunkZ).orElse(null);
        if (data == null) return -1.0;

        double r = Math.max(0.0, data.getCurrentValue());
        double biome = Math.max(0.0, data.getBiomeFactor());
        double location = locationFactor(world, chunkX, chunkZ, config);

        double v = config.getValuationAlpha() * r
                + config.getValuationBeta() * biome
                + config.getValuationGamma() * location;
        return Math.max(0.0, config.getValuationBase() * v);
    }

    /** 距离衰减：1 - min(1, dist/max)；无所属城镇时返回 0。 */
    private static double locationFactor(String world, int chunkX, int chunkZ, ResourcePricingConfig config) {
        try {
            org.bukkit.World bw = org.bukkit.Bukkit.getWorld(world);
            if (bw == null) return 0.0;
            Location center = new Location(bw, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
            var townBlock = TownyAPI.getInstance().getTownBlock(center);
            if (townBlock == null || !townBlock.hasTown()) return 0.0;
            Town town = townBlock.getTownOrNull();
            if (town == null || !town.hasSpawn()) return 0.0;
            Location spawn = town.getSpawn();
            if (spawn == null || spawn.getWorld() == null
                    || !spawn.getWorld().getName().equals(world)) return 0.0;
            double dx = center.getX() - spawn.getX();
            double dz = center.getZ() - spawn.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double max = Math.max(1.0, config.getLocationMaxDistance());
            return Math.max(0.0, 1.0 - Math.min(1.0, dist / max));
        } catch (Throwable t) {
            return 0.0;
        }
    }
}
