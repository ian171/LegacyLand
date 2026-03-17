package net.chen.legacyLand.item.attribute;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.nation.tech.TechManager;
import net.chen.legacyLand.nation.tech.TechNode;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 属性动态修正工具类
 * 根据玩家所属国家的科技等级计算物品实际效能
 */
public final class AttributeEvaluator {

    private AttributeEvaluator() {}

    /**
     * 计算物品在当前玩家国家科技下的实际攻击力
     */
    public static double getEffectiveAttackDamage(Player player, ItemAttributes attrs) {
        return attrs.getAttackDamage() * getEfficiencyRatio(player, attrs);
    }

    /**
     * 获取效能比例（0.0 ~ 1.0）
     * 满足科技需求返回 1.0，不足则按比例降低
     */
    public static double getEfficiencyRatio(Player player, ItemAttributes attrs) {
        TechRequirement req = attrs.getRequiredTech();
        if (req == null) return 1.0;

        int nationTier = getNationTierForLine(player, req.lineId());
        return req.efficiencyRatio(nationTier);
    }

    /**
     * 是否满足科技需求
     */
    public static boolean meetsTechRequirement(Player player, ItemAttributes attrs) {
        TechRequirement req = attrs.getRequiredTech();
        if (req == null) return true;
        return req.isMet(getNationTierForLine(player, req.lineId()));
    }

    /**
     * 获取玩家所属国家在指定科技线上已达到的最高 tier
     */
    public static int getNationTierForLine(Player player, String lineId) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasNation()) return 0;
            Nation nation = resident.getNationOrNull();
            if (nation == null) return 0;

            TechManager techManager = TechManager.getInstance();
            if (techManager == null) return 0;

            // 遍历该科技线所有节点，找出国家已完成的最高 tier
            List<TechNode> nodes = techManager.getLineNodes().get(lineId.toUpperCase());
            if (nodes == null || nodes.isEmpty()) return 0;

            int maxTier = 0;
            for (TechNode node : nodes) {
                if (techManager.getState(nation.getName()) != null
                        && techManager.getState(nation.getName()).hasCompleted(node.id())) {
                    maxTier = Math.max(maxTier, node.tier());
                }
            }
            return maxTier;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 生成 Lore 警告行（科技不足时显示红色提示）
     */
    public static String buildTechWarningLore(Player player, ItemAttributes attrs) {
        TechRequirement req = attrs.getRequiredTech();
        if (req == null) return null;

        int nationTier = getNationTierForLine(player, req.lineId());
        if (req.isMet(nationTier)) return null;

        int pct = (int) ((1.0 - req.efficiencyRatio(nationTier)) * 100);
        return "§c⚠ 科技不足 [" + req.lineId() + " Tier " + req.requiredTier()
                + "]，攻击力降低 " + pct + "%";
    }
}
