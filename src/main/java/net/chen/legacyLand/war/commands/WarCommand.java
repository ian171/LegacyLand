package net.chen.legacyLand.war.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationPermission;
import net.chen.legacyLand.war.*;
import net.chen.legacyLand.war.siege.SiegeWar;
import net.chen.legacyLand.war.siege.SiegeWarManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 战争命令处理器
 */
public class WarCommand implements CommandExecutor, TabCompleter {

    private final WarManager warManager;
    private final SiegeWarManager siegeWarManager;
    private final NationManager nationManager;
    private final TownyAPI townyAPI;

    public WarCommand() {
        this.warManager = WarManager.getInstance();
        this.siegeWarManager = SiegeWarManager.getInstance();
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
            case "start":
                return handleStart(player, args);
            case "info":
                return handleInfo(player, args);
            case "join":
                return handleJoin(player, args);
            case "surrender":
                return handleSurrender(player);
            case "peace":
                return handlePeace(player);
            case "list":
                return handleList(player);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleStart(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /war start <敌方城镇名> <战役名称>");
            return true;
        }

        // 检查权限
        if (!nationManager.hasPermission(player, NationPermission.DECLARE_WAR)) {
            player.sendMessage("§c你没有权限发动战争！");
            return true;
        }

        // 获取玩家所在城镇
        Town playerTown = townyAPI.getTown(player);
        if (playerTown == null) {
            player.sendMessage("§c你不在任何城镇中！");
            return true;
        }

        // 获取玩家所在国家
        Nation playerNation = nationManager.getPlayerNation(player);
        if (playerNation == null) {
            player.sendMessage("§c你不在任何国家中！");
            return true;
        }

        // 获取目标城镇
        String targetTownName = args[1];
        Town targetTown = townyAPI.getTown(targetTownName);
        if (targetTown == null) {
            player.sendMessage("§c城镇 " + targetTownName + " 不存在！");
            return true;
        }

        // 获取目标国家
        Nation targetNation = targetTown.getNationOrNull();
        if (targetNation == null) {
            player.sendMessage("§c目标城镇不属于任何国家！");
            return true;
        }

        // 检查是否接壤
        if (!siegeWarManager.areTownsAdjacent(playerTown.getName(), targetTownName)) {
            player.sendMessage("§c两个城镇不接壤，无法发动攻城战！");
            return true;
        }
        String warName = args[2];
        // 创建战争
        War war = warManager.createWar(
            WarType.EXTERNAL, warName,
            playerNation.getName(),
            targetNation.getName(),
            playerTown.getName(),
            targetTownName
        );

        // 创建攻城战
        SiegeWar siegeWar = siegeWarManager.createSiegeWar(
            warName,
            playerTown.getName(),
            targetTownName
        );


        player.sendMessage("§a战争已创建！战争ID: §f" + args[2]);
        player.sendMessage("§e请先建立前哨战，维持1小时后才能正式开战。");
        player.sendMessage("§e使用 /siege outpost 建立前哨战。");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        player.sendMessage("§6========== 战争信息 ==========");
        player.sendMessage("§e战争ID: §f" + war.getWarName());
        player.sendMessage("§e类型: §f" + war.getType().getDisplayName());
        player.sendMessage("§e状态: §f" + war.getStatus().getDisplayName());
        player.sendMessage("§e攻方: §f" + war.getAttackerNation() + " (" + war.getAttackerTown() + ")");
        player.sendMessage("§e守方: §f" + war.getDefenderNation() + " (" + war.getDefenderTown() + ")");
        player.sendMessage("§e攻方活跃人数: §f" + war.getActiveAttackerCount());
        player.sendMessage("§e守方活跃人数: §f" + war.getActiveDefenderCount());

        if (war.getStatus().isEnded()) {
            player.sendMessage("§e胜者: §f" + (war.getWinner() != null ? war.getWinner() : "平局"));
        }

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /war join <战争ID> <角色>");
            player.sendMessage("§c角色: soldier(战士), logistics(后勤), scout(侦查)");
            return true;
        }

        String warId = args[1];
        War war = warManager.getWar(warId);

        if (war == null) {
            player.sendMessage("§c战争不存在！");
            return true;
        }

        if (war.getStatus().isEnded()) {
            player.sendMessage("§c战争已结束！");
            return true;
        }

        // 解析角色
        WarRole role;
        try {
            role = WarRole.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的角色！可用: soldier, logistics, scout");
            return true;
        }

        // 获取玩家所在城镇
        Town playerTown = townyAPI.getTown(player);
        if (playerTown == null) {
            player.sendMessage("§c你不在任何城镇中！");
            return true;
        }

        // 判断是攻方还是守方
        boolean isAttacker = playerTown.getName().equals(war.getAttackerTown());
        boolean isDefender = playerTown.getName().equals(war.getDefenderTown());

        if (!isAttacker && !isDefender) {
            player.sendMessage("§c你的城镇不参与此战争！");
            return true;
        }

        // 创建参与者（默认10份补给）
        WarParticipant participant = new WarParticipant(
            player.getUniqueId(),
            player.getName(),
            playerTown.getName(),
            role,
            10
        );

        warManager.addParticipant(warId, participant, isAttacker);

        player.sendMessage("§a你已加入战争！角色: §f" + role.getDisplayName());
        player.sendMessage("§e初始补给: §f10");

        return true;
    }

    private boolean handleSurrender(Player player) {
        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        if (!nationManager.hasPermission(player, NationPermission.DECLARE_WAR)) {
            player.sendMessage("§c你没有权限投降！");
            return true;
        }

        // 判断是攻方还是守方
        boolean isAttacker = war.getAttackers().containsKey(player.getUniqueId());

        if (warManager.surrender(war.getWarName(), isAttacker)) {
            player.sendMessage("§c你的一方已投降！");
            String winner = isAttacker ? war.getDefenderNation() : war.getAttackerNation();
            player.sendMessage("§e胜者: §f" + winner);
        } else {
            player.sendMessage("§c投降失败！");
        }

        return true;
    }

    private boolean handlePeace(Player player) {
        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        if (!nationManager.hasPermission(player, NationPermission.DECLARE_WAR)) {
            player.sendMessage("§c你没有权限进行和谈！");
            return true;
        }

        if (warManager.makePeace(war.getWarName())) {
            player.sendMessage("§a战争以平局结束！");
        } else {
            player.sendMessage("§c和谈失败！");
        }

        return true;
    }

    private boolean handleList(Player player) {
        List<War> activeWars = new ArrayList<>(warManager.getActiveWars());

        if (activeWars.isEmpty()) {
            player.sendMessage("§e当前没有进行中的战争。");
            return true;
        }

        player.sendMessage("§6========== 进行中的战争 ==========");
        for (War war : activeWars) {
            player.sendMessage("§e" + war.getAttackerNation() + " §cvs §e" + war.getDefenderNation());
            player.sendMessage("  §7ID: " + war.getWarName());
            player.sendMessage("  §7状态: " + war.getStatus().getDisplayName());
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 战争命令帮助 ==========");
        player.sendMessage("§e/war start <城镇名> §7- 发动战争");
        player.sendMessage("§e/war info §7- 查看战争信息");
        player.sendMessage("§e/war join <战争ID> <角色> §7- 加入战争");
        player.sendMessage("§e/war surrender §7- 投降");
        player.sendMessage("§e/war peace §7- 和谈（平局）");
        player.sendMessage("§e/war list §7- 列出所有战争");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "info", "join", "surrender", "peace", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            // 列出所有城镇
            townyAPI.getTowns().forEach(town -> completions.add(town.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("join")) {
            completions.addAll(Arrays.asList("soldier", "logistics", "scout"));
        }

        return completions;
    }
}
