package net.chen.legacyLand.organization;

/**
 * 组织角色枚举
 */
public enum OrganizationRole {
    LEADER("领导者"),
    MANAGER("管理者"),
    MEMBER("成员");

    private final String displayName;

    OrganizationRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
