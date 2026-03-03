package net.chen.legacyLand.economy.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.economy.CurrencyItem;
import net.chen.legacyLand.economy.TreasuryManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

/**
 * 货币合成监听器
 * 监听工作台合成，实现：国库印章 + 纸张 + 墨囊 = 货币
 */
public class CurrencyCraftListener implements Listener {

    private final TreasuryManager treasuryManager;

    public CurrencyCraftListener(TreasuryManager treasuryManager) {
        this.treasuryManager = treasuryManager;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        // 检查合成配方：需要国库印章、纸张、墨囊
        boolean hasSeal = false;
        boolean hasPaper = false;
        boolean hasInkSac = false;
        String nationName = null;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (CurrencyItem.isTreasurySeal(item)) {
                hasSeal = true;
                nationName = CurrencyItem.getNation(item);
            } else if (item.getType() == Material.PAPER) {
                hasPaper = true;
            } else if (item.getType() == Material.INK_SAC) {
                hasInkSac = true;
            }
        }

        // 如果不是货币合成配方，正常处理
        if (!hasSeal || !hasPaper || !hasInkSac) {
            return;
        }

        // 取消默认合成
        event.setCancelled(true);

        // 验证玩家权限
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            player.sendMessage("§c你必须是 Towny 居民！");
            return;
        }

        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null || !nation.getName().equals(nationName)) {
            player.sendMessage("§c你不属于 " + nationName + "，无法使用该国库印章！");
            return;
        }

        // 检查国库是否存在
        TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nationName);
        if (treasury == null) {
            player.sendMessage("§c国库不存在！");
            return;
        }

        // 请求玩家输入面值（通过聊天）
        player.sendMessage("§e请在聊天框输入要铸造的货币面值（例如：100）");
        player.sendMessage("§7提示：输入 'cancel' 取消铸造");

        // 注册聊天监听器等待输入
        CurrencyMintSession.startSession(player, nationName, treasuryManager);
    }
}
