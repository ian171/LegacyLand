package net.chen.legacyLand.nation.diplomacy;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.database.DatabaseManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 外交管理器
 */
public class DiplomacyManager {
    private static DiplomacyManager instance;
    private final Map<String, List<DiplomacyRelation>> relations;
    private final DatabaseManager database;

    private DiplomacyManager() {
        this.relations = new HashMap<>();
        this.database = LegacyLand.getInstance().getDatabaseManager();
    }

    public static DiplomacyManager getInstance() {
        if (instance == null) {
            instance = new DiplomacyManager();
        }
        return instance;
    }

    /**
     * 设置外交关系
     */
    public boolean setRelation(String nation1, String nation2, RelationType type) {
        if (nation1.equals(nation2)) {
            return false;
        }

        // 确保 nation1 字典序小于 nation2，保持一致性
        if (nation1.compareTo(nation2) > 0) {
            String temp = nation1;
            nation1 = nation2;
            nation2 = temp;
        }

        // 移除旧关系
        removeRelation(nation1, nation2);

        // 创建新关系
        DiplomacyRelation relation = new DiplomacyRelation(nation1, nation2, type);

        // 添加到两个国家的关系列表
        relations.computeIfAbsent(nation1, k -> new ArrayList<>()).add(relation);
        relations.computeIfAbsent(nation2, k -> new ArrayList<>()).add(relation);

        // 保存到数据库
        database.saveDiplomacyRelation(relation);

        return true;
    }

    /**
     * 获取两国之间的关系
     */
    public RelationType getRelation(String nation1, String nation2) {
        if (nation1.equals(nation2)) {
            return RelationType.NEUTRAL;
        }

        List<DiplomacyRelation> nation1Relations = relations.get(nation1);
        if (nation1Relations == null) {
            return RelationType.NEUTRAL;
        }

        for (DiplomacyRelation relation : nation1Relations) {
            if (relation.matches(nation1, nation2)) {
                return relation.getRelationType();
            }
        }

        return RelationType.NEUTRAL;
    }

    /**
     * 移除外交关系（恢复中立）
     */
    public boolean removeRelation(String nation1, String nation2) {
        boolean removed = false;

        List<DiplomacyRelation> nation1Relations = relations.get(nation1);
        if (nation1Relations != null) {
            removed = nation1Relations.removeIf(r -> r.matches(nation1, nation2));
        }

        List<DiplomacyRelation> nation2Relations = relations.get(nation2);
        if (nation2Relations != null) {
            nation2Relations.removeIf(r -> r.matches(nation1, nation2));
        }

        if (removed) {
            database.deleteDiplomacyRelation(nation1, nation2);
        }

        return removed;
    }

    /**
     * 获取国家的所有外交关系
     */
    public List<DiplomacyRelation> getNationRelations(String nationName) {
        return relations.getOrDefault(nationName, new ArrayList<>());
    }

    /**
     * 获取国家的所有盟友
     */
    public List<String> getAllies(String nationName) {
        return getNationRelations(nationName).stream()
                .filter(r -> r.getRelationType().isAlliance())
                .map(r -> r.getOtherNation(nationName))
                .collect(Collectors.toList());
    }

    /**
     * 获取国家的所有敌人
     */
    public List<String> getEnemies(String nationName) {
        return getNationRelations(nationName).stream()
                .filter(r -> r.getRelationType().isHostile())
                .map(r -> r.getOtherNation(nationName))
                .collect(Collectors.toList());
    }

    /**
     * 检查两国是否处于战争状态
     */
    public boolean isAtWar(String nation1, String nation2) {
        return getRelation(nation1, nation2) == RelationType.WAR;
    }

    /**
     * 检查两国是否是盟友
     */
    public boolean isAllied(String nation1, String nation2) {
        RelationType relation = getRelation(nation1, nation2);
        return relation.isAlliance();
    }

    /**
     * 宣战
     */
    public boolean declareWar(String aggressor, String target) {
        RelationType currentRelation = getRelation(aggressor, target);

        // 如果已经在战争中，返回 false
        if (currentRelation == RelationType.WAR) {
            return false;
        }

        // 移除任何现有的友好关系
        if (currentRelation.isFriendly()) {
            removeRelation(aggressor, target);
        }

        return setRelation(aggressor, target, RelationType.WAR);
    }

    /**
     * 和平（恢复中立）
     */
    public boolean makePeace(String nation1, String nation2) {
        return removeRelation(nation1, nation2);
    }

    /**
     * 结盟
     */
    public boolean formAlliance(String nation1, String nation2, boolean isDefensive) {
        RelationType currentRelation = getRelation(nation1, nation2);

        // 如果在战争中，不能结盟
        if (currentRelation == RelationType.WAR) {
            return false;
        }

        RelationType allianceType = isDefensive ?
                RelationType.ALLIANCE_DEFENSIVE : RelationType.ALLIANCE_OFFENSIVE;

        return setRelation(nation1, nation2, allianceType);
    }

    /**
     * 签订贸易协议
     */
    public boolean signTradeAgreement(String nation1, String nation2) {
        RelationType currentRelation = getRelation(nation1, nation2);

        if (currentRelation == RelationType.WAR) {
            return false;
        }

        return setRelation(nation1, nation2, RelationType.TRADE_AGREEMENT);
    }

    /**
     * 签订科技协议
     */
    public boolean signTechAgreement(String nation1, String nation2) {
        RelationType currentRelation = getRelation(nation1, nation2);

        if (currentRelation == RelationType.WAR) {
            return false;
        }

        return setRelation(nation1, nation2, RelationType.TECH_AGREEMENT);
    }

    /**
     * 清除国家的所有外交关系
     */
    public void clearNationRelations(String nationName) {
        List<DiplomacyRelation> nationRelations = relations.remove(nationName);
        if (nationRelations != null) {
            for (DiplomacyRelation relation : nationRelations) {
                String otherNation = relation.getOtherNation(nationName);
                List<DiplomacyRelation> otherRelations = relations.get(otherNation);
                if (otherRelations != null) {
                    otherRelations.removeIf(r -> r.involves(nationName));
                }
            }
        }
    }
}
