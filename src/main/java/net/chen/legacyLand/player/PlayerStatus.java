package net.chen.legacyLand.player;

import lombok.Getter;

/**
 * 玩家状态效果
 */
@Getter
public enum PlayerStatus {
    // 精神状态
    FOOD_POISONING("食物中毒", StatusType.MENTAL, 300),
    HEATSTROKE("中暑", StatusType.MENTAL, 180),
    HYPOTHERMIA("失温", StatusType.MENTAL, 180),

    // 战斗受伤
    BLEEDING("流血", StatusType.INJURY, 120),
    FRACTURE("骨折", StatusType.INJURY, 600),
    ARROW_WOUND("箭伤", StatusType.INJURY, 240),
    CONCUSSION("脑震荡", StatusType.INJURY, 300),
    BURN("烧伤", StatusType.INJURY, 180),
    POISONED("中毒", StatusType.INJURY, 240),

    // 生活受伤
    FALL_INJURY("摔伤", StatusType.LIFE_INJURY, 180),
    WORK_INJURY("工伤", StatusType.LIFE_INJURY, 120),
    FOOT_DISEASE("脚气", StatusType.LIFE_INJURY, 600);

    private final String displayName;
    private final StatusType type;
    private final int duration;  // 持续时间(秒)

    PlayerStatus(String displayName, StatusType type, int duration) {
        this.displayName = displayName;
        this.type = type;
        this.duration = duration;
    }

    /**
     * 状态类型
     */
    public enum StatusType {
        MENTAL("精神状态"),
        INJURY("战斗受伤"),
        LIFE_INJURY("生活受伤");

        @Getter
        private final String displayName;

        StatusType(String displayName) {
            this.displayName = displayName;
        }
    }
}
