package net.chen.legacyLand.player.status;

import lombok.Data;
import net.chen.legacyLand.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 温度系统管理器
 */
@Data
public class TemperatureManager {
    private static TemperatureManager instance;

    // 玩家当前温度
    private final Map<UUID, Double> playerTemperature;

    // 季节基础温度
    private double seasonBaseTemperature = 22.0; // 默认春季

    // 温度变化速率（每次更新最多变化的度数）
    private double temperatureChangeRate = 4.5; // 默认每5秒最多变化4.5

    private TemperatureManager() {
        this.playerTemperature = new ConcurrentHashMap<>();
    }

    public static TemperatureManager getInstance() {
        if (instance == null) {
            instance = new TemperatureManager();
        }
        return instance;
    }

    /**
     * 更新玩家温度（渐进式变化）
     */
    public void updatePlayerTemperature(Player player, PlayerData playerData) {
        UUID playerId = player.getUniqueId();

        // 计算目标温度和温度影响强度
        TemperatureEffect effect = calculateTemperatureEffect(player);
        double targetTemperature = effect.targetTemperature;

        // 获取当前温度
        double currentTemperature = playerTemperature.getOrDefault(playerId, seasonBaseTemperature);

        // 检查是否处于极端环境（岩浆、紧贴热源/冷源）
        boolean isExtremeHeat = checkExtremeHeat(player);
        boolean isExtremeCold = checkExtremeCold(player);

        // 根据温度影响强度和极端环境动态调整变化率
        double dynamicChangeRate = calculateDynamicChangeRate(effect, isExtremeHeat, isExtremeCold);

        // 计算新温度
        double newTemperature = getNewTemperature(targetTemperature, currentTemperature, dynamicChangeRate);

        // 更新玩家温度
        playerTemperature.put(playerId, newTemperature);
        playerData.setTemperature(newTemperature);

        // 检查温度状态
        checkTemperatureStatus(player, playerData, newTemperature);
    }

    /**
     * 检查是否处于极端高温环境
     */
    private boolean checkExtremeHeat(Player player) {
        // 检查是否在岩浆中
        if (player.isInLava()) {
            return true;
        }

        // 检查是否紧贴热源（1格以内）
        Block block = player.getLocation().getBlock();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearby = block.getRelative(x, y, z);
                    if (isFireSource(nearby.getType()) || nearby.getType() == Material.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查是否处于极端低温环境
     */
    private boolean checkExtremeCold(Player player) {
        // 检查是否在粉雪中
        Block playerBlock = player.getLocation().getBlock();
        if (playerBlock.getType() == Material.POWDER_SNOW) {
            return true;
        }

        // 检查是否紧贴冷源（1格以内）
        Block block = player.getLocation().getBlock();
        int coldSourceCount = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearby = block.getRelative(x, y, z);
                    if (nearby.getType() == Material.ICE ||
                        nearby.getType() == Material.PACKED_ICE ||
                        nearby.getType() == Material.POWDER_SNOW|| nearby.getType() == Material.WATER)  {
                        coldSourceCount++;
                    }
                }
            }
        }
        // 被多个冰块包围视为极端寒冷
        return coldSourceCount >= 3;
    }

    /**
     * 根据温度影响强度计算动态变化率
     */
    private double calculateDynamicChangeRate(TemperatureEffect effect, boolean isExtremeHeat, boolean isExtremeCold) {
        // 基础变化率
        double baseRate = temperatureChangeRate;

        // 极端环境：10倍速度
        if (isExtremeHeat || isExtremeCold) {
            return baseRate * 10.0;
        }

        // 计算总影响强度（热值 - 冷值的绝对值）
        double totalInfluence = Math.abs(effect.heatValue - effect.coldValue);

        // 根据影响强度调整变化率
        // 影响越强，变化越快
        if (totalInfluence > 10.0) {
            return baseRate * 2.0; // 极强影响：2倍速度
        } else if (totalInfluence > 5.0) {
            return baseRate * 1.5; // 强影响：1.5倍速度
        } else if (totalInfluence > 2.0) {
            return baseRate * 1.2; // 中等影响：1.2倍速度
        } else {
            return baseRate; // 弱影响：正常速度
        }
    }

    private double getNewTemperature(double targetTemperature, double currentTemperature, double changeRate) {
        double temperatureDiff = targetTemperature - currentTemperature;

        // 渐进式调整温度
        double newTemperature;
        if (Math.abs(temperatureDiff) <= changeRate) {
            // 温度差小于变化速率，直接到达目标温度
            newTemperature = targetTemperature;
        } else {
            // 温度差大于变化速率，按速率逐渐变化
            if (temperatureDiff > 0) {
                // 升温：正常速度
                newTemperature = currentTemperature + changeRate;
            } else {
                // 降温：3.5倍速度（降温比升温快）
                newTemperature = currentTemperature - (changeRate * 3.5);
            }
        }
        return newTemperature;
    }

    /**
     * 计算温度效果（包括热值、冷值和目标温度）
     */
    private TemperatureEffect calculateTemperatureEffect(Player player) {
        TemperatureEffect effect = new TemperatureEffect();
        effect.targetTemperature = seasonBaseTemperature;

        // 装备温度影响
        double armorTemp = getArmorTemperature(player);
        if (armorTemp > 0) {
            effect.heatValue += armorTemp;
        } else {
            effect.coldValue += Math.abs(armorTemp);
        }
        effect.targetTemperature += armorTemp;

        // 环境温度影响
        EnvironmentTemperature envTemp = getEnvironmentTemperatureDetailed(player);
        effect.heatValue += envTemp.heatValue;
        effect.coldValue += envTemp.coldValue;
        effect.targetTemperature += (envTemp.heatValue - envTemp.coldValue);

        // 手持物品温度影响
        double heldTemp = getHeldItemTemperature(player);
        if (heldTemp > 0) {
            effect.heatValue += heldTemp;
        } else {
            effect.coldValue += Math.abs(heldTemp);
        }
        effect.targetTemperature += heldTemp;

        return effect;
    }

    /**
     * 温度效果数据类
     */
    private static class TemperatureEffect {
        double targetTemperature = 0.0;
        double heatValue = 0.0; // 热值（升温因素）
        double coldValue = 0.0; // 冷值（降温因素）
    }

    /**
     * 环境温度详细数据类
     */
    private static class EnvironmentTemperature {
        double heatValue = 0.0;
        double coldValue = 0.0;
    }

    /**
     * 获取装备温度影响
     */
    private double getArmorTemperature(Player player) {
        double temp = 0.0;
        boolean isSummer = seasonBaseTemperature >= 27.0;

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        // 皮革盔甲
        if (isLeatherArmor(helmet) || isLeatherArmor(chestplate) ||
            isLeatherArmor(leggings) || isLeatherArmor(boots)) {
            temp += isSummer ? 5.0 : 3.0;
        }

        // 锁链盔甲
        if (isChainmailArmor(helmet) || isChainmailArmor(chestplate) ||
            isChainmailArmor(leggings) || isChainmailArmor(boots)) {
            temp += isSummer ? 2.0 : 1.0;
        }

        // 铁盔甲
        if (isIronArmor(helmet) || isIronArmor(chestplate) ||
            isIronArmor(leggings) || isIronArmor(boots)) {
            temp += isSummer ? 10.0 : 4.0;
        }

        return temp;
    }

    /**
     * 获取环境温度影响（详细版本，返回热值和冷值）
     */
    private EnvironmentTemperature getEnvironmentTemperatureDetailed(Player player) {
        EnvironmentTemperature result = new EnvironmentTemperature();

        // 检查是否在岩浆中（极端高温）
        if (player.isInLava()) {
            result.heatValue = 50.0; // 岩浆造成极高温度
            return result;
        }
        if (isWeatherRainy(player)){
            result.coldValue = 5.0;
        }

        if (isInColdBiome(player)){
            result.coldValue = 9.5;
        }
        if (isInHotBiome(player)){
            result.heatValue = 9.5;
        }

        Block block = player.getLocation().getBlock();

        // 先用1格快速检查是否有热/冷源，无则跳过完整扫描
        boolean hasNearbySources = false;
        outer:
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Material m = block.getRelative(x, y, z).getType();
                    if (isFireSource(m) || m == Material.LAVA ||
                        m == Material.SNOW || m == Material.ICE ||
                        m == Material.PACKED_ICE || m == Material.POWDER_SNOW) {
                        hasNearbySources = true;
                        break outer;
                    }
                }
            }
        }

        if (!hasNearbySources) {
            return result;
        }

        // 统计附近的热源和冷源数量及距离
        int fireSourceCount = 0;
        int coldSourceCount = 0;
        double totalHeatInfluence = 0.0;
        double totalColdInfluence = 0.0;

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block nearby = block.getRelative(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    // 检查火源和岩浆
                    if (isFireSource(nearby.getType()) || nearby.getType() == Material.LAVA) {
                        fireSourceCount++;
                        // 距离越近影响越大，岩浆影响更强
                        double baseInfluence = nearby.getType() == Material.LAVA ? 8.0 : 4.0;
                        double influence = baseInfluence * (1.0 - distance / 5.0);
                        totalHeatInfluence += Math.max(influence, 0.5);
                    }

                    // 检查冷源
                    if (nearby.getType() == Material.SNOW ||
                        nearby.getType() == Material.ICE ||
                        nearby.getType() == Material.PACKED_ICE ||
                        nearby.getType() == Material.POWDER_SNOW) {
                        coldSourceCount++;
                        // 距离越近影响越大
                        double influence = 4.0 * (1.0 - distance / 5.0);
                        totalColdInfluence += Math.max(influence, 0.5);
                    }
                }
            }
        }

        // 热源影响：多个热源有叠加效果，但有上限
        if (fireSourceCount > 0) {
            result.heatValue = Math.min(totalHeatInfluence, 30.0); // 最多+30°C
        }

        // 冷源影响：多个冷源有叠加效果，但有上限
        if (coldSourceCount > 0) {
            result.coldValue = Math.min(totalColdInfluence, 15.0); // 最多-15°C
        }

        // 检查是否在水中（强冷却效果）
        if (player.isInWater()) {
            result.coldValue += 3.0;
        }

        return result;
    }

    /**
     * 获取手持物品温度影响
     */
    private double getHeldItemTemperature(Player player) {
        double temp = 0.0;
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (mainHand.getType() == Material.WATER_BUCKET) {
            temp -= 2.0;
        } else if (mainHand.getType() == Material.LAVA_BUCKET) {
            temp += 5.0;
        }

        return temp;
    }

    /**
     * 检查温度状态
     */
    private void checkTemperatureStatus(Player player, PlayerData playerData, double temperature) {
        PlayerStatusManager statusManager = PlayerStatusManager.getInstance();

        if (temperature > 40) {
            statusManager.checkAndApplyBodyStatus(player, playerData);
        } else if (temperature < 10) {
            statusManager.checkAndApplyBodyStatus(player, playerData);
        }
    }

    /**
     * 玩家退出时清理温度数据
     */
    public void removePlayer(UUID playerId) {
        playerTemperature.remove(playerId);
    }

    /**
     * 设置季节基础温度
     */
    public void setSeasonBaseTemperature(String season) {
        switch (season.toLowerCase()) {
            case "spring", "early_spring", "mid_spring", "late_spring", "spring_equinox" -> seasonBaseTemperature = 22.0;
            case "summer", "early_summer", "mid_summer", "late_summer", "summer_solstice" -> seasonBaseTemperature = 30.0;
            case "autumn", "early_autumn", "mid_autumn", "late_autumn", "autumn_equinox" -> seasonBaseTemperature = 21.0;
            case "winter", "early_winter", "mid_winter", "late_winter", "winter_solstice" -> seasonBaseTemperature = 15.0;
            default -> seasonBaseTemperature = 22.0;
        }
    }

    /**
     * 获取温度显示颜色
     */
    public String getTemperatureColor(double temperature) {
        if (temperature <= 0) {
            return "§1"; // 深蓝色（重度寒冷）
        } else if (temperature <= 15) {
            return "§9"; // 浅蓝色（轻度寒冷）
        } else if (temperature <= 27) {
            return "§a"; // 绿色（正常温度）
        } else if (temperature <= 35) {
            return "§c"; // 淡红色（轻度炎热）
        } else {
            return "§4"; // 红色（重度炎热）
        }
    }

    /**
     * 获取温度描述
     */
    public String getTemperatureDescription(double temperature) {
        if (temperature <= 0) {
            return "重度寒冷";
        } else if (temperature <= 15) {
            return "轻度寒冷";
        } else if (temperature <= 27) {
            return "正常温度";
        } else if (temperature <= 35) {
            return "轻度炎热";
        } else {
            return "重度炎热";
        }
    }

    // 辅助方法
    private boolean isLeatherArmor(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE ||
               type == Material.LEATHER_LEGGINGS || type == Material.LEATHER_BOOTS;
    }

    private boolean isChainmailArmor(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.CHAINMAIL_HELMET || type == Material.CHAINMAIL_CHESTPLATE ||
               type == Material.CHAINMAIL_LEGGINGS || type == Material.CHAINMAIL_BOOTS;
    }

    private boolean isIronArmor(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.IRON_HELMET || type == Material.IRON_CHESTPLATE ||
               type == Material.IRON_LEGGINGS || type == Material.IRON_BOOTS;
    }

    private boolean isFireSource(Material material) {
        return material == Material.FIRE || material == Material.LAVA ||
               material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE ||
               material == Material.TORCH || material == Material.SOUL_TORCH;
    }

    /**
     * 清理玩家数据
     */
    public void clearPlayerData(UUID playerId) {
        playerTemperature.remove(playerId);
    }

    private boolean isWeatherRainy(Player player) {
        World world = player.getWorld();
        return world.getEnvironment() == World.Environment.NORMAL && world.hasStorm();
    }
    private boolean isInColdBiome(Player player) {
        World world = player.getWorld();
        Biome biome = world.getBiome(player.getLocation());
        return biome == Biome.FROZEN_OCEAN || biome == Biome.FROZEN_RIVER ||
               biome == Biome.COLD_OCEAN || biome == Biome.DEEP_COLD_OCEAN ||
               biome == Biome.ICE_SPIKES;
    }
    private boolean isInHotBiome(Player player) {
        World world = player.getWorld();
        Biome biome = world.getBiome(player.getLocation());
        return biome == Biome.WARM_OCEAN || biome == Biome.LUKEWARM_OCEAN ||
                biome == Biome.DEEP_LUKEWARM_OCEAN;
    }
}
