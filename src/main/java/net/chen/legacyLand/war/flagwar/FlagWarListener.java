package net.chen.legacyLand.war.flagwar;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.*;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * FlagWar 监听器
 * 监听旗帜放置和破坏事件
 */
public class FlagWarListener implements Listener {

    private final FlagWarManager flagWarManager;

    public FlagWarListener() {
        this.flagWarManager = FlagWarManager.getInstance();
    }

    /**
     * 监听橡木栅栏放置 - 开始 FlagWar
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlagPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        // 检查是否是橡木栅栏
        if (material != Material.OAK_FENCE) {
            return;
        }

        Location location = event.getBlock().getLocation();

        // 检查玩家是否已经有进行中的 FlagWar
        if (flagWarManager.getPlayerFlagWar(player.getUniqueId()) != null) {
            player.sendMessage("§c你已经有一个进行中的旗帜战争！");
            event.setCancelled(true);
            return;
        }

        // 检查是否是 OP
//        if (player.isOp()) {
//            player.sendMessage("§c服务器管理员无法发起旗帜战争！");
//            event.setCancelled(true);
//            return;
//        }

        // 获取玩家所在国家
        Resident attackerResident = TownyAPI.getInstance().getResident(player);
        if (attackerResident == null) {
            player.sendMessage("§c你必须加入一个国家才能发起旗帜战争！");
            event.setCancelled(true);
            return;
        }

        Nation attackerNation = TownyAPI.getInstance().getResidentNationOrNull(attackerResident);
        if (attackerNation == null) {
            player.sendMessage("§c你必须加入一个国家才能发起旗帜战争！");
            event.setCancelled(true);
            return;
        }

        // 检查国家是否是中立/和平状态
        if (attackerNation.isNeutral()) {
            attackerNation.setNeutral(false);
            player.sendMessage("你已放弃中立国家身份，再次放置宣战");
            event.setCancelled(true);
            return;
        }

        // 获取目标地块
        WorldCoord coord = WorldCoord.parseWorldCoord(location);
        TownBlock targetBlock = TownyAPI.getInstance().getTownBlock(coord);

        if (targetBlock == null) {
            return; // 不是城镇地块，允许正常放置
        }

        Town defenderTown = targetBlock.getTownOrNull();
        if (defenderTown == null) {
            return;
        }

        Nation defenderNation = defenderTown.getNationOrNull();
        if (defenderNation == null) {
            player.sendMessage("§c目标城镇不属于任何国家！");
            event.setCancelled(true);
            return;
        }

        // 检查是否是敌对国家
        if (!attackerNation.hasEnemy(defenderNation)) {
            player.sendMessage("§c你只能攻击敌对国家的地块！使用 /nation enemy add <国家名> 宣战。");
            event.setCancelled(true);
            return;
        }

        // 检查目标国家是否是中立/和平状态
        if (defenderNation.isNeutral()) {
            defenderNation.setNeutral(false);
            player.sendMessage("你已放弃中立国家身份");
        }

        // 检查是否只能攻击边界地块
        boolean onlyBorder = LegacyLand.getInstance().getConfig().getBoolean("flagwar.only-border-plots", true);
        if (onlyBorder && !isBorderPlot(targetBlock)) {
            player.sendMessage("§c你只能攻击边界地块！");
            event.setCancelled(true);
            return;
        }

        // 检查在线人数要求
        int minAttackerOnline = LegacyLand.getInstance().getConfig().getInt("flagwar.min-online-attackers", 1);
        int minDefenderOnline = LegacyLand.getInstance().getConfig().getInt("flagwar.min-online-defenders", 1);

        int attackerOnline = getOnlinePlayersInNation(attackerNation);
        int defenderOnline = getOnlinePlayersInNation(defenderNation);

        if (attackerOnline < minAttackerOnline) {
            player.sendMessage("§c你的国家在线人数不足！需要至少 " + minAttackerOnline + " 人在线。");
            event.setCancelled(true);
            return;
        }

        if (defenderOnline < minDefenderOnline) {
            player.sendMessage("§c目标国家在线人数不足！需要至少 " + minDefenderOnline + " 人在线。");
            event.setCancelled(true);
            return;
        }

        // 检查天空是否被遮挡
        if (!isSkyVisible(location)) {
            player.sendMessage("§c无法在天空被遮挡的地方放置旗帜！");
            event.setCancelled(true);
            return;
        }

        // 检查是否在地表
        if (!isOnSurface(location)) {
            player.sendMessage("§c旗帜必须放置在地表！");
            event.setCancelled(true);
            return;
        }

        // 扣除放置费用
        double stakingFee = LegacyLand.getInstance().getConfig().getDouble("flagwar.costs.staking-fee", 100.0);
        if (targetBlock.isHomeBlock()) {
            stakingFee *= 2;
        }

        // TODO: 集成经济系统扣费
        double balance = LegacyLand.getEcon().getBalance(player);
        if (balance < stakingFee){
            player.sendMessage("§4余额不足，无法打仗！");
            return;
        }
        LegacyLand.getEcon().depositPlayer(player,stakingFee);
        player.sendMessage("§e已扣除放置费用: " + stakingFee + " 金币");

        // 创建 FlagWar
        FlagWarData flagWar = flagWarManager.createFlagWar(player, location, targetBlock);

        if (flagWar != null) {
            player.sendMessage("§a旗帜战争已开始！保护你的旗帜直到计时器完成。");
            player.sendMessage("§e目标地块: " + flagWar.getTownBlockCoords());

            // 自动将攻击方国家添加到防守方的敌人列表
            if (!defenderNation.hasEnemy(attackerNation)) {
                defenderNation.addEnemy(attackerNation);
                player.sendMessage("§e" + defenderNation.getName() + " 现在可以反击你的国家了！");
            }
        } else {
            player.sendMessage("§c创建旗帜战争失败！");
            event.setCancelled(true);
        }
    }

    /**
     * 监听计时器方块破坏 - 防守方胜利
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTimerBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();

        // 检查是否是计时器方块
        FlagWarData flagWar = null;
        for (FlagWarData fw : flagWarManager.getActiveFlagWars()) {
            if (fw.getTimerBlockLocation().equals(location)) {
                flagWar = fw;
                break;
            }
        }

        if (flagWar == null) {
            return;
        }

        Player player = event.getPlayer();

        // 检查是否是防守方
        Resident playerResident = TownyAPI.getInstance().getResident(player);
        Nation playerNation = playerResident != null ? TownyAPI.getInstance().getResidentNationOrNull(playerResident) : null;
        if (playerNation == null || !playerNation.getName().equals(flagWar.getDefenderNation())) {
            player.sendMessage("§c只有防守方可以破坏计时器方块！");
            event.setCancelled(true);
            return;
        }

        // 防守方胜利
        flagWarManager.handleDefenderVictory(flagWar);
        player.sendMessage("§a你成功破坏了敌人的旗帜！");
    }

    /**
     * 检查是否是边界地块
     */
    private boolean isBorderPlot(TownBlock block) {
        try {
            WorldCoord coord = block.getWorldCoord();
            Town town = block.getTownOrNull();

            if (town == null) {
                return false;
            }

            // 检查四个方向是否有非本城镇的地块
            WorldCoord[] neighbors = {
                coord.add(1, 0),
                coord.add(-1, 0),
                coord.add(0, 1),
                coord.add(0, -1)
            };

            for (WorldCoord neighbor : neighbors) {
                TownBlock neighborBlock = TownyAPI.getInstance().getTownBlock(neighbor);
                if (neighborBlock == null || !neighborBlock.hasTown() ||
                    !town.equals(neighborBlock.getTownOrNull())) {
                    return true; // 至少有一个方向不是本城镇，说明是边界
                }
            }

            return false;
        } catch (Exception e) {
            return true; // 出错时默认允许
        }
    }

    /**
     * 获取国家在线玩家数
     */
    private int getOnlinePlayersInNation(Nation nation) {
        return (int) nation.getResidents().stream()
            .filter(resident -> resident.getPlayer() != null && resident.getPlayer().isOnline())
            .count();
    }

    /**
     * 检查天空是否可见
     */
    private boolean isSkyVisible(Location location) {
        Location skyCheck = location.clone();
        for (int y = location.getBlockY() + 1; y < location.getWorld().getMaxHeight(); y++) {
            skyCheck.setY(y);
            if (skyCheck.getBlock().getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否在地表
     */
    private boolean isOnSurface(Location location) {
        // 检查下方是否有固体方块
        Location below = location.clone().subtract(0, 1, 0);
        return below.getBlock().getType().isSolid();
    }
}
