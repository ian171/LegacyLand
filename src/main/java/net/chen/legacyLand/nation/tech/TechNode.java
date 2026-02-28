package net.chen.legacyLand.nation.tech;

import java.util.List;
import java.util.Map;

/**
 * 科技节点定义（从 tech.yml 加载）
 */
public record TechNode(
        String id,
        String lineId,
        int tier,
        String displayName,
        String description,
        int cost,
        List<String> prerequisites,
        Map<String, Double> effects
) {
    /** 检查某个效果键是否存在 */
    public boolean hasEffect(String key) {
        return effects != null && effects.containsKey(key);
    }

    /** 获取效果值，不存在返回 0 */
    public double getEffect(String key) {
        if (effects == null) return 0.0;
        return effects.getOrDefault(key, 0.0);
    }
}
