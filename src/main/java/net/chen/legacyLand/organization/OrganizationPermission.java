package net.chen.legacyLand.organization;

import lombok.Getter;

/**
 * 组织权限枚举
 */
@Getter
public enum OrganizationPermission {
    CREATE_OUTPOST("创建据点"),
    DELETE_OUTPOST("删除据点"),
    TRANSFER_OUTPOST("转让据点"),
    CLOSE_OUTPOST("关闭据点"),
    ADD_OUTPOST_GOODS("添加据点货物"),
    SET_GOODS_PRICE("设置货物价格"),
    SET_GOODS_TYPE("设置货物种类"),
    SET_GOODS_QUANTITY("设置货物数量"),
    MANAGE_OUTPOST_MEMBERS("管理据点成员");

    private final String displayName;

    OrganizationPermission(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取角色的默认权限
     */
    public static OrganizationPermission[] getDefaultPermissions(OrganizationRole role) {
        return switch (role) {
            case LEADER -> values(); // Leader 拥有所有权限
            case MANAGER -> new OrganizationPermission[]{
                    CREATE_OUTPOST,
                    CLOSE_OUTPOST,
                    ADD_OUTPOST_GOODS,
                    SET_GOODS_PRICE,
                    SET_GOODS_TYPE,
                    SET_GOODS_QUANTITY,
                    MANAGE_OUTPOST_MEMBERS
            };
            case MEMBER -> new OrganizationPermission[]{}; // Member 默认无权限
        };
    }
}
