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
 * /market 命令
 */
public class MarketCommand implements CommandExecutor, TabCompleter {

    private final MarketManager marketManager;

    public MarketCommand() {
        this.marketManager = MarketManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().translate("msg.player_only"));
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                var result = marketManager.createMarket(player,args[1]);
                switch (result) {
                    case SUCCESS ->
                            player.sendMessage(LanguageManager.getInstance().translate("market.created"));
                    case NOT_IN_TOWN ->
                            player.sendMessage(LanguageManager.getInstance().translate("market.not_in_town"));
                    case NO_PERMISSION ->
                            player.sendMessage(LanguageManager.getInstance().translate("market.mayor_only"));
                    case NO_NATION ->
                            player.sendMessage(LanguageManager.getInstance().translate("market.nation_required"));
                    case ALREADY_MARKET ->
                            player.sendMessage(LanguageManager.getInstance().translate("market.already_market"));
                }
            }
            case "delete" -> {
                var result = marketManager.deleteMarket(player);
                switch (result) {
                    case SUCCESS -> player.sendMessage(LanguageManager.getInstance().translate("market.deleted"));
                    case NOT_MARKET -> player.sendMessage(LanguageManager.getInstance().translate("market.not_market"));
                    case NO_PERMISSION -> player.sendMessage(LanguageManager.getInstance().translate("market.no_delete_permission"));
                }
            }
            case "info" -> {
                var market = marketManager.getMarketAt(player.getLocation());
                if (market == null) {
                    player.sendMessage(LanguageManager.getInstance().translate("market.not_market"));
                } else {
                    player.sendMessage("§6===== 市场信息 =====");
                    player.sendMessage("§e所属国家: §f" + market.getNationName());
                    player.sendMessage("§e世界: §f" + market.getWorldName());
                    player.sendMessage("§e区块: §f(" + market.getChunkX() + ", " + market.getChunkZ() + ")");
                    player.sendMessage("§e销售箱数量: §f" + market.getAllChests().size());
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6===== 市场命令 =====");
        player.sendMessage("§e/market create §7- " + LanguageManager.getInstance().translate("market.mayor_only"));
        player.sendMessage("§e/market delete §7- " + LanguageManager.getInstance().translate("market.no_delete_permission"));
        player.sendMessage("§e/market info §7- 查看当前地块市场信息");
        player.sendMessage("§7在市场地块放置箱子会自动注册为销售箱。");
        player.sendMessage("§7使用 §f/price set <金额> §7为你的销售箱设置价格。");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("create", "delete", "info");
        return List.of();
    }
}
