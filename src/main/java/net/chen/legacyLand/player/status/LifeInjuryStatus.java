package net.chen.legacyLand.player.status;

import lombok.Getter;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 受伤状态（生活）
 */
@Getter
public enum LifeInjuryStatus {
    /**
     * 骨折
     * 条件：玩家从高处跌落触发（战争部分BUFF也指向此类）
     * CD：条件存在（得到治疗解除此类BUFF）
     * BUFF：缓慢+10%
     */
    FRACTURE("骨折", "从高处跌落", -1, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0) // 持续缓慢I
    )),

    /**
     * 砸伤
     * 条件：玩家在挖矿，砍树等概率砸伤（几率设置为1%）
     * CD：15s
     * BUFF：缓慢+10%，挖掘疲劳I
     */
    WORK_INJURY("砸伤", "挖矿、砍树等工作", 15, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 300, 0), // 15s 缓慢I
            new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 0) // 15s 挖掘疲劳I
    )),

    /**
     * 水肿
     * 条件：玩家长期在水田/捕捞等工作概率触发（几率设置为1%）
     * CD：15s
     * BUFF：缓慢+10%，挖掘疲劳I
     */
    EDEMA("水肿", "长期在水田/捕捞等工作", 15, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 300, 0), // 15s 缓慢I
            new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 0) // 15s 挖掘疲劳I
    ));

    private final String displayName;
    private final String description;
    private final int cooldownSeconds; // -1 表示需要治疗才能解除
    private final List<PotionEffect> effects;

    LifeInjuryStatus(String displayName, String description, int cooldownSeconds, List<PotionEffect> effects) {
        this.displayName = displayName;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.effects = effects;
    }
}
