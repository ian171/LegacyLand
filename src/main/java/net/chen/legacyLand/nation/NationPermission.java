package net.chen.legacyLand.nation;

import lombok.Getter;

/**
 * 国家权限
 */
@Getter
public enum NationPermission {
    // 人事权限
    APPOINT_ALL_POSITIONS("任免所有职位"),
    NOMINATE_POSITION("提名职位"),
    DISMISS_POSITION("罢免职位"),
    APPOINT_MAYOR("任免市长"),

    // 外交权限
    DECLARE_WAR("宣战"),
    FORM_ALLIANCE("结盟"),
    PROPOSE_DIPLOMACY("提出外交议案"),
    VOTE_DIPLOMACY("投票外交议案"),

    // 财政权限
    ADJUST_TAX_RATE("调整税收比例"),
    ADJUST_LAND_SALE_TAX("调整土地出售税"),
    ADJUST_LAND_RENT_TAX("调整土地租赁税"),
    ALLOCATE_FUNDS_TO_CITY("向城市拨款"),
    ALLOCATE_FUNDS_TO_DEFENSE("向军需拨款"),
    ADJUST_VASSAL_TAX("调整封臣税"),

    // 司法权限
    JUDGE_DETAINED("审判被拘留者"),
    ISSUE_ARREST_WARRANT("发布逮捕令"),

    // 军事权限
    MANAGE_SUPPLY_LINE("管理补给线"),
    ISSUE_SUPPLY_TRANSPORT("发布补给运送任务"),
    ADJUST_SUPPLY_REWARD("调整补给奖励"),
    DECIDE_SIEGE_ROSTER("决定攻城名单"),

    // 议会权限
    VOTE_GOVERNOR("投票选举总督"),
    VOTE_NOMINATION("投票官员提名");

    private final String description;

    NationPermission(String description) {
        this.description = description;
    }

}
