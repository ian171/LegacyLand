package net.chen.legacyLand.nation.diplomacy;

import lombok.Getter;

/**
 * 外交关系类型
 */
@Getter
public enum RelationType {
    /**
     * 战争状态
     */
    WAR("战争"),

    /**
     * 敌对状态
     */
    HOSTILE("敌对"),

    /**
     * 中立状态
     */
    NEUTRAL("中立"),

    /**
     * 友好状态
     */
    FRIENDLY("友好"),

    /**
     * 同盟 - 共同防御协议
     */
    ALLIANCE_DEFENSIVE("共同防御同盟"),

    /**
     * 同盟 - 共同进攻协议
     */
    ALLIANCE_OFFENSIVE("共同进攻同盟"),

    /**
     * 贸易协议
     */
    TRADE_AGREEMENT("贸易协议"),

    /**
     * 科技协议
     */
    TECH_AGREEMENT("科技协议");

    private final String displayName;

    RelationType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAlliance() {
        return this == ALLIANCE_DEFENSIVE || this == ALLIANCE_OFFENSIVE;
    }

    public boolean isHostile() {
        return this == WAR || this == HOSTILE;
    }

    public boolean isFriendly() {
        return this == FRIENDLY || isAlliance() || this == TRADE_AGREEMENT || this == TECH_AGREEMENT;
    }
}
