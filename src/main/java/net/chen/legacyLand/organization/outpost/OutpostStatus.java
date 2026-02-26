package net.chen.legacyLand.organization.outpost;

/**
 * 据点状态枚举
 */
public enum OutpostStatus {
    OPEN("开放"),
    CLOSED("关闭");

    private final String displayName;

    OutpostStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
