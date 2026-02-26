package net.chen.legacyLand.organization;

import lombok.Data;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * 组织成员数据类
 */
@Data
public class OrganizationMember {
    private final UUID playerId;
    private OrganizationRole role;
    private final Set<OrganizationPermission> permissions;
    private final long joinedAt;

    public OrganizationMember(UUID playerId, OrganizationRole role) {
        this.playerId = playerId;
        this.role = role;
        this.permissions = EnumSet.noneOf(OrganizationPermission.class);
        this.joinedAt = System.currentTimeMillis();

        // 添加默认权限
        permissions.addAll(Set.of(OrganizationPermission.getDefaultPermissions(role)));
    }

    /**
     * 检查是否拥有指定权限
     */
    public boolean hasPermission(OrganizationPermission permission) {
        return role == OrganizationRole.LEADER || permissions.contains(permission);
    }

    /**
     * 添加权限
     */
    public void addPermission(OrganizationPermission permission) {
        permissions.add(permission);
    }

    /**
     * 移除权限
     */
    public void removePermission(OrganizationPermission permission) {
        permissions.remove(permission);
    }
}
