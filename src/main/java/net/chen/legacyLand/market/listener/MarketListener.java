package net.chen.legacyLand.market.listener;

import net.chen.legacyLand.market.MarketChest;
import net.chen.legacyLand.market.MarketManager;
import net.chen.legacyLand.util.FoliaScheduler;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 市场监听器
 * - 玩家在市场地块放置箱子 → 自动注册为销售箱
 * - 玩家破坏销售箱 → 自动注销
 * - 玩家右键点击销售箱：
 *   a. 若是箱子主人且有待定价状态 → 记录该箱子，提示输入 /price set
 *   b. 若是其他玩家 → 直接购买
 */
public class MarketListener implements Listener {

    private final MarketManager marketManager;
    private final LegacyLand plugin;

    public MarketListener(LegacyLand plugin) {
        this.plugin = plugin;
        this.marketManager = MarketManager.getInstance();
    }

    /**
     * 玩家放置箱子 → 在市场地块内自动注册为销售箱
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;

        Player player = event.getPlayer();
        Location loc = block.getLocation();

        // 检查是否在市场内
        if (marketManager.getMarketAt(loc) == null) return;

        boolean registered = marketManager.registerChest(player, loc);
        if (registered) {
            player.sendMessage("§a已将箱子注册为销售箱！使用 §f/price set §a来为手持物品定价，然后右键点击此箱子。");
        }
    }

    /**
     * 玩家破坏箱子 → 注销销售箱（箱子中的物品由 Bukkit 正常掉落）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;

        Location loc = block.getLocation();
        if (!marketManager.isMarketChest(loc)) return;

        MarketChest chest = marketManager.getChestAt(loc);
        // 只有箱子主人可以破坏
        if (chest != null && !chest.getOwnerUuid().equals(event.getPlayer().getUniqueId())
                && !event.getPlayer().hasPermission("legacyland.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c你只能破坏自己的销售箱！");
            return;
        }

        marketManager.unregisterChest(loc);
        if (chest != null) {
            event.getPlayer().sendMessage("§7销售箱已注销。");
        }
    }

    /**
     * 玩家右键点击箱子
     * - 自己的箱子 + 等待定价状态 → 记录目标箱子
     * - 自己的箱子（正常） → 打开箱子（不拦截）
     * - 他人的销售箱 → 触发购买流程
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理右键点击方块，且只处理主手
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;

        Location loc = block.getLocation();
        if (!marketManager.isMarketChest(loc)) return;

        Player player = event.getPlayer();
        MarketChest chest = marketManager.getChestAt(loc);
        if (chest == null) return;

        // 如果是自己的箱子
        if (chest.getOwnerUuid().equals(player.getUniqueId())) {
            // 如果处于等待定价流程，记录目标箱子
            if (marketManager.hasPendingPriceSet(player.getUniqueId())) {
                marketManager.setPendingChest(player.getUniqueId(), chest.getLocationKey());
                event.setCancelled(true);
                player.sendMessage("§e已选中此销售箱。现在输入 §f/price set <金额> §e来确认价格。");
            }
            // 否则正常打开箱子（卖家可以直接补货/取货），不拦截
            return;
        }

        // 他人的销售箱 → 阻止原生打开，改为购买
        event.setCancelled(true);

        if (!chest.isActive()) {
            player.sendMessage("§c此销售箱尚未开放销售！");
            return;
        }

        // 使用 Folia 兼容方式在正确线程执行购买
        FoliaScheduler.runForPlayer(plugin, player, () -> {
            var result = marketManager.purchaseFromChest(player, loc);
            switch (result) {
                case SUCCESS ->
                        player.sendMessage("§a购买成功！花费 §6" + chest.getPricePerItem() + " §a金币。");
                case CHEST_EMPTY ->
                        player.sendMessage("§c此销售箱已售罄！");
                case INSUFFICIENT_FUNDS ->
                        player.sendMessage("§c余额不足！此物品售价 §6" + chest.getPricePerItem() + " §c金币。");
                case INVENTORY_FULL ->
                        player.sendMessage("§c你的背包已满！");
                case OWN_CHEST ->
                        player.sendMessage("§c你不能购买自己的销售箱中的物品！");
                case ECON_ERROR ->
                        player.sendMessage("§c经济系统错误，请联系管理员！");
                default ->
                        player.sendMessage("§c购买失败！");
            }
        });
    }
}
