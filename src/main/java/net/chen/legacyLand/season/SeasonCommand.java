package net.chen.legacyLand.season;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 季节命令处理器
 * /season info - 查看当前季节信息
 * /season set <季节> - 设置季节（需要权限）
 * /season config <天数> - 设置每个子季节持续天数（需要权限）
 */
public class SeasonCommand implements CommandExecutor, TabCompleter {

    private final LegacyLand plugin;
    private final SeasonManager seasonManager;

    public SeasonCommand(LegacyLand plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c用法: /season <info|set|config>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                return handleInfo(sender);
            case "set":
                return handleSet(sender, args);
            case "config":
                return handleConfig(sender, args);
            default:
                sender.sendMessage("§c未知子命令: " + args[0]);
                return true;
        }
    }

    /**
     * 处理 info 命令
     */
    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage("§e§l========== 季节信息 ==========");
        sender.sendMessage(seasonManager.getSeasonInfo());
        sender.sendMessage("§e基础温度: §f" + seasonManager.getCurrentSeason().getBaseTemperature() + "°C");
        return true;
    }

    /**
     * 处理 set 命令
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("legacyland.season.set")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /season set <季节>");
            sender.sendMessage("§7可用季节: early_spring, mid_spring, late_spring, early_summer, mid_summer, late_summer, early_autumn, mid_autumn, late_autumn, early_winter, mid_winter, late_winter");
            return true;
        }

        Season season = Season.fromKey(args[1]);
        seasonManager.setSeason(season);
        sender.sendMessage("§a季节已设置为: " + season.getDisplayName() + " (" + season.getType().getDisplayName() + ")");
        return true;
    }

    /**
     * 处理 config 命令
     */
    private boolean handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("legacyland.season.config")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /season config <天数>");
            sender.sendMessage("§7当前设置: 每个子季节持续 " + seasonManager.getDaysPerSubSeason() + " 天");
            return true;
        }

        try {
            int days = Integer.parseInt(args[1]);
            if (days < 1) {
                sender.sendMessage("§c天数必须大于0！");
                return true;
            }
            seasonManager.setDaysPerSubSeason(days);
            sender.sendMessage("§a每个子季节持续天数已设置为: " + days + " 天");
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的数字: " + args[1]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            if (sender.hasPermission("legacyland.season.set")) {
                completions.add("set");
            }
            if (sender.hasPermission("legacyland.season.config")) {
                completions.add("config");
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Arrays.stream(Season.values())
                    .map(Season::getKey)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            return Arrays.asList("1", "5", "8", "10", "15");
        }

        return completions;
    }
}
