package net.chen.legacyLand.organization;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 组织数据类
 */
@Data
public class Organization {
    private final String id;
    private final String name;
    private final UUID leaderId;
    private final String nationName; // null 表示非国家组织
    private final Map<UUID, OrganizationMember> members;
    private final long createdAt;

    public Organization(String id, String name, UUID leaderId, String nationName) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.nationName = nationName;
        this.members = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();

        // 添加 Leader
        members.put(leaderId, new OrganizationMember(leaderId, OrganizationRole.LEADER));
    }

    /**
     * 是否是国家组织
     */
    public boolean isNationalOrganization() {
        return nationName != null;
    }

    /**
     * 添加成员
     */
    public void addMember(UUID playerId, OrganizationRole role) {
        members.put(playerId, new OrganizationMember(playerId, role));
    }

    /**
     * 移除成员
     */
    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    /**
     * 获取成员
     */
    public OrganizationMember getMember(UUID playerId) {
        return members.get(playerId);
    }

    /**
     * 检查玩家是否是成员
     */
    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    /**
     * 检查玩家是否是 Leader
     */
    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    /**
     * 检查玩家是否拥有指定权限
     */
    public boolean hasPermission(UUID playerId, OrganizationPermission permission) {
        OrganizationMember member = members.get(playerId);
        return member != null && member.hasPermission(permission);
    }

    /**
     * 获取所有成员列表
     */
    public Collection<OrganizationMember> getAllMembers() {
        return members.values();
    }
}
