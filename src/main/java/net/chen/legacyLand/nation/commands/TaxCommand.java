package net.chen.legacyLand.nation.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 税收命令处理器
 * 注意：税收数据存储在 Towny 的 Nation 对象中
 */
public class TaxCommand implements CommandExecutor, TabCompleter {

    private final NationManager nationManager;
    private final TownyAPI townyAPI;

    public TaxCommand() {
        this.nationManager = NationManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                return handleInfo(player);
            case "set":
                return handleSet(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleInfo(Player player) {
        Nation nation = nationManager.getPlayerNation(player);
        if (nation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        player.sendMessage("§6========== " + nation.getName() + " 税收信息 ==========");
        player.sendMessage("§e国家税率: §f" + nation.getTaxes() + "%");
        player.sendMessage("§7提示: 使用 Towny 的 /nation set taxes 命令设置税率");

        return true;
    }

    private boolean handleSet(Player player, String[] args) {
        Nation nation = nationManager.getPlayerNation(player);
        if (nation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        if (!nationManager.hasPermission(player, NationPermission.ADJUST_TAX_RATE)) {
            player.sendMessage("§c你没有权限管理税收！");
            return true;
        }

        player.sendMessage("§e请使用 Towny 的命令设置税率:");
        player.sendMessage("§f/nation set taxes <金额> §7- 设置国家税");
        player.sendMessage("§f/nation set plottax <金额> §7- 设置地块税");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 税收命令 ==========");
        player.sendMessage("§e/tax info §7- 查看税收信息");
        player.sendMessage("§e/tax set §7- 查看设置税收的帮助");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("info", "set"));
        }

        return completions;
    }
}
