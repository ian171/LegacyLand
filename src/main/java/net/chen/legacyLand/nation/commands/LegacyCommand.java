package net.chen.legacyLand.nation.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.events.PlayerObtainsRoleEvent;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationRole;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LegacyLand 扩展命令 - 用于设置政体、角色等扩展功能
 */
public class LegacyCommand implements CommandExecutor, TabCompleter {

    private final NationManager nationManager;
    private final TownyAPI townyAPI;

    public LegacyCommand() {
        this.nationManager = NationManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "government" -> handleGovernment(player, args);
            case "role" -> handleRole(player, args);
            case "info" -> handleInfo(player);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * 设置国家政体
     */
    private void handleGovernment(Player player, String[] args) {
        Resident resident = townyAPI.getResident(player);
        if (resident == null || !resident.hasNation()) {
            player.sendMessage("§c你不在任何国家中！");
            return;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage("§c无法获取国家信息！");
            return;
        }

        // 检查是否是国家领袖
        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国家领袖才能设置政体！");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c用法: /legacy government <FEUDAL|REPUBLIC>");
            player.sendMessage("§eFEUDAL - 分封制（国王制）");
            player.sendMessage("§eREPUBLIC - 城市共和制");
            return;
        }

        GovernmentType govType;
        try {
            govType = GovernmentType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的政体类型！请使用 FEUDAL 或 REPUBLIC");
            return;
        }

        nationManager.setGovernmentType(nation.getName(), govType);
        player.sendMessage("§a成功设置国家政体为: " + govType.getDisplayName());
    }

    /**
     * 设置玩家角色
     */
    private void handleRole(Player player, String[] args) {
        Resident resident = townyAPI.getResident(player);
        if (resident == null || !resident.hasNation()) {
            player.sendMessage("§c你不在任何国家中！");
            return;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage("§c无法获取国家信息！");
            return;
        }

        // 检查是否是国家领袖
        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国家领袖才能设置角色！");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§c用法: /legacy role <玩家名> <角色>");
            player.sendMessage("§c分封制角色: CHANCELLOR, ATTORNEY_GENERAL, MINISTER_OF_JUSTICE, MINISTER_OF_DEFENSE");
            player.sendMessage("§c共和制角色: FINANCE_OFFICER, JUDICIAL_OFFICER, LEGAL_OFFICER, MILITARY_SUPPLY_OFFICER, PARLIAMENT_MEMBER");
            return;
        }

        Resident target = townyAPI.getResident(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到玩家: " + args[1]);
            return;
        }

        if (!target.hasNation() || !target.getNationOrNull().equals(nation)) {
            player.sendMessage("§c该玩家不在你的国家中！");
            return;
        }

        NationRole role;
        try {
            role = NationRole.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的角色类型！");
            return;
        }

        if (role.isLeader()) {
            player.sendMessage("§c无法设置领袖角色！领袖由 Towny 的 /nation set king 命令管理。");
            return;
        }

        nationManager.setPlayerRole(nation.getName(), target.getUUID(), role);
        PlayerObtainsRoleEvent event = new PlayerObtainsRoleEvent(nation,role,player);
        event.callEvent();
        player.sendMessage("§a成功设置 " + args[1] + " 的角色为: " + role.getDisplayName());
    }

    /**
     * 查看国家扩展信息
     */
    private void handleInfo(Player player) {
        Resident resident = townyAPI.getResident(player);
        if (resident == null || !resident.hasNation()) {
            player.sendMessage("§c你不在任何国家中！");
            return;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage("§c无法获取国家信息！");
            return;
        }

        GovernmentType govType = nationManager.getGovernmentType(nation.getName());
        NationRole role = nationManager.getPlayerRole(nation.getName(), player.getUniqueId());

        player.sendMessage("§6========== " + nation.getName() + " 扩展信息 ==========");
        player.sendMessage("§e政体: §f" + (govType != null ? govType.getDisplayName() : "未设置"));
        player.sendMessage("§e你的角色: §f" + (role != null ? role.getDisplayName() : "公民"));
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== LegacyLand 命令 ==========");
        player.sendMessage("§e/legacy government <政体> §7- 设置国家政体");
        player.sendMessage("§e/legacy role <玩家> <角色> §7- 设置玩家角色");
        player.sendMessage("§e/legacy info §7- 查看国家扩展信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("government", "role", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("government")) {
            completions.addAll(Arrays.asList("FEUDAL", "REPUBLIC"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("role")) {
            for (NationRole role : NationRole.values()) {
                if (!role.isLeader()) {
                    completions.add(role.name());
                }
            }
        }

        return completions;
    }
}
