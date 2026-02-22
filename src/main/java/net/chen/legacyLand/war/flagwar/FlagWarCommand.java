package net.chen.legacyLand.war.flagwar;

import net.chen.legacyLand.LegacyLand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * FlagWar 命令处理器
 * /flagwar <info|list|cancel>
 */
public class FlagWarCommand implements CommandExecutor, TabCompleter {

    private final FlagWarManager flagWarManager;

    public FlagWarCommand() {
        this.flagWarManager = FlagWarManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(sender);
            case "list" -> handleList(sender);
            case "cancel" -> handleCancel(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * 显示当前玩家的 FlagWar 信息
     */
    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return;
        }

        FlagWarData flagWar = flagWarManager.getPlayerFlagWar(player.getUniqueId());
        if (flagWar == null) {
            player.sendMessage("§e你当前没有进行中的旗帜战争。");
            return;
        }

        player.sendMessage("§6===== 旗帜战争信息 =====");
        player.sendMessage("§e攻击方: §f" + flagWar.getAttackerNation() + " (" + flagWar.getAttackerTown() + ")");
        player.sendMessage("§e防守方: §f" + flagWar.getDefenderNation() + " (" + flagWar.getDefenderTown() + ")");
        player.sendMessage("§e目标地块: §f" + flagWar.getTownBlockCoords());
        player.sendMessage("§e主城方块: §f" + (flagWar.isHomeBlock() ? "是" : "否"));
        player.sendMessage("§e计时器进度: §f" + flagWar.getTimerProgress() + "%");
        player.sendMessage("§e状态: §f" + flagWar.getStatus().getDisplayName());
        player.sendMessage("§e放置费用: §f" + flagWar.getStakingFee() + " 金币");
        player.sendMessage("§e防守破坏费用: §f" + flagWar.getDefenseBreakFee() + " 金币");
    }

    /**
     * 列出所有活跃的 FlagWar
     */
    private void handleList(CommandSender sender) {
        Collection<FlagWarData> activeFlagWars = flagWarManager.getActiveFlagWars();

        if (activeFlagWars.isEmpty()) {
            sender.sendMessage("§e当前没有进行中的旗帜战争。");
            return;
        }

        sender.sendMessage("§6===== 活跃旗帜战争 (" + activeFlagWars.size() + ") =====");
        for (FlagWarData flagWar : activeFlagWars) {
            sender.sendMessage(String.format("§e%s §7→ §e%s §f[%s%%] 地块: %s",
                flagWar.getAttackerTown(),
                flagWar.getDefenderTown(),
                flagWar.getTimerProgress(),
                flagWar.getTownBlockCoords()
            ));
        }
    }

    /**
     * 取消当前玩家的 FlagWar
     */
    private void handleCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return;
        }

        FlagWarData flagWar = flagWarManager.getPlayerFlagWar(player.getUniqueId());
        if (flagWar == null) {
            player.sendMessage("§c你当前没有进行中的旗帜战争！");
            return;
        }

        flagWarManager.cancelFlagWar(flagWar);
        player.sendMessage("§a旗帜战争已取消。");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== FlagWar 命令帮助 =====");
        sender.sendMessage("§e/flagwar info §7- 查看当前旗帜战争信息");
        sender.sendMessage("§e/flagwar list §7- 列出所有活跃旗帜战争");
        sender.sendMessage("§e/flagwar cancel §7- 取消当前旗帜战争");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = List.of("info", "list", "cancel");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }
        return completions;
    }
}
