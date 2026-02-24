package net.chen.legacyLand.war.flagwar;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.*;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * FlagWar 管理器
 * 管理所有旗帜战争实例
 */
public class FlagWarManager {
    private static FlagWarManager instance;
    private final Map<String, FlagWarData> activeFlagWars;
    private final Map<Location, FlagWarData> flagLocationMap;
    private final Map<UUID, FlagWarData> playerFlagWarMap;

    private FlagWarManager() {
        this.activeFlagWars = new HashMap<>();
        this.flagLocationMap = new HashMap<>();
        this.playerFlagWarMap = new HashMap<>();
    }

    public static FlagWarManager getInstance() {
        if (instance == null) {
            instance = new FlagWarManager();
        }
        return instance;
    }

    /**
     * 创建新的 FlagWar
     */
    public FlagWarData createFlagWar(Player attacker, Location flagLocation, TownBlock targetBlock) {
        try {
            Town defenderTown = targetBlock.getTownOrNull();
            if (defenderTown == null) {
                return null;
            }

            Resident attackerResident = TownyAPI.getInstance().getResident(attacker);
            if (attackerResident == null) {
                return null;
            }

            Nation attackerNation = TownyAPI.getInstance().getResidentNationOrNull(attackerResident);
            Nation defenderNation = defenderTown.getNationOrNull();

            if (attackerNation == null || defenderNation == null) {
                return null;
            }

            Town attackerTown = TownyAPI.getInstance().getResidentTownOrNull(attackerResident);
            if (attackerTown == null) {
                return null;
            }

            String flagWarId = UUID.randomUUID().toString();
            FlagWarData flagWar = new FlagWarData(
                flagWarId,
                attacker.getUniqueId(),
                attackerNation.getName(),
                attackerTown.getName(),
                defenderNation.getName(),
                defenderTown.getName(),
                flagLocation
            );

            // 设置地块信息
            flagWar.setTownBlockCoords(targetBlock.getX() + "," + targetBlock.getZ());
            flagWar.setHomeBlock(targetBlock.isHomeBlock());

            // 计算经济成本
            calculateEconomicCosts(flagWar);

            // 放置旗帜和计时器方块
            placeFlagWarBlocks(flagWar);

            // 保存到内存
            activeFlagWars.put(flagWarId, flagWar);
            flagLocationMap.put(flagLocation, flagWar);
            playerFlagWarMap.put(attacker.getUniqueId(), flagWar);

            // 保存到数据库
            LegacyLand.getInstance().getDatabaseManager().saveFlagWar(flagWar);

            // 全局广播
            broadcastFlagWarStart(flagWar);

            LegacyLand.logger.info("FlagWar 已创建: " + flagWarId);
            return flagWar;

        } catch (Exception e) {
            LegacyLand.logger.severe("创建 FlagWar 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 放置旗帜战争方块
     */
    private void placeFlagWarBlocks(FlagWarData flagWar) {
        Location flagLoc = flagWar.getFlagLocation();
        Location timerLoc = flagWar.getTimerBlockLocation();
        Location beaconLoc = flagWar.getBeaconLocation();

        // 放置旗帜（橡木栅栏）
        flagLoc.getBlock().setType(Material.OAK_FENCE);

        // 放置计时器方块（初始为绿色羊毛）
        timerLoc.getBlock().setType(Material.LIME_WOOL);

        // 放置光源（火把）
        Location lightLoc = timerLoc.clone().add(0, 1, 0);
        lightLoc.getBlock().setType(Material.TORCH);

        // 放置信标（天空中的标记）
        beaconLoc.getBlock().setType(Material.GLOWSTONE);
    }

    /**
     * 计算经济成本
     */
    private void calculateEconomicCosts(FlagWarData flagWar) {
        // 从配置读取成本
        double stakingFee = LegacyLand.getInstance().getConfig().getDouble("flagwar.costs.staking-fee", 100.0);
        double defenseBreakFee = LegacyLand.getInstance().getConfig().getDouble("flagwar.costs.defense-break-fee", 200.0);
        double victoryCost = LegacyLand.getInstance().getConfig().getDouble("flagwar.costs.victory-cost", 500.0);

        // 主城方块成本更高
        if (flagWar.isHomeBlock()) {
            stakingFee *= 2;
            defenseBreakFee *= 2;
            victoryCost *= 2;
        }

        flagWar.setStakingFee(stakingFee);
        flagWar.setDefenseBreakFee(defenseBreakFee);
        flagWar.setVictoryCost(victoryCost);
    }

    /**
     * 广播 FlagWar 开始
     */
    private void broadcastFlagWarStart(FlagWarData flagWar) {
        String message = String.format(
            "§c[战争] §e%s 的 %s 正在攻击 %s 的 %s！坐标: %s",
            flagWar.getAttackerNation(),
            flagWar.getAttackerTown(),
            flagWar.getDefenderNation(),
            flagWar.getDefenderTown(),
            flagWar.getTownBlockCoords()
        );
        Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(message));
    }

    /**
     * 更新计时器进度
     */
    public void updateTimerProgress(FlagWarData flagWar, int progress) {
        flagWar.setTimerProgress(progress);

        // 更新羊毛颜色
        Material woolColor = flagWar.getTimerWoolColor();
        flagWar.getTimerBlockLocation().getBlock().setType(woolColor);

        // 检查是否完成
        if (flagWar.isCompleted()) {
            handleAttackerVictory(flagWar);
        }
    }

    /**
     * 处理攻击方胜利
     */
    private void handleAttackerVictory(FlagWarData flagWar) {
        flagWar.end(FlagWarStatus.ATTACKER_VICTORY);

        // 转移地块所有权
        transferTownBlock(flagWar);

        // 清理方块
        removeFlagWarBlocks(flagWar);

        // 广播结果
        String message = String.format(
            "§c[战争] §a%s 成功占领了 %s 的地块！",
            flagWar.getAttackerTown(),
            flagWar.getDefenderTown()
        );
        Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(message));

        // 从内存移除
        removeFlagWar(flagWar);

        // 更新数据库
        LegacyLand.getInstance().getDatabaseManager().saveFlagWar(flagWar);
    }

    /**
     * 处理防守方胜利（旗帜被破坏）
     */
    public void handleDefenderVictory(FlagWarData flagWar) {
        flagWar.end(FlagWarStatus.DEFENDER_VICTORY);

        // 清理方块
        removeFlagWarBlocks(flagWar);

        // 扣除攻击方费用
        Player attacker = Bukkit.getPlayer(flagWar.getAttackerId());
        if (attacker != null) {
            attacker.sendMessage("§c你的攻击失败了！需支付 " + flagWar.getDefenseBreakFee() + " 金币给防守方。");
        }

        // 广播结果
        String message = String.format(
            "§c[战争] §a%s 成功防守了地块！",
            flagWar.getDefenderTown()
        );
        Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(message));

        // 从内存移除
        removeFlagWar(flagWar);

        // 更新数据库
        LegacyLand.getInstance().getDatabaseManager().saveFlagWar(flagWar);
    }

    /**
     * 转移地块所有权
     */
    private void transferTownBlock(FlagWarData flagWar) {
        try {
            String[] coords = flagWar.getTownBlockCoords().split(",");
            int x = Integer.parseInt(coords[0]);
            int z = Integer.parseInt(coords[1]);

            WorldCoord worldCoord = new WorldCoord(flagWar.getFlagLocation().getWorld().getName(), x, z);
            TownBlock block = TownyAPI.getInstance().getTownBlock(worldCoord);
            if (block != null) {
                Town attackerTown = TownyAPI.getInstance().getTown(flagWar.getAttackerTown());
                if (attackerTown != null) {
                    block.setTown(attackerTown);
                    LegacyLand.logger.info("地块已转移: " + flagWar.getTownBlockCoords());
                }
            }
        } catch (Exception e) {
            LegacyLand.logger.severe("转移地块失败: " + e.getMessage());
        }
    }

    /**
     * 清理 FlagWar 方块
     */
    private void removeFlagWarBlocks(FlagWarData flagWar) {
        flagWar.getFlagLocation().getBlock().setType(Material.AIR);
        flagWar.getTimerBlockLocation().getBlock().setType(Material.AIR);
        flagWar.getTimerBlockLocation().clone().add(0, 1, 0).getBlock().setType(Material.AIR);
        flagWar.getBeaconLocation().getBlock().setType(Material.AIR);
    }

    /**
     * 从内存移除 FlagWar
     */
    private void removeFlagWar(FlagWarData flagWar) {
        activeFlagWars.remove(flagWar.getFlagWarId());
        flagLocationMap.remove(flagWar.getFlagLocation());
        playerFlagWarMap.remove(flagWar.getAttackerId());
    }

    /**
     * 获取指定位置的 FlagWar
     */
    public FlagWarData getFlagWarAtLocation(Location location) {
        return flagLocationMap.get(location);
    }

    /**
     * 获取玩家的 FlagWar
     */
    public FlagWarData getPlayerFlagWar(UUID playerId) {
        return playerFlagWarMap.get(playerId);
    }

    /**
     * 获取所有活跃的 FlagWar
     */
    public Collection<FlagWarData> getActiveFlagWars() {
        return Collections.unmodifiableCollection(activeFlagWars.values());
    }

    /**
     * 取消 FlagWar
     */
    public void cancelFlagWar(FlagWarData flagWar) {
        flagWar.end(FlagWarStatus.CANCELLED);
        removeFlagWarBlocks(flagWar);
        removeFlagWar(flagWar);
        LegacyLand.getInstance().getDatabaseManager().saveFlagWar(flagWar);
    }

    /**
     * 从数据库加载活跃的 FlagWar
     */
    public void loadFromDatabase(DatabaseManager databaseManager) {
        List<FlagWarData> flagWars = databaseManager.loadActiveFlagWars();
        for (FlagWarData flagWar : flagWars) {
            activeFlagWars.put(flagWar.getFlagWarId(), flagWar);
            flagLocationMap.put(flagWar.getFlagLocation(), flagWar);
            playerFlagWarMap.put(flagWar.getAttackerId(), flagWar);
        }
        LegacyLand.logger.info("已从数据库加载 " + flagWars.size() + " 个活跃的 FlagWar");
    }
}
