package net.chen.legacyLand.resource;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生物群落资源配置
 * 定义不同生物群落的资源产出规则
 */
public class BiomeResourceConfig {

    // 生物群落 -> 方块类型 -> 资源掉落配置
    private static final Map<Biome, Map<Material, ResourceDrop>> BIOME_DROPS = new ConcurrentHashMap<>();

    static {
        // 沙漠群落 - 高纯度铁
        Map<Material, ResourceDrop> desertDrops = new HashMap<>();
        desertDrops.put(Material.IRON_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_IRON, 0.10));
        desertDrops.put(Material.DEEPSLATE_IRON_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_IRON, 0.10));
        desertDrops.put(Material.GOLD_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_GOLD, 0.15));
        desertDrops.put(Material.DEEPSLATE_GOLD_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_GOLD, 0.15));
        BIOME_DROPS.put(Biome.DESERT, desertDrops);

        // 恶地群落 - 高纯度金
        Map<Material, ResourceDrop> badlandsDrops = new HashMap<>();
        badlandsDrops.put(Material.GOLD_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_GOLD, 0.20));
        badlandsDrops.put(Material.DEEPSLATE_GOLD_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_GOLD, 0.20));
        BIOME_DROPS.put(Biome.BADLANDS, badlandsDrops);
        BIOME_DROPS.put(Biome.ERODED_BADLANDS, badlandsDrops);
        BIOME_DROPS.put(Biome.WOODED_BADLANDS, badlandsDrops);

        // 山地群落 - 高纯度铜
        Map<Material, ResourceDrop> mountainDrops = new HashMap<>();
        mountainDrops.put(Material.COPPER_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_COPPER, 0.12));
        mountainDrops.put(Material.DEEPSLATE_COPPER_ORE, new ResourceDrop(ResourceType.HIGH_PURITY_COPPER, 0.12));
        BIOME_DROPS.put(Biome.STONY_PEAKS, mountainDrops);
        BIOME_DROPS.put(Biome.JAGGED_PEAKS, mountainDrops);
        BIOME_DROPS.put(Biome.FROZEN_PEAKS, mountainDrops);
    }

    /**
     * 获取生物群落的资源掉落配置
     */
    public static Optional<ResourceDrop> getResourceDrop(Biome biome, Material blockType) {
        Map<Material, ResourceDrop> drops = BIOME_DROPS.get(biome);
        if (drops == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(drops.get(blockType));
    }

    /**
     * 获取默认掉落（杂质资源）
     */
    public static ResourceType getDefaultDrop(Material blockType) {
        return switch (blockType) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> ResourceType.IMPURE_IRON;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> ResourceType.IMPURE_GOLD;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> ResourceType.IMPURE_COPPER;
            default -> null;
        };
    }

    /**
     * 资源掉落配置
     */
    public record ResourceDrop(ResourceType resourceType, double probability) {
        public boolean shouldDrop() {
            return Math.random() < probability;
        }
    }
}
