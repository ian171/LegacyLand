package net.chen.legacyLand.player.status;

import lombok.Getter;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 受伤状态（战场状态）
 */
@Getter
public enum InjuryStatus {
    /**
     * 刀伤
     * 条件：玩家被刀/剑类砍到概率触发
     * CD：10s
     * BUFF：缓慢+10%，缓慢掉血
     */
    BLADE_WOUND("刀伤", "被刀/剑类砍到", 10, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 200, 0), // 10s 缓慢I
            new PotionEffect(PotionEffectType.POISON, 200, 0) // 10s 中毒I（缓慢掉血）
    )),

    /**
     * 斧伤
     * 条件：玩家被轻斧/重斧砍到概率触发
     * CD：10s
     * BUFF：缓慢+30%，一次性掉10%-15%生命值，10%触发骨折BUFF
     */
    AXE_WOUND("斧伤", "被轻斧/重斧砍到", 10, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 200, 1) // 10s 缓慢II
    )),

    /**
     * 箭伤
     * 条件：玩家被弓箭射到概率触发
     * CD：10s
     * BUFF：缓慢+5%，一次性掉5%-10%生命值
     */
    ARROW_WOUND("箭伤", "被弓箭射到", 10, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 200, 0) // 10s 缓慢I
    )),

    /**
     * 投具砸伤
     * 条件：玩家被投石机/实心炮弹砸到概率触发
     * CD：10s
     * BUFF：缓慢+35%，一次性掉10-20%生命值，15%触发骨折BUFF
     */
    PROJECTILE_WOUND("投具砸伤", "被投石机/实心炮弹砸到", 10, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 200, 2) // 10s 缓慢III
    )),

    /**
     * 火伤
     * 条件：玩家被火类武器烧伤到概率触发
     * CD：10s
     * BUFF：缓慢+10%，虚弱I
     */
    BURN("火伤", "被火类武器烧伤", 10, List.of(
            new PotionEffect(PotionEffectType.SLOWNESS, 200, 0), // 10s 缓慢I
            new PotionEffect(PotionEffectType.WEAKNESS, 200, 0) // 10s 虚弱I
    )),

    /**
     * 武器中毒
     * 条件：玩家被含有毒性武器所伤到概率触发
     * CD：10s
     * BUFF：反胃II 20s 持续掉血
     */
    POISON("武器中毒", "被含有毒性武器所伤", 10, List.of(
            new PotionEffect(PotionEffectType.NAUSEA, 400, 1), // 20s 反胃II
            new PotionEffect(PotionEffectType.POISON, 400, 1) // 20s 中毒II
    ));

    private final String displayName;
    private final String description;
    private final int cooldownSeconds;
    private final List<PotionEffect> effects;

    InjuryStatus(String displayName, String description, int cooldownSeconds, List<PotionEffect> effects) {
        this.displayName = displayName;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.effects = effects;
    }
}
