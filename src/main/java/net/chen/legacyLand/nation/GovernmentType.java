package net.chen.legacyLand.nation;

import lombok.Getter;

/**
 * 政体类型
 */
@Getter
public enum GovernmentType {
    /**
     * 分封制
     */
    FEUDAL("分封制"),
    /**
     * 城市共和制
     */
    REPUBLIC("城市共和制"),
    /**
     * 君主立宪制
     */
    CONSTITUTIONAL_MONARCHY("君主立宪制"),
    /**
     * 君主专制(试验性）
     */
    ABSOLUTE_MONARCHY("君主专制");

    private final String displayName;

    GovernmentType(String displayName) {
        this.displayName = displayName;
    }

}
