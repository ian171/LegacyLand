package net.chen.legacyLand.resource;

import lombok.Getter;
import org.bukkit.Material;

/**
 * 资源类型枚举
 * 定义不同品质的资源
 */
@Getter
public enum ResourceType {
    // 铁资源
    HIGH_PURITY_IRON("高纯度铁", Material.IRON_INGOT, 1.5, "§b高纯度铁"),
    IMPURE_IRON("杂质铁", Material.IRON_INGOT, 1.0, "§7杂质铁"),

    // 金资源
    HIGH_PURITY_GOLD("高纯度金", Material.GOLD_INGOT, 2.0, "§e高纯度金"),
    IMPURE_GOLD("杂质金", Material.GOLD_INGOT, 1.0, "§7杂质金"),

    // 铜资源
    HIGH_PURITY_COPPER("高纯度铜", Material.COPPER_INGOT, 1.3, "§6高纯度铜"),
    IMPURE_COPPER("杂质铜", Material.COPPER_INGOT, 1.0, "§7杂质铜");

    private final String displayName;
    private final Material baseMaterial;
    private final double weight; // 运输重量系数
    private final String coloredName;

    ResourceType(String displayName, Material baseMaterial, double weight, String coloredName) {
        this.displayName = displayName;
        this.baseMaterial = baseMaterial;
        this.weight = weight;
        this.coloredName = coloredName;
    }
}
