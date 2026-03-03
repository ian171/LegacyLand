package net.chen.legacyLand.economy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * 货币铸造聊天监听器
 * 处理玩家输入的货币面值
 */
public class CurrencyMintChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 检查玩家是否有铸币会话
        if (CurrencyMintSession.hasSession(player)) {
            event.setCancelled(true);
            String input = event.getMessage();

            // 在主线程处理（因为涉及物品操作）
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("LegacyLand"),
                () -> CurrencyMintSession.handleInput(player, input)
            );
        }
    }
}
