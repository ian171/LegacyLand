package net.chen.legacyLand.economy.listeners;

import net.chen.legacyLand.economy.CurrencyItem;
import net.chen.legacyLand.economy.TreasuryManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 货币铸造会话管理
 * 管理玩家的铸币输入会话
 */
public class CurrencyMintSession {
    private static final Map<UUID, MintSession> sessions = new HashMap<>();

    public static void startSession(Player player, String nationName, TreasuryManager treasuryManager) {
        sessions.put(player.getUniqueId(), new MintSession(nationName, treasuryManager));
    }

    public static boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public static void handleInput(Player player, String input) {
        MintSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§c已取消铸造货币");
            sessions.remove(player.getUniqueId());
            return;
        }

        try {
            double denomination = Double.parseDouble(input);
            if (denomination <= 0) {
                player.sendMessage("§c面值必须大于 0！请重新输入");
                return;
            }

            // 发行货币
            ItemStack currency = session.treasuryManager.issueCurrency(
                session.nationName,
                denomination,
                player.getUniqueId().toString()
            );

            if (currency != null) {
                player.getInventory().addItem(currency);
                player.sendMessage("§a成功铸造面值 " + denomination + " 的货币！");
                player.sendMessage("§7序列号: " + CurrencyItem.getSerialNumber(currency));
            } else {
                player.sendMessage("§c铸造失败！");
            }

            sessions.remove(player.getUniqueId());
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数字！请输入有效的面值");
        }
    }

    public static void cancelSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private static class MintSession {
        final String nationName;
        final TreasuryManager treasuryManager;

        MintSession(String nationName, TreasuryManager treasuryManager) {
            this.nationName = nationName;
            this.treasuryManager = treasuryManager;
        }
    }
}
