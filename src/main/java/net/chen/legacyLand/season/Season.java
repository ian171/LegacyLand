package net.chen.legacyLand.season;

import lombok.Getter;

/**
 * 季节枚举类
 * 定义四季及其子季节
 */
@Getter
public enum Season {
    // 春季 - 初始温度 22.0
    EARLY_SPRING("early_spring", "初春", SeasonType.SPRING, 22.0),
    MID_SPRING("mid_spring", "仲春", SeasonType.SPRING, 22.0),
    LATE_SPRING("late_spring", "晚春", SeasonType.SPRING, 22.0),

    // 夏季 - 初始温度 30.0
    EARLY_SUMMER("early_summer", "初夏", SeasonType.SUMMER, 30.0),
    MID_SUMMER("mid_summer", "仲夏", SeasonType.SUMMER, 30.0),
    LATE_SUMMER("late_summer", "晚夏", SeasonType.SUMMER, 30.0),

    // 秋季 - 初始温度 21.0
    EARLY_AUTUMN("early_autumn", "初秋", SeasonType.AUTUMN, 21.0),
    MID_AUTUMN("mid_autumn", "仲秋", SeasonType.AUTUMN, 21.0),
    LATE_AUTUMN("late_autumn", "晚秋", SeasonType.AUTUMN, 21.0),

    // 冬季 - 初始温度 15.0
    EARLY_WINTER("early_winter", "初冬", SeasonType.WINTER, 15.0),
    MID_WINTER("mid_winter", "仲冬", SeasonType.WINTER, 15.0),
    LATE_WINTER("late_winter", "晚冬", SeasonType.WINTER, 15.0);

    private final String key;
    private final String displayName;
    private final SeasonType type;
    private final double baseTemperature;

    Season(String key, String displayName, SeasonType type, double baseTemperature) {
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.baseTemperature = baseTemperature;
    }

    /**
     * 获取下一个季节
     */
    public Season next() {
        Season[] seasons = values();
        int nextIndex = (this.ordinal() + 1) % seasons.length;
        return seasons[nextIndex];
    }

    /**
     * 根据key获取季节
     */
    public static Season fromKey(String key) {
        for (Season season : values()) {
            if (season.key.equalsIgnoreCase(key)) {
                return season;
            }
        }
        return EARLY_SPRING; // 默认返回初春
    }

    /**
     * 季节类型枚举
     */
    @Getter
    public enum SeasonType {
        SPRING("spring", "春季"),
        SUMMER("summer", "夏季"),
        AUTUMN("autumn", "秋季"),
        WINTER("winter", "冬季");

        private final String key;
        private final String displayName;

        SeasonType(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }
    }
}
