package net.chen.legacyLand.player;

import lombok.Getter;

/**
 * 职业枚举
 */
@Getter
public enum Profession {
    // 战斗职业
    WARRIOR("战士", ProfessionType.COMBAT, 2.0, 0.0),
    ARCHER("弓箭手", ProfessionType.COMBAT, 1.0, 1.5),
    KNIGHT("骑士", ProfessionType.COMBAT, 2.5, 0.0),

    // 生产职业
    MINER("矿工", ProfessionType.PRODUCTION, 0.0, 0.0),
    FARMER("农民", ProfessionType.PRODUCTION, 0.0, 0.0),
    LUMBERJACK("伐木工", ProfessionType.PRODUCTION, 0.5, 0.0),

    // 工艺职业
    BLACKSMITH("铁匠", ProfessionType.CRAFTING, 0.5, 0.0),
    CARPENTER("木匠", ProfessionType.CRAFTING, 0.0, 0.0),
    ALCHEMIST("炼金术士", ProfessionType.CRAFTING, 0.0, 0.5),

    // 商业职业
    MERCHANT("商人", ProfessionType.COMMERCE, 0.0, 0.0),
    TRADER("贸易商", ProfessionType.COMMERCE, 0.0, 0.0);

    private final String displayName;
    private final ProfessionType type;
    private final double attackBonus;      // 攻击加成
    private final double defenseBonus;     // 防御加成

    Profession(String displayName, ProfessionType type, double attackBonus, double defenseBonus) {
        this.displayName = displayName;
        this.type = type;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
    }

    /**
     * 职业类型
     */
    public enum ProfessionType {
        COMBAT("战斗"),
        PRODUCTION("生产"),
        CRAFTING("工艺"),
        COMMERCE("商业");

        @Getter
        private final String displayName;

        ProfessionType(String displayName) {
            this.displayName = displayName;
        }
    }
}
