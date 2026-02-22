package net.chen.legacyLand.war.flagwar;

import lombok.Getter;

/**
 * FlagWar 状态枚举
 */
@Getter
public enum FlagWarStatus {
    /**
     * 进行中
     */
    ACTIVE("进行中"),

    /**
     * 攻击方胜利（计时器完成）
     */
    ATTACKER_VICTORY("攻击方胜利"),

    /**
     * 防守方胜利（旗帜被破坏）
     */
    DEFENDER_VICTORY("防守方胜利"),

    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String displayName;

    FlagWarStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 检查是否已结束
     */
    public boolean isEnded() {
        return this != ACTIVE;
    }
}
