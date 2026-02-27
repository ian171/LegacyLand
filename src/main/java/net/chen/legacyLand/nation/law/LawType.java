package net.chen.legacyLand.nation.law;

import lombok.Getter;

/**
 * 法令类型枚举
 */
@Getter
public enum LawType {
    TRADE_EMBARGO("禁运令", "禁止与指定国家进行贸易"),
    TRADE_TAX_MODIFIER("贸易税调整令", "调整贸易税率"),
    IMPORT_BAN("进口禁令", "禁止进口指定材料"),
    WAR_MOBILIZATION("战争动员令", "提升军事强度"),
    CONSCRIPTION("征兵令", "增加兵员名额"),
    CURFEW("宵禁令", "限制特定时段的玩家移动"),
    AMNESTY("大赦令", "赦免被拘留的玩家"),
    RESEARCH_BOOST("科研促进令", "加速科技研究点生成");

    private final String displayName;
    private final String description;

    LawType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
