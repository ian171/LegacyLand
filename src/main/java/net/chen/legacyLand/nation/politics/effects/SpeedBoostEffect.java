package net.chen.legacyLand.nation.politics.effects;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.nation.politics.PoliticalEffect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 速度加成效果 - 为国家所有在线成员提供速度效果
 */
public class SpeedBoostEffect implements PoliticalEffect {

    private final int amplifier; // 效果等级（0=速度I, 1=速度II）
    private final TownyAPI townyAPI;

    public SpeedBoostEffect(int amplifier) {
        this.amplifier = amplifier;
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public String getId() {
        return "speed-boost";
    }

    @Override
    public void onApply(Nation nation) {
        applySpeedToNation(nation);
    }

    @Override
    public void onRemove(Nation nation) {
        removeSpeedFromNation(nation);
    }

    @Override
    public String getDescription() {
        return "为所有国家成员提供速度 " + (amplifier + 1) + " 效果";
    }

    /**
     * 为国家所有在线成员添加速度效果
     */
    private void applySpeedToNation(Nation nation) {
        for (Resident resident : nation.getResidents()) {
            Player player = resident.getPlayer();
            if (player != null && player.isOnline()) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED,
                        Integer.MAX_VALUE, // 永久效果
                        amplifier,
                        false, // 不显示粒子
                        false  // 不显示图标
                ));
            }
        }
    }

    /**
     * 移除国家所有在线成员的速度效果
     */
    private void removeSpeedFromNation(Nation nation) {
        for (Resident resident : nation.getResidents()) {
            Player player = resident.getPlayer();
            if (player != null && player.isOnline()) {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }

    /**
     * 为新加入的玩家应用速度效果（可在玩家加入国家事件中调用）
     */
    public void applyToPlayer(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                Integer.MAX_VALUE,
                amplifier,
                false,
                false
        ));
    }

    /**
     * 移除玩家的速度效果（可在玩家离开国家事件中调用）
     */
    public void removeFromPlayer(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
    }
}
