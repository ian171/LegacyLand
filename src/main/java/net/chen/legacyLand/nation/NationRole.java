package net.chen.legacyLand.nation;

import lombok.Getter;

import java.util.Set;

/**
 * 国家成员角色
 */
@Getter
public enum NationRole {
    // 分封制角色
    KINGDOM("国王", GovernmentType.FEUDAL, Set.of(
        NationPermission.APPOINT_ALL_POSITIONS,
        NationPermission.APPOINT_MAYOR,
        NationPermission.DECLARE_WAR,
        NationPermission.FORM_ALLIANCE,
        NationPermission.ADJUST_TAX_RATE,
        NationPermission.ADJUST_LAND_SALE_TAX,
        NationPermission.ADJUST_LAND_RENT_TAX,
        NationPermission.ALLOCATE_FUNDS_TO_CITY,
        NationPermission.ALLOCATE_FUNDS_TO_DEFENSE,
        NationPermission.ADJUST_VASSAL_TAX,
        NationPermission.WITHDRAW_TREASURY,
        NationPermission.APPROVE_TREASURY_REQUEST,
        NationPermission.JUDGE_DETAINED,
        NationPermission.ISSUE_ARREST_WARRANT,
        NationPermission.MANAGE_SUPPLY_LINE,
        NationPermission.ISSUE_SUPPLY_TRANSPORT,
        NationPermission.ADJUST_SUPPLY_REWARD,
        NationPermission.DECIDE_SIEGE_ROSTER
    )),

    CHANCELLOR("财政大臣", GovernmentType.FEUDAL, Set.of(
        NationPermission.ADJUST_TAX_RATE,
        NationPermission.ADJUST_LAND_SALE_TAX,
        NationPermission.ADJUST_LAND_RENT_TAX,
        NationPermission.ALLOCATE_FUNDS_TO_CITY,
        NationPermission.ALLOCATE_FUNDS_TO_DEFENSE,
        NationPermission.ADJUST_VASSAL_TAX,
        NationPermission.WITHDRAW_TREASURY,
        NationPermission.APPROVE_TREASURY_REQUEST
    )),

    ATTORNEY_GENERAL("司法大臣", GovernmentType.FEUDAL, Set.of(
        NationPermission.JUDGE_DETAINED
    )),

    MINISTER_OF_JUSTICE("执法大臣", GovernmentType.FEUDAL, Set.of(
        NationPermission.ISSUE_ARREST_WARRANT
    )),

    MINISTER_OF_DEFENSE("军需大臣", GovernmentType.FEUDAL, Set.of(
        NationPermission.MANAGE_SUPPLY_LINE,
        NationPermission.ISSUE_SUPPLY_TRANSPORT,
        NationPermission.ADJUST_SUPPLY_REWARD,
        NationPermission.DECIDE_SIEGE_ROSTER
    )),

    // 城市共和制角色
    GOVERNOR("总督", GovernmentType.REPUBLIC, Set.of(
        NationPermission.NOMINATE_POSITION,
        NationPermission.DISMISS_POSITION,
        NationPermission.APPOINT_MAYOR,
        NationPermission.DECLARE_WAR,
        NationPermission.FORM_ALLIANCE,
        NationPermission.ADJUST_TAX_RATE,
        NationPermission.ADJUST_LAND_SALE_TAX,
        NationPermission.ADJUST_LAND_RENT_TAX,
        NationPermission.ALLOCATE_FUNDS_TO_CITY,
        NationPermission.ALLOCATE_FUNDS_TO_DEFENSE,
        NationPermission.WITHDRAW_TREASURY,
        NationPermission.APPROVE_TREASURY_REQUEST,
        NationPermission.JUDGE_DETAINED,
        NationPermission.ISSUE_ARREST_WARRANT,
        NationPermission.MANAGE_SUPPLY_LINE,
        NationPermission.ISSUE_SUPPLY_TRANSPORT,
        NationPermission.ADJUST_SUPPLY_REWARD,
        NationPermission.DECIDE_SIEGE_ROSTER
    )),

    FINANCE_OFFICER("财政官", GovernmentType.REPUBLIC, Set.of(
        NationPermission.ADJUST_TAX_RATE,
        NationPermission.ADJUST_LAND_SALE_TAX,
        NationPermission.ADJUST_LAND_RENT_TAX,
        NationPermission.ALLOCATE_FUNDS_TO_CITY,
        NationPermission.ALLOCATE_FUNDS_TO_DEFENSE,
        NationPermission.WITHDRAW_TREASURY,
        NationPermission.APPROVE_TREASURY_REQUEST
    )),

    JUDICIAL_OFFICER("司法官", GovernmentType.REPUBLIC, Set.of(
        NationPermission.JUDGE_DETAINED
    )),

    LEGAL_OFFICER("执法官", GovernmentType.REPUBLIC, Set.of(
        NationPermission.ISSUE_ARREST_WARRANT
    )),

    MILITARY_SUPPLY_OFFICER("军需官", GovernmentType.REPUBLIC, Set.of(
        NationPermission.MANAGE_SUPPLY_LINE,
        NationPermission.ADJUST_SUPPLY_REWARD,
        NationPermission.DECIDE_SIEGE_ROSTER
    )),

    PARLIAMENT_MEMBER("议会议员", GovernmentType.REPUBLIC, Set.of(
        NationPermission.VOTE_GOVERNOR,
        NationPermission.VOTE_NOMINATION,
        NationPermission.PROPOSE_DIPLOMACY,
        NationPermission.VOTE_DIPLOMACY
    )),

    // 通用角色
    CITIZEN("公民", null, Set.of());

    private final String displayName;
    private final GovernmentType governmentType;
    private final Set<NationPermission> permissions;

    NationRole(String displayName, GovernmentType governmentType, Set<NationPermission> permissions) {
        this.displayName = displayName;
        this.governmentType = governmentType;
        this.permissions = permissions;
    }

    public boolean hasPermission(NationPermission permission) {
        return permissions.contains(permission);
    }

    public boolean isLeader() {
        return this == KINGDOM || this == GOVERNOR;
    }
}
