package net.chen.legacyLand.market.commands;

import net.chen.legacyLand.market.MarketManager;
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
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§c用法: /price set <金额>");
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
                player.sendMessage("§c金额必须是有效数字！");
                return true;
            }
            var result = marketManager.setPrice(player, price);
            switch (result) {
                case SUCCESS ->
                        player.sendMessage("§a价格已设置为 §6" + price + " §a金币/个！箱子现在对外开放销售。");
                case NOT_PENDING ->
                        player.sendMessage("§c请先右键点击你的销售箱，再使用此命令。");
                case NO_CHEST_SELECTED ->
                        player.sendMessage("§c请先右键点击你的销售箱！");
                case CHEST_NOT_FOUND ->
                        player.sendMessage("§c找不到该销售箱，可能已被删除。");
                case NOT_OWNER ->
                        player.sendMessage("§c你只能为自己的销售箱设置价格！");
                case INVALID_PRICE ->
                        player.sendMessage("§c价格必须大于0！");
            }
        } else {
            player.sendMessage("§c用法: /price set <金额>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("set");
        return List.of();
    }
}
