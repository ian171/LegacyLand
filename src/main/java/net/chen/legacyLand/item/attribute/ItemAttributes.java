package net.chen.legacyLand.item.attribute;

import lombok.Getter;

/**
 * 物品属性数据类，使用 Builder 模式构建
 */
@Getter
public final class ItemAttributes {

    private final double attackDamage;
    private final double attackSpeed;
    private final double weight;
    private final double maxTemperature;
    private final TechRequirement requiredTech;

    private ItemAttributes(Builder builder) {
        this.attackDamage   = builder.attackDamage;
        this.attackSpeed    = builder.attackSpeed;
        this.weight         = builder.weight;
        this.maxTemperature = builder.maxTemperature;
        this.requiredTech   = builder.requiredTech;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private double attackDamage   = 1.0;
        private double attackSpeed    = 1.6;
        private double weight         = 1.0;
        private double maxTemperature = 100.0;
        private TechRequirement requiredTech = null;

        public Builder damage(double val)         { this.attackDamage = val;   return this; }
        public Builder speed(double val)          { this.attackSpeed = val;    return this; }
        public Builder weight(double val)         { this.weight = val;         return this; }
        public Builder maxTemp(double val)        { this.maxTemperature = val; return this; }

        /** 科技需求：科技线 ID + 最低 tier，如 tech("INDUSTRIAL", 5) */
        public Builder tech(String lineId, int tier) {
            this.requiredTech = new TechRequirement(lineId, tier);
            return this;
        }

        public ItemAttributes build() { return new ItemAttributes(this); }
    }
}
