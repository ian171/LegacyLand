package net.chen.legacyLand.nation.law;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.nation.NationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /law 命令处理器
 */
public class LawCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§6[法令]§r ";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令。");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "enact"     -> handleEnact(player, args);
            case "propose"   -> handlePropose(player, args);
            case "vote"      -> handleVote(player, args);
            case "repeal"    -> handleRepeal(player, args);
            case "list"      -> handleList(player, args);
            case "proposals" -> handleProposals(player, args);
            default          -> sendHelp(player);
        }
        return true;
    }

    // /law enact <type> [duration_hours] [key=value ...]
    private void handleEnact(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + "用法: /law enact <类型> [持续小时] [参数...]");
            return;
        }
        LawType type = parseLawType(player, args[1]);
        if (type == null) return;

        long duration = 24;
        String paramsJson = "{}";
        if (args.length >= 3) {
            try { duration = Long.parseLong(args[2]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 4) {
            paramsJson = buildParamsJson(Arrays.copyOfRange(args, 3, args.length));
        }

        Nation nation = NationManager.getInstance().getPlayerNation(player);
        if (nation == null) { player.sendMessage(PREFIX + "§c你不属于任何国家。"); return; }

        LawManager.EnactResult result = LawManager.getInstance()
                .enactLaw(player, nation.getName(), type, paramsJson, duration);
        switch (result) {
            case SUCCESS      -> player.sendMessage(PREFIX + "§a法令 §e" + type.getDisplayName() + " §a已颁布，持续 " + duration + " 小时。");
            case NO_PERMISSION -> player.sendMessage(PREFIX + "§c你没有颁布法令的权限。");
            case INVALID_PARAMS -> player.sendMessage(PREFIX + "§c参数无效。");
            case DB_ERROR     -> player.sendMessage(PREFIX + "§c数据库错误，请联系管理员。");
        }
    }

    // /law propose <type> [vote_hours] [key=value ...]
    private void handlePropose(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + "用法: /law propose <类型> [投票小时] [参数...]");
            return;
        }
        LawType type = parseLawType(player, args[1]);
        if (type == null) return;

        long voteHours = 24;
        String paramsJson = "{}";
        if (args.length >= 3) {
            try { voteHours = Long.parseLong(args[2]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 4) {
            paramsJson = buildParamsJson(Arrays.copyOfRange(args, 3, args.length));
        }

        Nation nation = NationManager.getInstance().getPlayerNation(player);
        if (nation == null) { player.sendMessage(PREFIX + "§c你不属于任何国家。"); return; }

        LawManager.ProposeResult result = LawManager.getInstance()
                .proposeLaw(player, nation.getName(), type, paramsJson, voteHours);
        switch (result) {
            case SUCCESS      -> player.sendMessage(PREFIX + "§a提案 §e" + type.getDisplayName() + " §a已提交，投票截止 " + voteHours + " 小时后。");
            case NO_PERMISSION -> player.sendMessage(PREFIX + "§c你没有提交提案的权限。");
            case INVALID_PARAMS -> player.sendMessage(PREFIX + "§c参数无效。");
            case DB_ERROR     -> player.sendMessage(PREFIX + "§c数据库错误，请联系管理员。");
        }
    }

    // /law vote <proposalId> <yes|no>
    private void handleVote(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + "用法: /law vote <提案ID> <yes|no>");
            return;
        }
        boolean approve = args[2].equalsIgnoreCase("yes") || args[2].equalsIgnoreCase("y");
        LawManager.VoteResult result = LawManager.getInstance().castVote(player, args[1], approve);
        switch (result) {
            case SUCCESS      -> player.sendMessage(PREFIX + "§a投票成功：" + (approve ? "§a赞成" : "§c反对"));
            case NOT_FOUND    -> player.sendMessage(PREFIX + "§c找不到该提案。");
            case ALREADY_VOTED -> player.sendMessage(PREFIX + "§c你已经投过票了。");
            case NO_PERMISSION -> player.sendMessage(PREFIX + "§c你没有投票权限。");
            case CLOSED       -> player.sendMessage(PREFIX + "§c该提案已关闭。");
        }
    }

    // /law repeal <lawId>
    private void handleRepeal(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + "用法: /law repeal <法令ID>");
            return;
        }
        Nation nation = NationManager.getInstance().getPlayerNation(player);
        if (nation == null) { player.sendMessage(PREFIX + "§c你不属于任何国家。"); return; }

        boolean ok = LawManager.getInstance().repealLaw(player, nation.getName(), args[1]);
        player.sendMessage(ok ? PREFIX + "§a法令已废除。" : PREFIX + "§c废除失败，请检查法令ID或权限。");
    }

    // /law list [nation]
    private void handleList(Player player, String[] args) {
        String nationName = resolveNationName(player, args, 1);
        if (nationName == null) return;

        List<ActiveLaw> laws = LawManager.getInstance().getActiveLaws(nationName);
        if (laws.isEmpty()) {
            player.sendMessage(PREFIX + nationName + " 当前没有生效的法令。");
            return;
        }
        player.sendMessage(PREFIX + "§e" + nationName + " 的生效法令：");
        for (ActiveLaw law : laws) {
            String expiry = law.isPermanent() ? "永久" :
                    "到期: " + new java.util.Date(law.expiresAt());
            player.sendMessage("  §7[" + law.id().substring(0, 8) + "] §f" +
                    law.type().getDisplayName() + " §8(" + expiry + ")");
        }
    }

    // /law proposals [nation]
    private void handleProposals(Player player, String[] args) {
        String nationName = resolveNationName(player, args, 1);
        if (nationName == null) return;

        List<LawProposal> proposals = LawManager.getInstance().getOpenProposals(nationName);
        if (proposals.isEmpty()) {
            player.sendMessage(PREFIX + nationName + " 当前没有开放的提案。");
            return;
        }
        player.sendMessage(PREFIX + "§e" + nationName + " 的开放提案：");
        for (LawProposal p : proposals) {
            player.sendMessage("  §7[" + p.getId().substring(0, 8) + "] §f" +
                    p.getType().getDisplayName() +
                    " §a赞成:" + p.getApproveCount() + " §c反对:" + p.getRejectCount());
        }
    }

    // ===================== 辅助方法 =====================

    private LawType parseLawType(Player player, String name) {
        try {
            return LawType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(PREFIX + "§c未知法令类型: " + name);
            player.sendMessage(PREFIX + "可用类型: " + Arrays.stream(LawType.values())
                    .map(LawType::name).collect(Collectors.joining(", ")));
            return null;
        }
    }

    /** 将 key=value 参数数组转为简单 JSON */
    private String buildParamsJson(String[] kvArgs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kvArgs.length; i++) {
            String[] kv = kvArgs[i].split("=", 2);
            if (kv.length != 2) continue;
            if (sb.length() > 1) sb.append(",");
            String val = kv[1];
            try {
                Double.parseDouble(val);
                sb.append("\"").append(kv[0]).append("\":").append(val);
            } catch (NumberFormatException e) {
                sb.append("\"").append(kv[0]).append("\":\"").append(val).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String resolveNationName(Player player, String[] args, int idx) {
        if (args.length > idx) {
            Nation n = TownyAPI.getInstance().getNation(args[idx]);
            if (n == null) { player.sendMessage(PREFIX + "§c找不到国家: " + args[idx]); return null; }
            return n.getName();
        }
        Nation n = NationManager.getInstance().getPlayerNation(player);
        if (n == null) { player.sendMessage(PREFIX + "§c你不属于任何国家，请指定国家名。"); return null; }
        return n.getName();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6===== 法令系统 =====");
        player.sendMessage("§e/law enact <类型> [小时] [参数]  §7- 颁布法令");
        player.sendMessage("§e/law propose <类型> [小时] [参数] §7- 提交提案");
        player.sendMessage("§e/law vote <提案ID> <yes|no>      §7- 投票");
        player.sendMessage("§e/law repeal <法令ID>             §7- 废除法令");
        player.sendMessage("§e/law list [国家]                 §7- 查看生效法令");
        player.sendMessage("§e/law proposals [国家]            §7- 查看开放提案");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enact", "propose", "vote", "repeal", "list", "proposals");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("enact") || args[0].equalsIgnoreCase("propose"))) {
            return Arrays.stream(LawType.values()).map(LawType::name)
                    .filter(n -> n.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("vote")) {
            return Arrays.asList("yes", "no");
        }
        return Collections.emptyList();
    }
}
