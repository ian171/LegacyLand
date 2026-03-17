package net.chen.legacyLand.item.attribute;

/**
 * 物品科技需求：科技线 ID + 最低等级（tier）
 */
public record TechRequirement(String lineId, int requiredTier) {

    /**
     * 检查国家已完成的节点中，指定科技线的最高 tier 是否满足需求
     *
     * @param nationMaxTier 国家在该科技线上已达到的最高 tier
     */
    public boolean isMet(int nationMaxTier) {
        return nationMaxTier >= requiredTier;
    }

    /**
     * 计算效能比例（0.0 ~ 1.0），不足时按比例降低
     */
    public double efficiencyRatio(int nationMaxTier) {
        if (nationMaxTier >= requiredTier) return 1.0;
        if (requiredTier == 0) return 1.0;
        return (double) nationMaxTier / requiredTier;
    }
}
