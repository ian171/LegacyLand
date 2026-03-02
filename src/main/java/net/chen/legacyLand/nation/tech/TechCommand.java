package net.chen.legacyLand.nation.tech;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationRole;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /tech 命令处理器
 */
public class TechCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§b[科技]§r ";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令。");
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "research" -> handleResearch(player, args);
            case "list"     -> handleList(player, args);
            case "info"     -> handleInfo(player, args);
            case "status"   -> handleStatus(player);
            default         -> sendHelp(player);
        }
        return true;
    }

    // /tech research <nodeId>
    private void handleResearch(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + "用法: /tech research <科技ID>");
            return;
        }
        Nation nation = NationManager.getInstance().getPlayerNation(player);
        if (nation == null) { player.sendMessage(PREFIX + "§c你不属于任何国家。"); return; }
        NationRole role = NationManager.getInstance().getPlayerRole(nation.getName(), player.getUniqueId());
        if (role == null || !role.isLeader()) {
            player.sendMessage(PREFIX + "§c只有国家领导人才能研究科技。");
            return;
        }

        TechManager.ResearchResult result = TechManager.getInstance()
                .research(nation.getName(), args[1]);
        switch (result) {
            case SUCCESS -> {
                TechNode node = TechManager.getInstance().getNodeDefinitions().get(args[1]);
                player.sendMessage(PREFIX + "§a科技 §e" + (node != null ? node.displayName() : args[1]) + " §a研究完成！");
            }
            case NOT_FOUND            -> player.sendMessage(PREFIX + "§c找不到科技节点: " + args[1]);
            case ALREADY_COMPLETED    -> player.sendMessage(PREFIX + "§c该科技已经研究完成。");
            case PREREQUISITES_NOT_MET -> player.sendMessage(PREFIX + "§c前置科技未完成。");
            case INSUFFICIENT_POINTS  -> player.sendMessage(PREFIX + "§c研究点不足。");
            case LINE_LOCKED          -> player.sendMessage(PREFIX + "§c你的政体无法研究该科技线。");
            case NO_NATION            -> player.sendMessage(PREFIX + "§c国家不存在。");
        }
    }

    // /tech list [line]
    private void handleList(Player player, String[] args) {
        Nation nation = NationManager.getInstance().getPlayerNation(player);
        String nationName = nation != null ? nation.getName() : null;
        TechManager mgr = TechManager.getInstance();

        Map<String, List<TechNode>> lines = mgr.getLineNodes();
        String filterLine = args.length >= 2 ? args[1].toUpperCase() : null;

        player.sendMessage(PREFIX + "§e===== 科技树 =====");
        for (Map.Entry<String, List<TechNode>> entry : lines.entrySet()) {
            if (filterLine != null && !entry.getKey().equalsIgnoreCase(filterLine)) continue;
            boolean locked = nationName != null && mgr.isLineLocked(nationName, entry.getKey());
            player.sendMessage("§6[" + entry.getKey() + "]" + (locked ? " §c(政体锁定)" : ""));
            for (TechNode node : entry.getValue()) {
                TechStatus status = nationName != null
                        ? mgr.getNodeStatus(nationName, node.id())
                        : TechStatus.LOCKED;
                String statusColor = switch (status) {
                    case COMPLETED -> "§a✔ ";
                    case AVAILABLE -> "§e○ ";
                    case LOCKED    -> "§8✗ ";
                };
                player.sendMessage("  " + statusColor + "§f" + node.displayName() +
                        " §8(费用:" + node.cost() + "点)");
            }
        }
    }

    // /tech info <nodeId>
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(PREFIX + "用法: /tech info <科技ID>"); return; }
        TechNode node = TechManager.getInstance().getNodeDefinitions().get(args[1]);
        if (node == null) { player.sendMessage(PREFIX + "§c找不到科技节点: " + args[1]); return; }

        player.sendMessage(PREFIX + "§e" + node.displayName() + " §8[" + node.id() + "]");
        player.sendMessage("  §7" + node.description());
        player.sendMessage("  §f费用: §e" + node.cost() + " 研究点");
        if (!node.prerequisites().isEmpty()) {
            player.sendMessage("  §f前置: §7" + String.join(", ", node.prerequisites()));
        }
        if (node.effects() != null && !node.effects().isEmpty()) {
            player.sendMessage("  §f效果:");
            node.effects().forEach((k, v) -> player.sendMessage("    §a" + k + ": §f+" + v));
        }
    }

    // /tech status
    private void handleStatus(Player player) {
        Nation nation = NationManager.getInstance().getPlayerNation(player);
        if (nation == null) { player.sendMessage(PREFIX + "§c你不属于任何国家。"); return; }

        NationTechState state = TechManager.getInstance().getState(nation.getName());
        int points = state != null ? state.getResearchPoints() : 0;
        int completed = state != null ? state.getCompletedNodes().size() : 0;
        int total = TechManager.getInstance().getNodeDefinitions().size();

        player.sendMessage(PREFIX + "§e" + nation.getName() + " 的科技状态");
        player.sendMessage("  §f研究点: §e" + points);
        player.sendMessage("  §f已完成: §a" + completed + " §f/ §7" + total + " 节点");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§b===== 科技树系统 =====");
        player.sendMessage("§e/tech research <科技ID>  §7- 研究科技");
        player.sendMessage("§e/tech list [科技线]      §7- 查看科技树");
        player.sendMessage("§e/tech info <科技ID>      §7- 查看节点详情");
        player.sendMessage("§e/tech status             §7- 查看研究进度");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Arrays.asList("research", "list", "info", "status");
        if (args.length == 2 && (args[0].equalsIgnoreCase("research") || args[0].equalsIgnoreCase("info"))) {
            TechManager mgr = TechManager.getInstance();
            if (mgr == null) return Collections.emptyList();
            return mgr.getNodeDefinitions().keySet().stream()
                    .filter(id -> id.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            TechManager mgr = TechManager.getInstance();
            if (mgr == null) return Collections.emptyList();
            return new ArrayList<>(mgr.getLineNodes().keySet());
        }
        return Collections.emptyList();
    }
}
