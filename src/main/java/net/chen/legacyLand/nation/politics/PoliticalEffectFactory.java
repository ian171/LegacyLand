package net.chen.legacyLand.nation.politics;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.Map;

/**
 * 政治效果工厂 - 用于从配置创建效果实例
 */
@FunctionalInterface
public interface PoliticalEffectFactory {
    /**
     * 从配置创建效果实例
     *
     * @param nation 国家
     * @param config 配置参数
     * @return 效果实例
     */
    PoliticalEffect create(Nation nation, Map<String, Object> config);
}
