package net.chen.legacyLand.player.status;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/**
 * 医疗物品处理器
 */
public class MedicalItemHandler {

    private final PlayerStatusManager statusManager;
    private final PlayerManager playerManager;

    public MedicalItemHandler() {
        this.statusManager = PlayerStatusManager.getInstance();
        this.playerManager = PlayerManager.getInstance();
    }

    /**
     * 使用医疗物品
     */
    public boolean useMedicalItem(Player player, MedicalItemType itemType) {
        boolean success = false;

        switch (itemType) {
            case BANDAGE -> success = useBandage(player);
            case HERBAL_MEDICINE -> success = useHerbalMedicine(player);
            case SPLINT -> success = useSplint(player);
            case ANTIDOTE -> success = useAntidote(player);
            case HEMOSTATIC_MEDICINE -> success = useHemostaticMedicine(player);
            case PANACEA -> success = usePanacea(player);
        }

        if (success) {
            // 恢复生命值
            double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + itemType.getHealAmount());
            player.setHealth(newHealth);

            player.sendMessage("§a你使用了 " + itemType.getDisplayName() + "！");
            return true;
        } else {
            player.sendMessage("§c你当前没有需要使用 " + itemType.getDisplayName() + " 治疗的伤势！");
            return false;
        }
    }

    /**
     * 使用绷带 - 治疗刀伤、箭伤
     */
    private boolean useBandage(Player player) {
        Set<InjuryStatus> injuries = statusManager.getPlayerInjuryStatus().get(player.getUniqueId());
        if (injuries == null) return false;

        boolean healed = false;
        if (injuries.remove(InjuryStatus.BLADE_WOUND)) {
            player.sendMessage("§a刀伤已被治愈！");
            healed = true;
        }
        if (injuries.remove(InjuryStatus.ARROW_WOUND)) {
            player.sendMessage("§a箭伤已被治愈！");
            healed = true;
        }

        if (healed) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }

        return healed;
    }

    /**
     * 使用草药包 - 治疗砸伤、水肿、火伤
     */
    private boolean useHerbalMedicine(Player player) {
        Set<LifeInjuryStatus> lifeInjuries = statusManager.getPlayerLifeInjuryStatus().get(player.getUniqueId());
        Set<InjuryStatus> injuries = statusManager.getPlayerInjuryStatus().get(player.getUniqueId());

        boolean healed = false;

        if (lifeInjuries != null) {
            if (lifeInjuries.remove(LifeInjuryStatus.WORK_INJURY)) {
                player.sendMessage("§a砸伤已被治愈！");
                healed = true;
            }
            if (lifeInjuries.remove(LifeInjuryStatus.EDEMA)) {
                player.sendMessage("§a水肿已被治愈！");
                healed = true;
            }
        }

        if (injuries != null && injuries.remove(InjuryStatus.BURN)) {
            player.sendMessage("§a火伤已被治愈！");
            healed = true;
        }

        if (healed) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }

        return healed;
    }

    /**
     * 使用夹板 - 治疗骨折
     */
    private boolean useSplint(Player player) {
        Set<LifeInjuryStatus> lifeInjuries = statusManager.getPlayerLifeInjuryStatus().get(player.getUniqueId());
        if (lifeInjuries == null) return false;

        if (lifeInjuries.remove(LifeInjuryStatus.FRACTURE)) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.sendMessage("§a骨折已被治愈！");
            return true;
        }

        return false;
    }

    /**
     * 使用解毒剂 - 治疗中毒
     */
    private boolean useAntidote(Player player) {
        Set<InjuryStatus> injuries = statusManager.getPlayerInjuryStatus().get(player.getUniqueId());
        boolean healed = false;

        if (injuries != null && injuries.remove(InjuryStatus.POISON)) {
            player.sendMessage("§a武器中毒已被治愈！");
            healed = true;
        }

        // 检查食物中毒
        if (statusManager.getPlayerBodyStatus().get(player.getUniqueId()) == BodyStatus.FOOD_POISONING) {
            statusManager.getPlayerBodyStatus().put(player.getUniqueId(), BodyStatus.NORMAL);
            player.sendMessage("§a食物中毒已被治愈！");
            healed = true;
        }

        if (healed) {
            player.removePotionEffect(PotionEffectType.POISON);
            player.removePotionEffect(PotionEffectType.NAUSEA);
        }

        return healed;
    }

    /**
     * 使用止血药 - 治疗流血
     */
    private boolean useHemostaticMedicine(Player player) {
        Set<InjuryStatus> injuries = statusManager.getPlayerInjuryStatus().get(player.getUniqueId());
        if (injuries == null) return false;

        boolean healed = false;
        if (injuries.remove(InjuryStatus.BLADE_WOUND)) {
            player.sendMessage("§a刀伤流血已止住！");
            healed = true;
        }
        if (injuries.remove(InjuryStatus.AXE_WOUND)) {
            player.sendMessage("§a斧伤流血已止住！");
            healed = true;
        }

        if (healed) {
            player.removePotionEffect(PotionEffectType.POISON);
        }

        return healed;
    }

    /**
     * 使用万能药 - 治疗所有伤势
     */
    private boolean usePanacea(Player player) {
        // 清除所有受伤状态
        statusManager.getPlayerInjuryStatus().remove(player.getUniqueId());
        statusManager.getPlayerLifeInjuryStatus().remove(player.getUniqueId());
        statusManager.getPlayerBodyStatus().put(player.getUniqueId(), BodyStatus.NORMAL);

        // 清除所有负面效果
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        // 恢复生命值和饱食度
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        // 恢复饮水值
        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        if (playerData != null) {
            playerData.setHydration(10);
        }

        player.sendMessage("§a§l所有伤势和状态已被完全治愈！");
        return true;
    }

    /**
     * 使用水壶 - 恢复饮水值
     */
    public boolean useWaterCanteen(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        if (playerData == null) return false;

        if (playerData.getHydration() >= 10) {
            player.sendMessage("§c你的饮水值已满！");
            return false;
        }

        playerData.restoreHydration(3);
        player.sendMessage("§a你喝了一口水，恢复了 3 点饮水值！");
        player.sendMessage("§e当前饮水值: " + playerData.getHydration() + "/10");
        return true;
    }
}
