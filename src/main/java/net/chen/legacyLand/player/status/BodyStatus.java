package net.chen.legacyLand.player.status;

import lombok.Getter;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * 身体状态（精神状态）
 */
@Getter
public enum BodyStatus {
    /**
     * 正常状态
     */
    NORMAL("正常", "正常状态，无BUFF", new ArrayList<>()),

    /**
     * 愉悦状态
     * 条件：生命值为满，饱食度大于90%
     * BUFF：速度+5%，生命恢复30s（下次触发CD为5min）
     */
    JOYFUL("愉悦", "生命值为满，饱食度大于90%", List.of(
            new PotionEffect(PotionEffectType.SPEED, 600, 0), // 30s 速度I
            new PotionEffect(PotionEffectType.REGENERATION, 600, 0) // 30s 生命恢复I
    )),

    /**
     * 失落状态
     * 条件：生命值低于20%，饱食度低于10%
     * BUFF：缓慢+5%，厌食（食物按原有值50%饱食度摄入）
     */
    DEPRESSED("失落", "生命值低于20%，饱食度低于10%", List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 600, 0) // 30s 缓慢I
    )),

    /**
     * 紧张状态
     * 条件：战争发动
     * BUFF：速度+15%，力量+5%
     */
    NERVOUS("紧张", "战争发动", List.of(
            new PotionEffect(PotionEffectType.SPEED, 1200, 1), // 60s 速度II
            new PotionEffect(PotionEffectType.STRENGTH, 1200, 0) // 60s 力量I
    )),

    /**
     * 厌食状态
     * 条件：频繁食用单一食物（存在15次起判断）
     * BUFF：反胃I 20s
     */
    ANOREXIA("厌食", "频繁食用单一食物", List.of(
            new PotionEffect(PotionEffectType.NAUSEA, 400, 0) // 20s 反胃I
    )),

    /**
     * 饥饿状态
     * 条件：饱食度低于5%
     * BUFF：缓慢+10%，挖掘疲劳I
     */
    STARVING("饥饿", "饱食度低于5%", List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 600, 0), // 30s 缓慢I
            new PotionEffect(PotionEffectType.MINING_FATIGUE, 600, 0) // 30s 挖掘疲劳I
    )),

    /**
     * 食物中毒状态
     * 条件：服用生食（触发概率10%）
     * BUFF：反胃II 30s，缓慢掉血
     */
    FOOD_POISONING("食物中毒", "服用生食", List.of(
            new PotionEffect(PotionEffectType.NAUSEA, 600, 1), // 30s 反胃II
            new PotionEffect(PotionEffectType.POISON, 600, 0) // 30s 中毒I（缓慢掉血）
    )),

    /**
     * 营养不良状态
     * 条件：长期未食用肉类/鱼类食物（以游戏天3天判断）
     * BUFF：缓慢+5%，厌食（食物按原有值30%饱食度摄入）
     */
    MALNUTRITION("营养不良", "长期未食用肉类/鱼类食物", List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 1200, 0), // 60s 缓慢I
            new PotionEffect(PotionEffectType.WEAKNESS, 1200, 0) // 60s 虚弱I
    )),

    /**
     * 高温状态
     * 条件：玩家体温值在3min之内大于40°C
     * BUFF：反胃II 60s，缓慢掉血，如持续6min以上则持续掉血
     */
    HEAT_STROKE("高温", "体温值大于40°C", List.of(
            new PotionEffect(PotionEffectType.NAUSEA, 1200, 1), // 60s 反胃II
            new PotionEffect(PotionEffectType.POISON, 1200, 0) // 60s 中毒I
    )),

    /**
     * 严寒状态
     * 条件：玩家体温值在3min之内小于10°C
     * BUFF：缓慢+30%，虚弱I，如持续6min以上则持续掉血
     */
    HYPOTHERMIA("严寒", "体温值小于10°C", List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 1200, 1), // 60s 缓慢II
            new PotionEffect(PotionEffectType.WEAKNESS, 1200, 1) // 60s 虚弱II
    ));

    private final String displayName;
    private final String description;
    private final List<PotionEffect> effects;

    BodyStatus(String displayName, String description, List<PotionEffect> effects) {
        this.displayName = displayName;
        this.description = description;
        this.effects = effects;
    }
}
