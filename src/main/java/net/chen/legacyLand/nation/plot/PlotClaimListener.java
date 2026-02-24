package net.chen.legacyLand.nation.plot;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * PlotClaim 监听器
 * 监听占领相关事件
 */
public class PlotClaimListener implements Listener {

    private final PlotClaimManager manager = PlotClaimManager.getInstance();

    /**
     * 拦截 Towny 的 claim 事件，启动 PlotClaim 流程
     */
    @EventHandler
    public void onTownPreClaim(TownPreClaimEvent event) {
        // 检查是否启用 PlotClaim 功能
        if (!LegacyLand.getInstance().getConfig().getBoolean("plot-claim.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        WorldCoord worldCoord = event.getTownBlock().getWorldCoord();

        // 取消原始 claim
        event.setCancelled(true);

        // 启动 PlotClaim 流程
        manager.startClaim(player, worldCoord);
    }

    /**
     * 监听玩家移动，检测是否离开占领区块或敌方进入
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        // 只在跨区块移动时检查
        if (fromChunk.equals(toChunk)) {
            return;
        }

        // 检查玩家是否正在占领
        PlotClaim claim = manager.getClaimByPlayer(player.getUniqueId());
        if (claim != null) {
            // 检查是否离开了占领区块
            WorldCoord claimCoord = claim.getWorldCoord();
            if (toChunk.getX() != claimCoord.getX() || toChunk.getZ() != claimCoord.getZ()) {
                manager.cancelClaim(player.getUniqueId());
                return;
            }
        }

        // 检查是否有敌方进入/离开占领区块
        checkEnemyPresence(toChunk);
        checkEnemyPresence(fromChunk);
    }

    /**
     * 检查区块内的敌方玩家
     */
    private void checkEnemyPresence(Chunk chunk) {
        WorldCoord coord = WorldCoord.parseWorldCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        PlotClaim claim = manager.getClaimAtCoord(coord.getWorldName() + "," + coord.getX() + "," + coord.getZ());

        if (claim == null) {
            return;
        }

        Player claimer = org.bukkit.Bukkit.getPlayer(claim.getPlayerId());
        if (claimer == null) {
            manager.cancelClaim(claim.getPlayerId());
            return;
        }

        Town claimerTown = TownyAPI.getInstance().getTown(claimer);
        if (claimerTown == null) {
            return;
        }

        // 检查区块内是否有敌方玩家
        boolean hasEnemy = false;
        for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                Player other = (Player) entity;
                if (other.getUniqueId().equals(claim.getPlayerId())) {
                    continue;
                }

                Town otherTown = TownyAPI.getInstance().getTown(other);
                if (otherTown == null || !otherTown.equals(claimerTown)) {
                    hasEnemy = true;
                    break;
                }
            }
        }

        // 根据敌方存在情况暂停或恢复
        if (hasEnemy && !claim.isPaused()) {
            manager.pauseClaim(claim.getPlayerId(), "敌方玩家进入区块");
        } else if (!hasEnemy && claim.isPaused()) {
            manager.resumeClaim(claim.getPlayerId());
        }
    }

    /**
     * 玩家退出时取消占领
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlotClaim claim = manager.getClaimByPlayer(event.getPlayer().getUniqueId());
        if (claim != null) {
            manager.cancelClaim(event.getPlayer().getUniqueId());
        }
    }
}
