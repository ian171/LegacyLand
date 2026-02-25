package net.chen.legacyLand.nation.plot;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * PlotClaim 管理器
 * 管理所有地块占领实例
 */
public class PlotClaimManager {
    private static PlotClaimManager instance;
    private final Map<UUID, PlotClaim> activeClaims;
    private final Map<String, PlotClaim> coordClaimMap;

    private PlotClaimManager() {
        this.activeClaims = new HashMap<>();
        this.coordClaimMap = new HashMap<>();
    }

    public static PlotClaimManager getInstance() {
        if (instance == null) {
            instance = new PlotClaimManager();
        }
        return instance;
    }

    /**
     * 开始占领地块
     */
    public PlotClaim startClaim(Player player, WorldCoord worldCoord) {
        try {
            Town town = TownyAPI.getInstance().getTown(player);
            if (town == null) {
                player.sendMessage("§c你必须属于一个城镇才能占领地块！");
                return null;
            }

            // 检查玩家是否已有进行中的占领
            if (activeClaims.containsKey(player.getUniqueId())) {
                player.sendMessage("§c你已经在占领另一个地块了！");
                return null;
            }

            // 检查该区块是否已有人在占领
            String coordKey = getCoordKey(worldCoord);
            if (coordClaimMap.containsKey(coordKey)) {
                player.sendMessage("§c该地块已有人在占领中！");
                return null;
            }

            int requiredSeconds = LegacyLand.getInstance().getConfig().getInt("plot-claim.claim-duration-seconds", 60);

            PlotClaim claim = new PlotClaim(
                UUID.randomUUID().toString(),
                player.getUniqueId(),
                town.getName(),
                worldCoord,
                System.currentTimeMillis(),
                0,
                requiredSeconds,
                false,
                null
            );

            activeClaims.put(player.getUniqueId(), claim);
            coordClaimMap.put(coordKey, claim);

            player.sendMessage("§e开始占领此区块，请在区块内停留 " + requiredSeconds + " 秒...");
            LegacyLand.logger.info("PlotClaim 已创建: " + claim.getClaimId() + " by " + player.getName());

            return claim;

        } catch (Exception e) {
            LegacyLand.logger.severe("创建 PlotClaim 失败: " + e.getMessage());
            player.sendMessage("遇到未知问题，请联系管理员寻求帮助");
            return null;
        }
    }

    /**
     * 取消占领
     */
    public void cancelClaim(UUID playerId) {
        PlotClaim claim = activeClaims.get(playerId);
        if (claim != null) {
            PlotClaimTimerTask.removeBossBar(claim);

            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§c占领已取消！");
            }

            String coordKey = getCoordKey(claim.getWorldCoord());
            activeClaims.remove(playerId);
            coordClaimMap.remove(coordKey);

            LegacyLand.logger.info("PlotClaim 已取消: " + claim.getClaimId());
        }
    }

    /**
     * 完成占领
     */
    public void completeClaim(PlotClaim claim) {
        try {
            Player player = Bukkit.getPlayer(claim.getPlayerId());
            Town town = TownyAPI.getInstance().getTown(claim.getTownName());

            if (town == null) {
                if (player != null) {
                    player.sendMessage("§c城镇不存在，占领失败！");
                }
                cancelClaim(claim.getPlayerId());
                return;
            }

            // 调用 Towny API 完成真正的占领
            try {
                //town.addTownBlock(TownyAPI.getInstance().getTownBlock(claim.getWorldCoord()));
                //town.addTownBlock(TownyAPI.getInstance().getTownBlock(player).setClaimedAt(player.get););
                if (player != null) {
                    // 检查玩家是否具有声明领地的权限
                    Resident resident = TownyAPI.getInstance().getResident(player);
                    if (resident == null) {
                        player.sendMessage("§c你没有声明领地的权限！");
                        cancelClaim(claim.getPlayerId());
                        return;
                    }
                    if (!resident.isMayor()
                            && !resident.hasTownRank("assistant")
                            && !resident.hasPermissionNode("towny.command.town.claim")) {
                        player.sendMessage("§c你没有声明领地的权限！");
                        cancelClaim(claim.getPlayerId());
                        return;
                    }
                    List<WorldCoord> list = new ArrayList<>();
                    list.add(WorldCoord.parseWorldCoord(player));
                    new TownClaim(Towny.getPlugin(),player, town,list,false,true,true).run();
                }else {
                    LegacyLand.logger.warning("无法创建领地，因为无法找到玩家");
                    return;
                }
                // 广播消息
                String message = String.format("§e[领地] §a%s 成功占领了一块地块！", claim.getTownName());
                Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(message));

                LegacyLand.logger.info("PlotClaim 完成: " + claim.getClaimId());

            } catch (Exception e) {
                if (player != null) {
                    player.sendMessage("§c占领失败: " + e.getMessage());
                }
                LegacyLand.logger.severe("占领地块失败: " + e.getMessage());
            }

            // 清理 BossBar 并从内存移除
            PlotClaimTimerTask.removeBossBar(claim);
            String coordKey = getCoordKey(claim.getWorldCoord());
            activeClaims.remove(claim.getPlayerId());
            coordClaimMap.remove(coordKey);
        } catch (Exception e) {
            LegacyLand.logger.severe("完成 PlotClaim 失败: " + e.getMessage());
        }
    }

    /**
     * 暂停占领
     */
    public void pauseClaim(UUID playerId, String reason) {
        PlotClaim claim = activeClaims.get(playerId);
        if (claim != null && !claim.isPaused()) {
            claim.setPaused(true);
            claim.setPauseReason(reason);

            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§e占领已暂停: " + reason);
            }
        }
    }

    /**
     * 恢复占领
     */
    public void resumeClaim(UUID playerId) {
        PlotClaim claim = activeClaims.get(playerId);
        if (claim != null && claim.isPaused()) {
            claim.setPaused(false);
            claim.setPauseReason(null);

            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§a占领已恢复！");
            }
        }
    }

    /**
     * 获取玩家的占领
     */
    public PlotClaim getClaimByPlayer(UUID playerId) {
        return activeClaims.get(playerId);
    }

    /**
     * 获取指定坐标的占领
     */
    public PlotClaim getClaimAtCoord(String coordKey) {
        return coordClaimMap.get(coordKey);
    }

    /**
     * 获取所有活跃的占领
     */
    public Collection<PlotClaim> getActiveClaims() {
        return Collections.unmodifiableCollection(activeClaims.values());
    }

    /**
     * 生成坐标键
     */
    private String getCoordKey(WorldCoord coord) {
        return coord.getWorldName() + "," + coord.getX() + "," + coord.getZ();
    }
}
