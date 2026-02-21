package net.chen.legacyLand.nation.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationPermission;
import net.chen.legacyLand.nation.diplomacy.DiplomacyManager;
import net.chen.legacyLand.nation.diplomacy.DiplomacyRelation;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 外交命令处理器
 */
public class DiplomacyCommand implements CommandExecutor, TabCompleter {

    private final NationManager nationManager;
    private final DiplomacyManager diplomacyManager;
    private final TownyAPI townyAPI;

    public DiplomacyCommand() {
        this.nationManager = NationManager.getInstance();
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
            case "info" -> handleInfo(player, args);
            case "war" -> handleWar(player, args);
            case "peace" -> handlePeace(player, args);
            case "ally" -> handleAlly(player, args);
            case "trade" -> handleTrade(player, args);
            case "tech" -> handleTech(player, args);
            case "neutral" -> handleNeutral(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }
    private boolean handleInfo(Player player, String[] args) {
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

        player.sendMessage("§6========== " + nation.getName() + " 外交关系 ==========");

        List<DiplomacyRelation> relations = diplomacyManager.getNationRelations(nation.getName());
        if (relations.isEmpty()) {
            player.sendMessage("§7暂无外交关系");
        } else {
            for (DiplomacyRelation relation : relations) {
                String otherNation = relation.getNation1().equals(nation.getName()) ?
                        relation.getNation2() : relation.getNation1();
                player.sendMessage("§e" + otherNation + " §7- §f" + relation.getRelationType().getDisplayName());
            }
        }

        return true;
    }

    private boolean handleWar(Player player, String[] args) {
        if (!nationManager.hasPermission(player, NationPermission.DECLARE_WAR)) {
            player.sendMessage("§c你没有权限宣战！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /diplomacy war <国家名>");
            return true;
        }

        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        String targetName = args[1];
        if (targetName.equals(myNation.getName())) {
            player.sendMessage("§c不能对自己的国家宣战！");
            return true;
        }

        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        diplomacyManager.setRelation(myNation.getName(), targetName, RelationType.WAR);
        player.sendMessage("§a已向 " + targetName + " 宣战！");

        return true;
    }

    private boolean handlePeace(Player player, String[] args) {
        if (!nationManager.hasPermission(player, NationPermission.DECLARE_WAR)) {
            player.sendMessage("§c你没有权限进行和谈！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /diplomacy peace <国家名>");
            return true;
        }

        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        String targetName = args[1];
        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        diplomacyManager.setRelation(myNation.getName(), targetName, RelationType.NEUTRAL);
        player.sendMessage("§a已与 " + targetName + " 和平！");

        return true;
    }

    private boolean handleAlly(Player player, String[] args) {
        if (!nationManager.hasPermission(player, NationPermission.FORM_ALLIANCE)) {
            player.sendMessage("§c你没有权限结盟！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /diplomacy ally <国家名>");
            return true;
        }

        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        String targetName = args[1];
        if (targetName.equals(myNation.getName())) {
            player.sendMessage("§c不能与自己的国家结盟！");
            return true;
        }

        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        diplomacyManager.setRelation(myNation.getName(), targetName, RelationType.ALLIANCE_DEFENSIVE);
        player.sendMessage("§a已与 " + targetName + " 结盟！");

        return true;
    }

    private boolean handleTrade(Player player, String[] args) {
        if (!nationManager.hasPermission(player, NationPermission.PROPOSE_DIPLOMACY)) {
            player.sendMessage("§c你没有权限签订贸易协议！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /diplomacy trade <国家名>");
            return true;
        }

        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        String targetName = args[1];
        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        diplomacyManager.setRelation(myNation.getName(), targetName, RelationType.TRADE_AGREEMENT);
        player.sendMessage("§a已与 " + targetName + " 签订贸易协议！");

        return true;
    }

    private boolean handleTech(Player player, String[] args) {
        if (!nationManager.hasPermission(player, NationPermission.PROPOSE_DIPLOMACY)) {
            player.sendMessage("§c你没有权限签订科技协议！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /diplomacy tech <国家名>");
            return true;
        }

        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        String targetName = args[1];
        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        diplomacyManager.setRelation(myNation.getName(), targetName, RelationType.TECH_AGREEMENT);
        player.sendMessage("§a已与 " + targetName + " 签订科技协议！");

        return true;
    }

    private boolean handleNeutral(Player player, String[] args) {
        if (!nationManager.hasPermission(player, NationPermission.PROPOSE_DIPLOMACY)) {
            player.sendMessage("§c你没有权限设置中立关系！");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /diplomacy neutral <国家名>");
            return true;
        }

        Nation myNation = nationManager.getPlayerNation(player);
        if (myNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        String targetName = args[1];
        Nation targetNation = townyAPI.getNation(targetName);
        if (targetNation == null) {
            player.sendMessage("§c国家 " + targetName + " 不存在！");
            return true;
        }

        diplomacyManager.setRelation(myNation.getName(), targetName, RelationType.NEUTRAL);
        player.sendMessage("§a已与 " + targetName + " 设为中立关系！");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 外交命令 ==========");
        player.sendMessage("§e/diplomacy info [国家名] §7- 查看外交关系");
        player.sendMessage("§e/diplomacy war <国家名> §7- 宣战");
        player.sendMessage("§e/diplomacy peace <国家名> §7- 和谈");
        player.sendMessage("§e/diplomacy ally <国家名> §7- 结盟");
        player.sendMessage("§e/diplomacy trade <国家名> §7- 贸易协议");
        player.sendMessage("§e/diplomacy tech <国家名> §7- 科技协议");
        player.sendMessage("§e/diplomacy neutral <国家名> §7- 中立");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("info", "war", "peace", "ally", "trade", "tech", "neutral"));
        } else if (args.length == 2) {
            for (Nation nation : townyAPI.getNations()) {
                completions.add(nation.getName());
            }
        }

        return completions;
    }
}
