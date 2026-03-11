package net.chen.legacyLand.resource.commands;

import net.chen.legacyLand.resource.*;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源系统命令
 * /resource <subcommand>
 */
public class ResourceCommand implements CommandExecutor, TabCompleter {

    private final ResourceSystemManager manager;

    public ResourceCommand(ResourceSystemManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(player, args);
            case "zone" -> handleZone(player, args);
            case "transport" -> handleTransport(player, args);
            case "info" -> handleInfo(player, args);
            case "help" -> sendHelp(player);
            default -> player.sendMessage("§c未知子命令。使用 /resource help 查看帮助");
        }

        return true;
    }

    /**
     * 给予资源物品
     * /resource give <player> <type> <amount>
     */
    private void handleGive(Player sender, String[] args) {
        if (!sender.hasPermission("legacyland.resource.give")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§c用法: /resource give <玩家> <资源类型> <数量>");
            return;
        }

        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家不在线");
            return;
        }

        try {
            ResourceType type = ResourceType.valueOf(args[2].toUpperCase());
            int amount = Integer.parseInt(args[3]);

            if (amount <= 0 || amount > 64) {
                sender.sendMessage("§c数量必须在 1-64 之间");
                return;
            }

            if (manager.giveResourceToPlayer(target, type, amount)) {
                sender.sendMessage("§a已给予 " + target.getName() + " " +
                        type.getColoredName() + " §ax" + amount);
            } else {
                sender.sendMessage("§c给予资源失败");
            }

        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c无效的资源类型");
        }
    }

    /**
     * 管理工业区域
     * /resource zone <register|info>
     */
    private void handleZone(Player sender, String[] args) {
        if (!sender.hasPermission("legacyland.resource.zone")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /resource zone <register|info>");
            return;
        }

        if (args[1].equalsIgnoreCase("register")) {
            if (args.length < 5) {
                sender.sendMessage("§c用法: /resource zone register <国家名> <半径> <类型>");
                sender.sendMessage("§7类型: NATIONAL_FURNACE, MINT_FACTORY, PROCESSING_PLANT");
                return;
            }

            String nationName = args[2];
            double radius = Double.parseDouble(args[3]);
            IndustrialZoneManager.ZoneType type = IndustrialZoneManager.ZoneType.valueOf(args[4].toUpperCase());

            manager.registerIndustrialZone(nationName, sender.getLocation(), radius, type);
            sender.sendMessage("§a已注册工业区域: " + type.getDisplayName());

        } else if (args[1].equalsIgnoreCase("info")) {
            manager.getZoneAt(sender.getLocation()).ifPresentOrElse(
                    zone -> {
                        sender.sendMessage("§e§l=== 工业区域信息 ===");
                        sender.sendMessage("§7国家: §f" + zone.getNationName());
                        sender.sendMessage("§7类型: §f" + zone.getType().getDisplayName());
                    },
                    () -> sender.sendMessage("§c当前位置不在任何工业区域内")
            );
        }
    }

    /**
     * 运输成本计算
     * /resource transport <x> <y> <z>
     */
    private void handleTransport(Player sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c用法: /resource transport <x> <y> <z>");
            return;
        }

        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);

            Location target = new Location(sender.getWorld(), x, y, z);
            manager.showTransportCostPreview(sender, sender.getLocation(), target);

        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的坐标");
        }
    }

    /**
     * 查看资源信息
     * /resource info
     */
    private void handleInfo(Player sender, String[] args) {
        sender.sendMessage("§e§l=== 资源系统信息 ===");
        sender.sendMessage("§7背包重量: §f" +
                String.format("%.2f", manager.getPlayerInventoryWeight(sender)) + " 单位");

        manager.getZoneAt(sender.getLocation()).ifPresent(zone ->
                sender.sendMessage("§7当前区域: §a" + zone.getType().getDisplayName() +
                        " §7(" + zone.getNationName() + ")")
        );
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage("§e§l=== 资源系统命令 ===");
        player.sendMessage("§7/resource give <玩家> <类型> <数量> §f- 给予资源");
        player.sendMessage("§7/resource zone register <国家> <半径> <类型> §f- 注册工业区域");
        player.sendMessage("§7/resource zone info §f- 查看当前区域信息");
        player.sendMessage("§7/resource transport <x> <y> <z> §f- 计算运输成本");
        player.sendMessage("§7/resource info §f- 查看资源信息");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "zone", "transport", "info", "help");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("zone")) {
            return Arrays.asList("register", "info");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(ResourceType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
