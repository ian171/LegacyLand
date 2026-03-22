package net.chen.legacyLand.player.commands;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import net.chen.legacyLand.player.Profession;
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
 * 玩家命令处理器
 */
public class PlayerCommand implements CommandExecutor, TabCompleter {

    private final PlayerManager playerManager;

    public PlayerCommand() {
        this.playerManager = PlayerManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(player);
            case "profession" -> handleProfession(player, args);
            case "stats" -> handleStats(player);
            default -> showHelp(player);
        }

        return true;
    }

    /**
     * 显示帮助信息
     */
    private void showHelp(Player player) {
        player.sendMessage(LanguageManager.getInstance().translate("player.help_header"));
        player.sendMessage("§e/player info §7- 查看个人信息");
        player.sendMessage("§e/player profession <main|sub> <职业> §7- 选择职业");
        player.sendMessage("§e/player stats §7- 查看详细属性");
    }

    /**
     * 查看个人信息
     */
    private void handleInfo(Player player) {
        PlayerData data = playerManager.getPlayerData(player);
        if (data == null) {
            player.sendMessage("§c未找到玩家数据！");
            return;
        }

        player.sendMessage("§6========== 个人信息 ==========");
        player.sendMessage("§e玩家: §f" + data.getPlayerName());
        player.sendMessage("§e最大血量: §f" + data.getMaxHealth() + " ❤");
        player.sendMessage("§e饮水值: §f" + data.getHydration() + " 💧");
        player.sendMessage("§e体温: §f" + String.format("%.1f", data.getTemperature()) + "°C");

        if (data.getMainProfession() != null) {
            player.sendMessage("§e主职业: §f" + data.getMainProfession().getDisplayName() +
                             " §7(Lv." + data.getMainProfessionLevel() + ")");
        } else {
            player.sendMessage("§e主职业: §7未选择");
        }

        if (data.getSubProfession() != null) {
            player.sendMessage("§e副职业: §f" + data.getSubProfession().getDisplayName() +
                             " §7(Lv." + data.getSubProfessionLevel() + ")");
        } else {
            player.sendMessage("§e副职业: §7未选择");
        }

        player.sendMessage("§e天赋点: §f" + data.getTalentPoints());
    }

    /**
     * 选择职业
     */
    private void handleProfession(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /player profession <main|sub> <职业>");
            return;
        }

        PlayerData data = playerManager.getPlayerData(player);
        if (data == null) {
            player.sendMessage("§c未找到玩家数据！");
            return;
        }

        String type = args[1].toLowerCase();
        String professionName = args[2].toUpperCase();

        try {
            Profession profession = Profession.valueOf(professionName);

            if (type.equals("main")) {
                if (playerManager.setMainProfession(player.getUniqueId(), profession)) {
                    player.sendMessage("§a成功选择主职业: " + profession.getDisplayName());
                    player.setMaxHealth(data.getMaxHealth());
                } else {
                    player.sendMessage("§c你已经选择过主职业了！");
                }
            } else if (type.equals("sub")) {
                if (playerManager.setSubProfession(player.getUniqueId(), profession)) {
                    player.sendMessage("§a成功选择副职业: " + profession.getDisplayName());
                } else {
                    player.sendMessage("§c你还不能选择副职业！需要主职业达到20级。");
                }
            } else {
                player.sendMessage("§c无效的职业类型！使用 main 或 sub");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的职业名称！");
            showProfessions(player);
        }
    }

    /**
     * 显示所有职业
     */
    private void showProfessions(Player player) {
        player.sendMessage("§6可用职业:");
        for (Profession profession : Profession.values()) {
            player.sendMessage("§e- " + profession.name() + " §7(" + profession.getDisplayName() + ")");
        }
    }

    /**
     * 查看详细属性
     */
    private void handleStats(Player player) {
        PlayerData data = playerManager.getPlayerData(player);
        if (data == null) {
            player.sendMessage("§c未找到玩家数据！");
            return;
        }

        player.sendMessage("§6========== 详细属性 ==========");
        player.sendMessage("§e最大血量: §f" + data.getMaxHealth() + " ❤");
        player.sendMessage("§e饮水值: §f" + data.getHydration() + "/10 💧");
        player.sendMessage("§e体温: §f" + String.format("%.1f", data.getTemperature()) + "°C");

        if (data.getMainProfession() != null) {
            Profession main = data.getMainProfession();
            player.sendMessage("§e主职业: §f" + main.getDisplayName());
            player.sendMessage("  §7等级: " + data.getMainProfessionLevel());
            player.sendMessage("  §7经验: " + data.getMainProfessionExp());
            player.sendMessage("  §7攻击加成: +" + main.getAttackBonus());
            player.sendMessage("  §7防御加成: +" + main.getDefenseBonus());
        }

        if (data.getSubProfession() != null) {
            Profession sub = data.getSubProfession();
            player.sendMessage("§e副职业: §f" + sub.getDisplayName());
            player.sendMessage("  §7等级: " + data.getSubProfessionLevel() + "/5");
            player.sendMessage("  §7经验: " + data.getSubProfessionExp() + "/500");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("info", "profession", "stats"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("profession")) {
            completions.addAll(Arrays.asList("main", "sub"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("profession")) {
            for (Profession profession : Profession.values()) {
                completions.add(profession.name());
            }
        }

        return completions;
    }
}
