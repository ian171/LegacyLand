package net.chen.legacyLand.nation.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.diplomacy.DiplomacyManager;
import net.chen.legacyLand.nation.diplomacy.GuaranteeManager;
import net.chen.legacyLand.nation.diplomacy.GuaranteeRelation;
import net.chen.legacyLand.util.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 外交保卫命令处理器
 */
public class GuaranteeCommand implements CommandExecutor, TabCompleter {

    private final NationManager nationManager;
    private final GuaranteeManager guaranteeManager;
    private final DiplomacyManager diplomacyManager;
    private final TownyAPI townyAPI;

    public GuaranteeCommand() {
        this.nationManager = NationManager.getInstance();
        this.guaranteeManager = GuaranteeManager.getInstance();
        this.diplomacyManager = DiplomacyManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(LanguageManager.getInstance().translate("msg.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "add", "establish" -> handleEstablish(player, args);
            case "remove", "cancel" -> handleRemove(player, args);
            case "list", "info" -> handleList(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handleEstablish(Player player, String[] args) {
        // 检查是否是国王
        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage(LanguageManager.getInstance().translate("nation.nobelongs"));
            return true;
        }

        if (!myNation.getKing().getUUID().equals(player.getUniqueId())) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.king_only"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.add_usage"));
            return true;
        }

        String targetName = args[1];
        if (targetName.equals(myNation.getName())) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.cannot_self"));
            return true;
        }

        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage(LanguageManager.getInstance().translate("nation.not_found", targetName));
            return true;
        }

        // 检查是否已经存在保卫关系
        if (guaranteeManager.hasGuarantee(myNation.getName(), targetName)) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.already_protecting", targetName));
            return true;
        }

        // 建立保卫关系
        if (guaranteeManager.establishGuarantee(myNation.getName(), targetName)) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.established", targetName));
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.maintenance_cost"));
            return true;
        } else {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.establish_failed"));
            return true;
        }
    }

    private boolean handleRemove(Player player, String[] args) {
        // 检查是否是国王
        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage(LanguageManager.getInstance().translate("nation.nobelongs"));
            return true;
        }

        if (!myNation.getKing().getUUID().equals(player.getUniqueId())) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.king_only_remove"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.remove_usage"));
            return true;
        }

        String targetName = args[1];
        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage(LanguageManager.getInstance().translate("nation.not_found", targetName));
            return true;
        }

        // 取消保卫关系
        if (guaranteeManager.removeGuarantee(myNation.getName(), targetName)) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.removed", targetName));
            return true;
        } else {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.not_protecting", targetName));
            return true;
        }
    }

    private boolean handleList(Player player, String[] args) {
        Nation nation;

        if (args.length >= 2) {
            nation = townyAPI.getNation(args[1]);
            if (nation == null) {
                player.sendMessage(LanguageManager.getInstance().translate("nation.not_found", args[1]));
                return true;
            }
        } else {
            nation = nationManager.getPlayerNation(player);
            if (nation == null) {
                player.sendMessage(LanguageManager.getInstance().translate("nation.nobelongs"));
                return true;
            }
        }

        player.sendMessage(LanguageManager.getInstance().translate("guarantee.relations_header", nation.getName()));

        // 显示保卫的国家
        List<String> guaranteed = guaranteeManager.getGuaranteedNations(nation.getName());
        if (!guaranteed.isEmpty()) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.protecting_nations"));
            for (String protectedNation : guaranteed) {
                GuaranteeRelation relation = guaranteeManager.getGuarantee(nation.getName(), protectedNation);
                String status = relation.isActive() ? LanguageManager.getInstance().translate("status.active") : LanguageManager.getInstance().translate("status.inactive");
                player.sendMessage("  §7- §f" + protectedNation + " " + status);
            }
        } else {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.no_protecting"));
        }

        // 显示被保卫的国家
        List<String> guarantors = guaranteeManager.getGuarantors(nation.getName());
        if (!guarantors.isEmpty()) {
            player.sendMessage(LanguageManager.getInstance().translate("guarantee.protected_by"));
            for (String guarantorNation : guarantors) {
                player.sendMessage("  §7- §f" + guarantorNation);
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(LanguageManager.getInstance().translate("guarantee.help_header"));
        player.sendMessage(LanguageManager.getInstance().translate("guarantee.help_add"));
        player.sendMessage(LanguageManager.getInstance().translate("guarantee.help_remove"));
        player.sendMessage(LanguageManager.getInstance().translate("guarantee.help_list"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "remove", "list"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") ||
                args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("list"))) {
            for (Nation nation : townyAPI.getNations()) {
                completions.add(nation.getName());
            }
        }

        return completions;
    }
}