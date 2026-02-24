package net.chen.legacyLand.nation.politics;

import com.palmergames.bukkit.towny.object.Nation;
import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 政治体制管理器 - 从 politics.yml 加载政体定义，管理效果生命周期
 */
public class PoliticalSystemManager {
    private static PoliticalSystemManager instance;

    @Getter
    private final Map<String, PoliticalSystem> systems = new LinkedHashMap<>();
    private final Map<String, List<PoliticalEffect>> registeredEffects = new ConcurrentHashMap<>();
    private final Map<String, PoliticalEffectFactory> effectFactories = new ConcurrentHashMap<>();
    private final Map<String, List<PoliticalEffect>> activeNationEffects = new ConcurrentHashMap<>();
    private final Map<String, Long> changeCooldowns = new ConcurrentHashMap<>();

    @Getter
    private double changeCost;
    @Getter
    private long changeCooldown;

    private PoliticalSystemManager() {
    }

    public static PoliticalSystemManager getInstance() {
        if (instance == null) {
            instance = new PoliticalSystemManager();
        }
        return instance;
    }

    /**
     * 从 politics.yml 加载所有政体定义
     */
    public void load(LegacyLand plugin) {
        systems.clear();

        File file = new File(plugin.getDataFolder(), "politics.yml");
        if (!file.exists()) {
            plugin.saveResource("politics.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 合并默认配置中缺失的项
        InputStream defaultStream = plugin.getResource("politics.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        this.changeCooldown = config.getLong("change-cooldown", 86400) * 1000L;
        this.changeCost = config.getDouble("change-cost", 500.0);

        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection == null) {
            LegacyLand.logger.warning("politics.yml 中未找到 types 配置！");
            return;
        }

        for (String key : typesSection.getKeys(false)) {
            ConfigurationSection typeSection = typesSection.getConfigurationSection(key);
            if (typeSection == null) continue;

            String displayName = typeSection.getString("display-name", key);
            String description = typeSection.getString("description", "empty");
            List<String> roles = typeSection.getStringList("roles");

            // 加载效果
            Map<String, Double> effects = new HashMap<>();
            ConfigurationSection effectsSection = typeSection.getConfigurationSection("effects");
            if (effectsSection != null) {
                for (String effectKey : effectsSection.getKeys(false)) {
                    effects.put(effectKey, effectsSection.getDouble(effectKey, 1.0));
                }
            }

            // 加载自定义效果（从顶层 custom-effects 读取）
            Map<String, Object> customEffects = new HashMap<>();
            ConfigurationSection customSection = typeSection.getConfigurationSection("custom-effects");
            if (customSection != null) {
                for (String customKey : customSection.getKeys(false)) {
                    Object value = customSection.get(customKey);
                    if (value instanceof ConfigurationSection) {
                        // 转换为 Map
                        ConfigurationSection subSection = (ConfigurationSection) value;
                        Map<String, Object> subMap = new HashMap<>();
                        for (String subKey : subSection.getKeys(false)) {
                            subMap.put(subKey, subSection.get(subKey));
                        }
                        customEffects.put(customKey, subMap);
                    } else {
                        customEffects.put(customKey, value);
                    }
                }
            }

            PoliticalSystem system = new PoliticalSystem(key, displayName, description, roles, effects, customEffects);
            systems.put(key, system);
            //LegacyLand.logger.info("已加载政体: " + key + " (" + displayName + ")");
        }

        LegacyLand.logger.info("共加载 " + systems.size() + " 种政治体制。");
    }

    /**
     * 获取指定政体
     */
    public PoliticalSystem getSystem(String id) {
        return systems.get(id);
    }

    /**
     * 获取默认政体（配置中的第一个）
     */
    public PoliticalSystem getDefaultSystem() {
        if (systems.isEmpty()) return null;
        return systems.values().iterator().next();
    }

    /**
     * 检查政体ID是否有效
     */
    public boolean isValidSystem(String id) {
        return systems.containsKey(id);
    }

    /**
     * 获取所有政体ID
     */
    public Set<String> getSystemIds() {
        return systems.keySet();
    }

    // ========== 效果注册与管理 ==========

    /**
     * 为指定政体注册自定义效果
     *
     * @param systemId 政体ID
     * @param effect   效果实现
     */
    public void registerEffect(String systemId, PoliticalEffect effect) {
        registeredEffects.computeIfAbsent(systemId, k -> new ArrayList<>()).add(effect);
    }

    /**
     * 注册效果工厂（用于从配置创建效果）
     *
     * @param effectName 效果名称（对应配置中的 custom-effects 键）
     * @param factory    效果工厂
     */
    public void registerEffectFactory(String effectName, PoliticalEffectFactory factory) {
        effectFactories.put(effectName, factory);
    }

    /**
     * 当国家切换政体时，触发效果变更
     *
     * @param nation      国家
     * @param oldSystemId 旧政体ID（可为null）
     * @param newSystemId 新政体ID
     */
    public void applySystemChange(Nation nation, String oldSystemId, String newSystemId) {
        // 移除旧政体的注册效果
        if (oldSystemId != null) {
            List<PoliticalEffect> oldEffects = registeredEffects.get(oldSystemId);
            if (oldEffects != null) {
                for (PoliticalEffect effect : oldEffects) {
                    effect.onRemove(nation);
                }
            }
        }

        // 移除旧政体的工厂创建效果（粒子等）
        List<PoliticalEffect> oldActiveEffects = activeNationEffects.remove(nation.getName());
        if (oldActiveEffects != null) {
            for (PoliticalEffect effect : oldActiveEffects) {
                effect.onRemove(nation);
            }
        }

        // 应用新政体的注册效果
        List<PoliticalEffect> newEffects = registeredEffects.get(newSystemId);
        if (newEffects != null) {
            for (PoliticalEffect effect : newEffects) {
                effect.onApply(nation);
            }
        }

        // 从配置创建并应用自定义效果，同时跟踪它们
        PoliticalSystem newSystem = systems.get(newSystemId);
        if (newSystem != null && newSystem.customEffects() != null) {
            List<PoliticalEffect> newActiveEffects = new ArrayList<>();
            for (Map.Entry<String, Object> entry : newSystem.customEffects().entrySet()) {
                String effectName = entry.getKey();
                PoliticalEffectFactory factory = effectFactories.get(effectName);
                if (factory != null && entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) entry.getValue();
                    PoliticalEffect effect = factory.create(nation, config);
                    if (effect != null) {
                        effect.onApply(nation);
                        newActiveEffects.add(effect);
                    }
                }
            }
            if (!newActiveEffects.isEmpty()) {
                activeNationEffects.put(nation.getName(), newActiveEffects);
            }
        }

        // 记录冷却时间
        changeCooldowns.put(nation.getName(), System.currentTimeMillis());
    }

    /**
     * 检查国家是否在切换冷却中
     */
    public boolean isOnCooldown(String nationName) {
        Long lastChange = changeCooldowns.get(nationName);
        if (lastChange == null) return false;
        return (System.currentTimeMillis() - lastChange) < changeCooldown;
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    public long getRemainingCooldown(String nationName) {
        Long lastChange = changeCooldowns.get(nationName);
        if (lastChange == null) return 0;
        long remaining = changeCooldown - (System.currentTimeMillis() - lastChange);
        return Math.max(0, remaining / 1000);
    }
}
