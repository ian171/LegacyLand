package net.chen.legacyLand.market.commands;

import net.chen.legacyLand.market.MarketManager;
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
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                var result = marketManager.createMarket(player);
                switch (result) {
                    case SUCCESS ->
                            player.sendMessage("§a已在当前地块创建市场！玩家可以在此地块放置箱子进行销售。");
                    case NOT_IN_TOWN ->
                            player.sendMessage("§c你必须站在一个城镇地块上！");
                    case NO_PERMISSION ->
                            player.sendMessage("§c只有城镇市长才能批准市场！");
                    case NO_NATION ->
                            player.sendMessage("§c你的城镇必须属于一个国家！");
                    case ALREADY_MARKET ->
                            player.sendMessage("§c当前地块已经是市场了！");
                }
            }
            case "delete" -> {
                var result = marketManager.deleteMarket(player);
                switch (result) {
                    case SUCCESS -> player.sendMessage("§a市场已删除。");
                    case NOT_MARKET -> player.sendMessage("§c当前地块不是市场！");
                    case NO_PERMISSION -> player.sendMessage("§c你没有权限删除此市场！");
                }
            }
            case "info" -> {
                var market = marketManager.getMarketAt(player.getLocation());
                if (market == null) {
                    player.sendMessage("§c当前地块不是市场！");
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
        player.sendMessage("§e/market create §7- 在当前地块建立市场（需城镇市长权限）");
        player.sendMessage("§e/market delete §7- 删除当前地块的市场");
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
