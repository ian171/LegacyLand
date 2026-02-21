package net.chen.legacyLand.nation;

/**
 * 政体类型
 */
public enum GovernmentType {
    /**
     * 分封制
     */
    FEUDAL("分封制"),

    /**
     * 城市共和制
     */
    REPUBLIC("城市共和制");

    private final String displayName;

    GovernmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
