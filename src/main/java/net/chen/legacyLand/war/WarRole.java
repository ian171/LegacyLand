package net.chen.legacyLand.war;

import lombok.Getter;

/**
 * 战争角色
 */
@Getter
public enum WarRole {
    /**
     * 战士 - 前线作战
     */
    SOLDIER("战士"),

    /**
     * 后勤兵 - 运送补给
     */
    LOGISTICS("后勤兵"),

    /**
     * 侦查员 - 建立前哨战
     */
    SCOUT("侦查员"),

    /**
     * 支援部队
     */
    SUPPORT("支援部队");

    private final String displayName;

    WarRole(String displayName) {
        this.displayName = displayName;
    }
}
