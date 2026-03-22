package net.chen.legacyLand.market.commands;

import net.chen.legacyLand.market.MarketManager;
import net.chen.legacyLand.util.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /price 命令
 */
public class PriceCommand implements CommandExecutor, TabCompleter {

    private final MarketManager marketManager;

    public PriceCommand() {
        this.marketManager = MarketManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().translate("msg.player_only"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(LanguageManager.getInstance().translate("market.price_usage"));
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                // 开始定价流程
                marketManager.startPriceSet(player);
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(LanguageManager.getInstance().translate("error.invalid_amount"));
                return true;
            }
            var result = marketManager.setPrice(player, price);
            switch (result) {
                case SUCCESS ->
                        player.sendMessage(LanguageManager.getInstance().translate("market.price_set", price));
                case NOT_PENDING ->
                        player.sendMessage(LanguageManager.getInstance().translate("market.select_chest_first"));
                case NO_CHEST_SELECTED ->
                        player.sendMessage(LanguageManager.getInstance().translate("market.click_chest"));
                case CHEST_NOT_FOUND ->
                        player.sendMessage(LanguageManager.getInstance().translate("market.chest_not_found"));
                case NOT_OWNER ->
                        player.sendMessage(LanguageManager.getInstance().translate("market.not_owner"));
                case INVALID_PRICE ->
                        player.sendMessage(LanguageManager.getInstance().translate("market.price_positive"));
            }
        } else {
            player.sendMessage(LanguageManager.getInstance().translate("market.price_usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("set");
        return List.of();
    }
}
