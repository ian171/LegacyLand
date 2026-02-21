package net.chen.legacyLand.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.diplomacy.DiplomacyManager;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 战争保护监听器
 * 防止敌对国家成员在对方领地进行互动
 */
public class WarProtectionListener implements Listener {

    private final NationManager nationManager;
    private final DiplomacyManager diplomacyManager;
    private final TownyAPI townyAPI;

    public WarProtectionListener() {
        this.nationManager = NationManager.getInstance();
        this.diplomacyManager = DiplomacyManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
    }

    /**
     * 检查玩家是否可以在该位置互动
     */
    private boolean canInteract(Player player, Location location) {
        // 获取玩家所在国家
        Nation playerNation = nationManager.getPlayerNation(player);
        if (playerNation == null) {
            // 无国家玩家，允许互动（由 Towny 自己处理权限）
            return false;
        }

        // 获取该位置所属的城镇
        TownBlock townBlock = townyAPI.getTownBlock(location);
        if (townBlock == null) {
            // 不在任何城镇中，允许互动
            return false;
        }

        Town town = townBlock.getTownOrNull();
        if (town == null) {
            return false;
        }

        // 获取城镇所属国家（通过 Towny 的 Nation）
        com.palmergames.bukkit.towny.object.Nation townyNation = town.getNationOrNull();
        if (townyNation == null) {
            // 城镇不属于任何国家，允许互动
            return false;
        }

        String townNationName = townyNation.getName();

        // 如果是自己国家的领地，允许互动
        if (playerNation.getName().equals(townNationName)) {
            return false;
        }

        // 检查外交关系
        RelationType relation = diplomacyManager.getRelation(playerNation.getName(), townNationName);

        // 如果是敌对关系（战争），禁止互动
        if (relation.isHostile()) {
            player.sendMessage("§c你无法在敌对国家的领地进行互动！");
            return true;
        }

        // 如果不是友好关系（中立），也禁止互动
        if (!relation.isFriendly() && relation != RelationType.NEUTRAL) {
            player.sendMessage("§c你无法在非友好国家的领地进行互动！");
            return true;
        }

        // 友好关系或中立关系，允许互动（由 Towny 自己处理权限）
        return false;
    }

    /**
     * 阻止破坏方块
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (canInteract(player, block.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 阻止放置方块
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (canInteract(player, block.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 阻止方块互动（如开门、使用箱子等）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        // 只检查右键互动
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            if (canInteract(player, block.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 阻止实体互动（如攻击村民、使用展示框等）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // 如果是玩家，不处理（PVP 由其他系统处理）
        if (entity instanceof Player) {
            return;
        }

        if (canInteract(player, entity.getLocation())) {
            event.setCancelled(true);
        }
    }
}
