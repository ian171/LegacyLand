package net.chen.legacyLand.war;

import lombok.Getter;

/**
 * 战争状态
 */
@Getter
public enum WarStatus {
    /**
     * 准备中 - 前哨战建立阶段
     */
    PREPARING("准备中"),

    /**
     * 进行中
     */
    ACTIVE("进行中"),

    /**
     * 已结束 - 胜利
     */
    ENDED_VICTORY("已结束-胜利"),

    /**
     * 已结束 - 失败
     */
    ENDED_DEFEAT("已结束-失败"),

    /**
     * 已结束 - 平局
     */
    ENDED_DRAW("已结束-平局"),

    /**
     * 已结束 - 投降
     */
    ENDED_SURRENDER("已结束-投降");

    private final String displayName;

    WarStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnded() {
        return this == ENDED_VICTORY || this == ENDED_DEFEAT ||
               this == ENDED_DRAW || this == ENDED_SURRENDER;
    }
}
