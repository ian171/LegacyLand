package net.chen.legacyLand.war;

import lombok.Getter;

/**
 * 战争类型
 */
@Getter
public enum WarType {
    /**
     * 对外战争 - 国家间战争
     */
    EXTERNAL("对外战争"),

    /**
     * 内战 - 叛军与镇压军
     */
    CIVIL("内战");

    private final String displayName;

    WarType(String displayName) {
        this.displayName = displayName;
    }
}
