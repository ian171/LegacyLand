package net.chen.legacyLand.resource.pricing;

import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源稀缺度定价系统的运行参数（P1：扫描参数与权重；P2 将复用同一份配置）。
 * <p>
 * 从 config.yml 的 resource-pricing 段读取，缺省值在 fallback 中给出，
 * 保证插件首次启动不依赖外部配置即可运行。
 */
public class ResourcePricingConfig {

    private final Map<Material, Double> weights = new EnumMap<>(Material.class);
    private final Map<String, Double> biomeFactors = new HashMap<>();

    @Getter
    private boolean enabled = true;
    @Getter
    private int scanYMin = -64;
    @Getter
    private int scanYMax = 64;
    private double defaultBiomeFactor = 0.7;
    @Getter
    private long rescanIntervalMillis = 0L;
    @Getter
    private boolean rescanOnLoad = false;
    @Getter
    private boolean logVerbose = false;
    @Getter
    private int recalcIntervalTicks = 1200;
    @Getter
    private double explosionDecayFactor = 0.5;
    @Getter
    private double valuationAlpha = 1.0;
    @Getter
    private double valuationBeta = 200.0;
    @Getter
    private double valuationGamma = 100.0;
    @Getter
    private double valuationBase = 1.0;
    @Getter
    private double locationMaxDistance = 1000.0;
    @Getter
    private long inquiryTtlSeconds = 3600L;

    public void load(LegacyLand plugin) {
        weights.clear();
        biomeFactors.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("resource-pricing");
        if (root == null) {
            applyDefaults();
            return;
        }

        enabled = root.getBoolean("enabled", true);
        scanYMin = root.getInt("scan.y-min", -64);
        scanYMax = root.getInt("scan.y-max", 64);
        defaultBiomeFactor = root.getDouble("biome-factors.default", 0.7);
        rescanIntervalMillis = root.getLong("scan.rescan-interval-ms", 0L);
        rescanOnLoad = root.getBoolean("scan.rescan-on-load", false);
        logVerbose = root.getBoolean("log-verbose", false);
        recalcIntervalTicks = Math.max(20, root.getInt("recalc-interval-ticks", 1200));
        explosionDecayFactor = Math.max(0.0, Math.min(1.0, root.getDouble("explosion-decay-factor", 0.5)));
        valuationAlpha = root.getDouble("valuation.alpha", 1.0);
        valuationBeta = root.getDouble("valuation.beta", 200.0);
        valuationGamma = root.getDouble("valuation.gamma", 100.0);
        valuationBase = Math.max(0.0, root.getDouble("valuation.base", 1.0));
        locationMaxDistance = Math.max(1.0, root.getDouble("valuation.location-max-distance", 1000.0));
        inquiryTtlSeconds = Math.max(60L, root.getLong("inquiry-ttl-seconds", 3600L));

        ConfigurationSection weightSection = root.getConfigurationSection("weights");
        if (weightSection != null) {
            for (String key : weightSection.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat == null) {
                    LegacyLand.logger.warning("[ResourcePricing] 未知材质: " + key);
                    continue;
                }
                weights.put(mat, weightSection.getDouble(key));
            }
        }

        ConfigurationSection biomeSection = root.getConfigurationSection("biome-factors");
        if (biomeSection != null) {
            for (String key : biomeSection.getKeys(false)) {
                if (key.equalsIgnoreCase("default")) continue;
                biomeFactors.put(key.toUpperCase(), biomeSection.getDouble(key));
            }
        }

        if (weights.isEmpty()) applyDefaultWeights();
        if (biomeFactors.isEmpty()) applyDefaultBiomes();
    }

    private void applyDefaults() {
        applyDefaultWeights();
        applyDefaultBiomes();
    }

    private void applyDefaultWeights() {
        weights.put(Material.ANCIENT_DEBRIS, 500.0);
        weights.put(Material.DIAMOND_ORE, 200.0);
        weights.put(Material.DEEPSLATE_DIAMOND_ORE, 250.0);
        weights.put(Material.EMERALD_ORE, 150.0);
        weights.put(Material.DEEPSLATE_EMERALD_ORE, 200.0);
        weights.put(Material.GOLD_ORE, 30.0);
        weights.put(Material.DEEPSLATE_GOLD_ORE, 35.0);
        weights.put(Material.NETHER_GOLD_ORE, 8.0);
        weights.put(Material.LAPIS_ORE, 10.0);
        weights.put(Material.DEEPSLATE_LAPIS_ORE, 12.0);
        weights.put(Material.IRON_ORE, 5.0);
        weights.put(Material.DEEPSLATE_IRON_ORE, 6.0);
        weights.put(Material.REDSTONE_ORE, 4.0);
        weights.put(Material.DEEPSLATE_REDSTONE_ORE, 5.0);
        weights.put(Material.COPPER_ORE, 3.0);
        weights.put(Material.DEEPSLATE_COPPER_ORE, 3.0);
        weights.put(Material.NETHER_QUARTZ_ORE, 2.0);
        weights.put(Material.COAL_ORE, 1.0);
        weights.put(Material.DEEPSLATE_COAL_ORE, 1.0);
    }

    private void applyDefaultBiomes() {
        biomeFactors.put("DESERT", 0.3);
        biomeFactors.put("BADLANDS", 0.4);
        biomeFactors.put("PLAINS", 0.8);
        biomeFactors.put("FOREST", 0.8);
        biomeFactors.put("JUNGLE", 1.0);
        biomeFactors.put("MUSHROOM_FIELDS", 1.0);
        biomeFactors.put("DEEP_DARK", 1.5);
        biomeFactors.put("LUSH_CAVES", 1.2);
        biomeFactors.put("DRIPSTONE_CAVES", 1.1);
    }

    public double weightOf(Material material) {
        Double w = weights.get(material);
        return w == null ? 0.0 : w;
    }

    public double biomeFactorOf(String biomeName) {
        if (biomeName == null) return defaultBiomeFactor;
        return biomeFactors.getOrDefault(biomeName.toUpperCase(), defaultBiomeFactor);
    }

}
