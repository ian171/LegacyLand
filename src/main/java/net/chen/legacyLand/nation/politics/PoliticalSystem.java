package net.chen.legacyLand.nation.politics;

import net.chen.legacyLand.nation.NationRole;

import java.util.*;

/**
 * 政治体制 - 从配置文件加载的政体定义
 */
public record PoliticalSystem(String id, String displayName, String description, List<String> roleNames,
                              Map<String, Double> effects, Map<String, Object> customEffects) {

    /**
     * 获取税收效率倍率
     */
    public double getTaxEfficiency() {
        return effects.getOrDefault("tax-efficiency", 1.0);
    }

    /**
     * 获取军事力量倍率
     */
    public double getMilitaryStrength() {
        return effects.getOrDefault("military-strength", 1.0);
    }

    /**
     * 获取最大同盟数量修正
     */
    public int getMaxAllianceModifier() {
        return effects.getOrDefault("max-alliance-modifier", 0.0).intValue();
    }

    /**
     * 获取城镇最大数量修正
     */
    public int getMaxTownModifier() {
        return effects.getOrDefault("max-town-modifier", 0.0).intValue();
    }

    /**
     * 获取国库收入倍率
     */
    public double getTreasuryIncome() {
        return effects.getOrDefault("treasury-income", 1.0);
    }

    /**
     * 获取战争冷却时间倍率
     */
    public double getWarCooldownModifier() {
        return effects.getOrDefault("war-cooldown-modifier", 1.0);
    }

    /**
     * 获取指定效果值
     *
     * @param key          效果键名
     * @param defaultValue 默认值
     * @return 效果值
     */
    public double getEffect(String key, double defaultValue) {
        return effects.getOrDefault(key, defaultValue);
    }

    /**
     * 检查该政体下是否允许指定角色
     */
    public boolean isRoleAllowed(NationRole role) {
        return roleNames.contains(role.name());
    }

    /**
     * 获取该政体下允许的 NationRole 列表
     */
    public List<NationRole> getAllowedRoles() {
        List<NationRole> roles = new ArrayList<>();
        for (String name : roleNames) {
            try {
                roles.add(NationRole.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // 配置中的角色名无效，跳过
            }
        }
        return roles;
    }

    /**
     * 获取该政体的领袖角色
     */
    public NationRole getLeaderRole() {
        for (NationRole role : getAllowedRoles()) {
            if (role.isLeader()) {
                return role;
            }
        }
        // 默认返回 KINGDOM
        return NationRole.KINGDOM;
    }
}
