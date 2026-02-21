package net.chen.legacyLand.player.status;

import lombok.Data;
import net.chen.legacyLand.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.*;

/**
 * 玩家状态管理器
 */
@Data
public class PlayerStatusManager {
    private static PlayerStatusManager instance;

    // 玩家当前身体状态
    private final Map<UUID, BodyStatus> playerBodyStatus;

    // 玩家当前受伤状态（战场）
    private final Map<UUID, Set<InjuryStatus>> playerInjuryStatus;

    // 玩家当前受伤状态（生活）
    private final Map<UUID, Set<LifeInjuryStatus>> playerLifeInjuryStatus;

    // 状态冷却时间
    private final Map<UUID, Map<String, Long>> statusCooldowns;

    // 愉悦状态冷却（5分钟）
    private final Map<UUID, Long> joyfulCooldown;

    // 食物消费记录（用于厌食判断）
    private final Map<UUID, Map<String, Integer>> foodConsumption;

    // 最后食用肉类/鱼类时间（用于营养不良判断）
    private final Map<UUID, Long> lastMeatFishTime;

    private PlayerStatusManager() {
        this.playerBodyStatus = new HashMap<>();
        this.playerInjuryStatus = new HashMap<>();
        this.playerLifeInjuryStatus = new HashMap<>();
        this.statusCooldowns = new HashMap<>();
        this.joyfulCooldown = new HashMap<>();
        this.foodConsumption = new HashMap<>();
        this.lastMeatFishTime = new HashMap<>();
    }

    public static PlayerStatusManager getInstance() {
        if (instance == null) {
            instance = new PlayerStatusManager();
        }
        return instance;
    }

    /**
     * 检查并应用身体状态
     */
    public void checkAndApplyBodyStatus(Player player, PlayerData playerData) {
        UUID playerId = player.getUniqueId();
        double healthPercent = player.getHealth() / player.getMaxHealth();
        int foodLevel = player.getFoodLevel();
        double foodPercent = foodLevel / 20.0;

        BodyStatus newStatus = BodyStatus.NORMAL;

        // 检查愉悦状态
        if (healthPercent >= 1.0 && foodPercent >= 0.9) {
            Long lastJoyful = joyfulCooldown.get(playerId);
            if (lastJoyful == null || System.currentTimeMillis() - lastJoyful >= 300000) { // 5分钟CD
                newStatus = BodyStatus.JOYFUL;
                joyfulCooldown.put(playerId, System.currentTimeMillis());
            }
        }
        // 检查失落状态
        else if (healthPercent <= 0.2 && foodPercent <= 0.1) {
            newStatus = BodyStatus.DEPRESSED;
        }
        // 检查饥饿状态
        else if (foodPercent <= 0.05) {
            newStatus = BodyStatus.STARVING;
        }
        // 检查营养不良状态
        else if (isPlayerMalnourished(playerId)) {
            newStatus = BodyStatus.MALNUTRITION;
        }
        // 检查温度相关状态
        else if (playerData.getTemperature() > 40) {
            newStatus = BodyStatus.HEAT_STROKE;
        } else if (playerData.getTemperature() < 10) {
            newStatus = BodyStatus.HYPOTHERMIA;
        }

        // 应用状态
        if (newStatus != playerBodyStatus.get(playerId)) {
            applyBodyStatus(player, newStatus);
            playerBodyStatus.put(playerId, newStatus);
        }
    }

    /**
     * 应用身体状态效果
     */
    public void applyBodyStatus(Player player, BodyStatus status) {
        // 移除旧的状态效果
        // player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // 应用新的状态效果
        for (PotionEffect effect : status.getEffects()) {
            player.addPotionEffect(effect);
        }

        // 发送消息
        if (status != BodyStatus.NORMAL) {
            player.sendMessage("§e你进入了 §c" + status.getDisplayName() + " §e状态！");
        }
    }

    /**
     * 应用受伤状态（战场）
     */
    public void applyInjuryStatus(Player player, InjuryStatus status) {
        UUID playerId = player.getUniqueId();

        // 检查冷却
        if (isOnCooldown(playerId, status.name())) {
            return;
        }

        // 应用效果
        for (PotionEffect effect : status.getEffects()) {
            player.addPotionEffect(effect);
        }

        // 添加到当前状态
        playerInjuryStatus.computeIfAbsent(playerId, k -> new HashSet<>()).add(status);

        // 设置冷却
        setCooldown(playerId, status.name(), status.getCooldownSeconds() * 1000L);

        // 发送消息
        player.sendMessage("§c你受到了 " + status.getDisplayName() + "！");

        // 特殊处理：斧伤和投具砸伤有概率触发骨折
        if (status == InjuryStatus.AXE_WOUND && Math.random() < 0.1) {
            applyLifeInjuryStatus(player, LifeInjuryStatus.FRACTURE);
        } else if (status == InjuryStatus.PROJECTILE_WOUND && Math.random() < 0.15) {
            applyLifeInjuryStatus(player, LifeInjuryStatus.FRACTURE);
        }
    }

    /**
     * 应用受伤状态（生活）
     */
    public void applyLifeInjuryStatus(Player player, LifeInjuryStatus status) {
        UUID playerId = player.getUniqueId();

        // 检查冷却（骨折除外）
        if (status.getCooldownSeconds() != -1 && isOnCooldown(playerId, status.name())) {
            return;
        }

        // 应用效果
        for (PotionEffect effect : status.getEffects()) {
            player.addPotionEffect(effect);
        }

        // 添加到当前状态
        playerLifeInjuryStatus.computeIfAbsent(playerId, k -> new HashSet<>()).add(status);

        // 设置冷却（骨折除外）
        if (status.getCooldownSeconds() != -1) {
            setCooldown(playerId, status.name(), status.getCooldownSeconds() * 1000L);
        }

        // 发送消息
        player.sendMessage("§c你受到了 " + status.getDisplayName() + "！");
    }

    /**
     * 治疗骨折
     */
    public void healFracture(Player player) {
        UUID playerId = player.getUniqueId();
        Set<LifeInjuryStatus> statuses = playerLifeInjuryStatus.get(playerId);

        if (statuses != null && statuses.contains(LifeInjuryStatus.FRACTURE)) {
            statuses.remove(LifeInjuryStatus.FRACTURE);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
            player.sendMessage("§a你的骨折已经治愈！");
        }
    }

    /**
     * 记录食物消费
     */
    public void recordFoodConsumption(Player player, String foodType) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> consumption = foodConsumption.computeIfAbsent(playerId, k -> new HashMap<>());

        int count = consumption.getOrDefault(foodType, 0) + 1;
        consumption.put(foodType, count);

        // 检查是否触发厌食
        if (count >= 15) {
            applyBodyStatus(player, BodyStatus.ANOREXIA);
            consumption.put(foodType, 0); // 重置计数
        }

        // 如果是肉类/鱼类，更新时间
        if (isMeatOrFish(foodType)) {
            lastMeatFishTime.put(playerId, System.currentTimeMillis());
        }
    }

    /**
     * 检查是否营养不良
     */
    private boolean isPlayerMalnourished(UUID playerId) {
        Long lastTime = lastMeatFishTime.get(playerId);
        if (lastTime == null) {
            return false;
        }
        // 3个游戏天 = 3 * 20分钟 = 60分钟 = 3600000毫秒
        return System.currentTimeMillis() - lastTime >= 3600000;
    }

    /**
     * 判断是否是肉类/鱼类
     */
    private boolean isMeatOrFish(String foodType) {
        return foodType.contains("MEAT") || foodType.contains("FISH") ||
               foodType.contains("BEEF") || foodType.contains("PORK") ||
               foodType.contains("CHICKEN") || foodType.contains("MUTTON") ||
               foodType.contains("COD") || foodType.contains("SALMON");
    }

    /**
     * 检查冷却
     */
    private boolean isOnCooldown(UUID playerId, String statusName) {
        Map<String, Long> cooldowns = statusCooldowns.get(playerId);
        if (cooldowns == null) {
            return false;
        }
        Long cooldownEnd = cooldowns.get(statusName);
        if (cooldownEnd == null) {
            return false;
        }
        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * 设置冷却
     */
    private void setCooldown(UUID playerId, String statusName, long durationMillis) {
        Map<String, Long> cooldowns = statusCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        cooldowns.put(statusName, System.currentTimeMillis() + durationMillis);
    }

    /**
     * 清理玩家数据
     */
    public void clearPlayerData(UUID playerId) {
        playerBodyStatus.remove(playerId);
        playerInjuryStatus.remove(playerId);
        playerLifeInjuryStatus.remove(playerId);
        statusCooldowns.remove(playerId);
        joyfulCooldown.remove(playerId);
        foodConsumption.remove(playerId);
        lastMeatFishTime.remove(playerId);
    }
}
