package net.chen.legacyLand.nation.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.diplomacy.DiplomacyManager;
import net.chen.legacyLand.nation.diplomacy.GuaranteeManager;
import net.chen.legacyLand.nation.diplomacy.GuaranteeRelation;
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
            sender.sendMessage("§c此命令只能由玩家执行！");
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
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        if (!myNation.getKing().getUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c只有国王才能建立外交保卫关系！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /guarantee add <国家名>");
            return true;
        }

        String targetName = args[1];
        if (targetName.equals(myNation.getName())) {
            player.sendMessage("§c不能保卫自己的国家！");
            return true;
        }

        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        // 检查是否已经存在保卫关系
        if (guaranteeManager.hasGuarantee(myNation.getName(), targetName)) {
            player.sendMessage("§c你的国家已经在保卫 " + targetName + " 了！");
            return true;
        }

        // 建立保卫关系
        if (guaranteeManager.establishGuarantee(myNation.getName(), targetName)) {
            player.sendMessage("§a成功建立对 " + targetName + " 的外交保卫关系！");
            player.sendMessage("§e每小时需支付 500 金币 + 10 Trade_XP 维持费用。");
            return true;
        } else {
            player.sendMessage("§c建立保卫关系失败！");
            return true;
        }
    }

    private boolean handleRemove(Player player, String[] args) {
        // 检查是否是国王
        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        if (!myNation.getKing().getUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c只有国王才能取消外交保卫关系！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /guarantee remove <国家名>");
            return true;
        }

        String targetName = args[1];
        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        // 取消保卫关系
        if (guaranteeManager.removeGuarantee(myNation.getName(), targetName)) {
            player.sendMessage("§a已取消对 " + targetName + " 的外交保卫关系！");
            return true;
        } else {
            player.sendMessage("§c你的国家没有保卫 " + targetName + "！");
            return true;
        }
    }

    private boolean handleList(Player player, String[] args) {
        Nation nation;

        if (args.length >= 2) {
            nation = townyAPI.getNation(args[1]);
            if (nation == null) {
                player.sendMessage("§c国家 " + args[1] + " 不存在！");
                return true;
            }
        } else {
            nation = nationManager.getPlayerNation(player);
            if (nation == null) {
                player.sendMessage("§c你不在任何国家中！");
                return true;
            }
        }

        player.sendMessage("§6========== " + nation.getName() + " 外交保卫关系 ==========");

        // 显示保卫的国家
        List<String> guaranteed = guaranteeManager.getGuaranteedNations(nation.getName());
        if (!guaranteed.isEmpty()) {
            player.sendMessage("§e保卫的国家:");
            for (String protectedNation : guaranteed) {
                GuaranteeRelation relation = guaranteeManager.getGuarantee(nation.getName(), protectedNation);
                String status = relation.isActive() ? "§a激活" : "§c失效";
                player.sendMessage("  §7- §f" + protectedNation + " " + status);
            }
        } else {
            player.sendMessage("§7暂无保卫任何国家");
        }

        // 显示被保卫的国家
        List<String> guarantors = guaranteeManager.getGuarantors(nation.getName());
        if (!guarantors.isEmpty()) {
            player.sendMessage("§e被以下国家保卫:");
            for (String guarantorNation : guarantors) {
                player.sendMessage("  §7- §f" + guarantorNation);
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 外交保卫命令 ==========");
        player.sendMessage("§e/guarantee add <国家名> §7- 建立保卫关系（仅国王）");
        player.sendMessage("§e/guarantee remove <国家名> §7- 取消保卫关系（仅国王）");
        player.sendMessage("§e/guarantee list [国家名] §7- 查看保卫关系");
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