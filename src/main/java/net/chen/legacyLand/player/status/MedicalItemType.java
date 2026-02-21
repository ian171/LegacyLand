package net.chen.legacyLand.player.status;

import lombok.Getter;

/**
 * 医疗物品类型
 */
@Getter
public enum MedicalItemType {
    /**
     * 绷带 - 治疗刀伤、箭伤
     */
    BANDAGE("bandage", "绷带", 5.0),

    /**
     * 草药包 - 治疗砸伤、水肿、火伤
     */
    HERBAL_MEDICINE("herbal_medicine", "草药包", 8.0),

    /**
     * 夹板 - 治疗骨折
     */
    SPLINT("splint", "夹板", 0.0),

    /**
     * 解毒剂 - 治疗中毒
     */
    ANTIDOTE("antidote", "解毒剂", 3.0),

    /**
     * 止血药 - 治疗流血
     */
    HEMOSTATIC_MEDICINE("hemostatic_medicine", "止血药", 6.0),

    /**
     * 万能药 - 治疗所有伤势
     */
    PANACEA("panacea", "万能药", 20.0);

    private final String itemId;
    private final String displayName;
    private final double healAmount; // 恢复的生命值

    MedicalItemType(String itemId, String displayName, double healAmount) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.healAmount = healAmount;
    }

    /**
     * 根据物品ID获取医疗物品类型
     */
    public static MedicalItemType fromItemId(String itemId) {
        for (MedicalItemType type : values()) {
            if (type.itemId.equalsIgnoreCase(itemId)) {
                return type;
            }
        }
        return null;
    }
}
