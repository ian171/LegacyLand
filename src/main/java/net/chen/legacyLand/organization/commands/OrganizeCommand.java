package net.chen.legacyLand.organization.commands;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.organization.*;
import net.chen.legacyLand.organization.outpost.Outpost;
import net.chen.legacyLand.organization.outpost.OutpostGoods;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 组织命令处理器
 * /legacy organize <子命令>
 */
public class OrganizeCommand implements CommandExecutor, TabCompleter {

    private final OrganizationManager orgManager;

    public OrganizeCommand() {
        this.orgManager = OrganizationManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        if (args.length < 2) {
            sendHelp(player);
            return true;
        }
        // 期望调用格式: /legacy organize <subcommand> [args...]
        String sub = args[1].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 2, args.length);

        switch (sub) {
            case "new"      -> handleNew(player, subArgs);
            case "disband"  -> handleDisband(player);
            case "invite"   -> handleInvite(player, subArgs);
            case "kick"     -> handleKick(player, subArgs);
            case "role"     -> handleRole(player, subArgs);
            case "perm"     -> handlePerm(player, subArgs);
            case "info"     -> handleInfo(player, subArgs);
            case "list"     -> handleList(player);
            default         -> sendHelp(player);
        }
        return true;
    }

    // ===================== 子命令处理 =====================

    private void handleNew(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§c用法: /legacy organize new <名称>");
            return;
        }
        String name = args[0];
        if (name.length() > 20) {
            player.sendMessage("§c组织名称不能超过20个字符！");
            return;
        }
        var result = orgManager.createOrganization(player, name);
        switch (result) {
            case SUCCESS ->
                    player.sendMessage("§a成功创建组织 §f" + name + " §a！花费 "
                            + LegacyLand.getInstance().getConfig().getDouble("organization.create-cost", 500.0) + " 金币。");
            case NAME_EXISTS ->
                    player.sendMessage("§c组织名称 §f" + name + " §c已存在！");
            case ALREADY_IN_ORG ->
                    player.sendMessage("§c你已经在一个组织中！请先退出。");
            case INSUFFICIENT_FUNDS ->
                    player.sendMessage("§c余额不足！创建组织需要 "
                            + LegacyLand.getInstance().getConfig().getDouble("organization.create-cost", 500.0) + " 金币。");
        }
    }

    private void handleDisband(Player player) {
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }
        if (!org.isLeader(player.getUniqueId())) {
            player.sendMessage("§c只有组织领导者才能解散组织！");
            return;
        }
        boolean ok = orgManager.disbandOrganization(player);
        if (ok) player.sendMessage("§a组织 §f" + org.getName() + " §a已解散。");
        else player.sendMessage("§c解散失败！");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage("§c用法: /legacy organize invite <玩家> [角色]"); return; }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }

        OrganizationMember self = org.getMember(player.getUniqueId());
        if (!self.hasPermission(OrganizationPermission.MANAGE_OUTPOST_MEMBERS) && !org.isLeader(player.getUniqueId())) {
            player.sendMessage("§c你没有邀请成员的权限！"); return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendMessage("§c玩家 §f" + args[0] + " §c不在线！"); return; }
        if (target.equals(player)) { player.sendMessage("§c不能邀请自己！"); return; }

        OrganizationRole role = OrganizationRole.MEMBER;
        if (args.length >= 2) {
            try { role = OrganizationRole.valueOf(args[1].toUpperCase()); }
            catch (IllegalArgumentException e) { player.sendMessage("§c无效角色！可选: MANAGER, MEMBER"); return; }
            if (role == OrganizationRole.LEADER) { player.sendMessage("§c不能邀请为领导者！"); return; }
        }

        boolean ok = orgManager.inviteMember(org, target, role);
        if (ok) {
            player.sendMessage("§a已邀请 §f" + target.getName() + " §a以 §f" + role.getDisplayName() + " §a身份加入组织 §f" + org.getName());
            target.sendMessage("§a你已加入组织 §f" + org.getName() + " §a，角色: §f" + role.getDisplayName());
        } else {
            player.sendMessage("§c邀请失败！玩家可能已在其他组织中，或不满足国家组织条件。");
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage("§c用法: /legacy organize kick <玩家>"); return; }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }

        OrganizationMember self = org.getMember(player.getUniqueId());
        if (!self.hasPermission(OrganizationPermission.MANAGE_OUTPOST_MEMBERS) && !org.isLeader(player.getUniqueId())) {
            player.sendMessage("§c你没有踢出成员的权限！"); return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        UUID targetId = target != null ? target.getUniqueId() : null;
        if (targetId == null) {
            // 尝试通过离线玩家查找
            var offline = Bukkit.getOfflinePlayer(args[0]);
            targetId = offline.hasPlayedBefore() ? offline.getUniqueId() : null;
        }
        if (targetId == null) { player.sendMessage("§c找不到玩家 §f" + args[0]); return; }
        if (!org.isMember(targetId)) { player.sendMessage("§c该玩家不在你的组织中！"); return; }
        if (org.isLeader(targetId)) { player.sendMessage("§c不能踢出领导者！"); return; }

        orgManager.kickMember(org, targetId);
        player.sendMessage("§a已将 §f" + args[0] + " §a从组织中移除。");
        if (target != null && target.isOnline()) {
            target.sendMessage("§c你已被移出组织 §f" + org.getName());
        }
    }

    private void handleRole(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage("§c用法: /legacy organize role <玩家> <MANAGER|MEMBER>"); return; }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null || !org.isLeader(player.getUniqueId())) {
            player.sendMessage("§c只有领导者可以修改成员角色！"); return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        UUID targetId = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer(args[0]).getUniqueId();
        if (!org.isMember(targetId)) { player.sendMessage("§c该玩家不在你的组织中！"); return; }
        OrganizationRole role;
        try { role = OrganizationRole.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException e) { player.sendMessage("§c无效角色！可选: MANAGER, MEMBER"); return; }
        if (role == OrganizationRole.LEADER) { player.sendMessage("§c不能通过此命令设置领导者！"); return; }

        orgManager.setMemberRole(org, targetId, role);
        player.sendMessage("§a已将 §f" + args[0] + " §a的角色设为 §f" + role.getDisplayName());
        if (target != null) target.sendMessage("§a你在组织 §f" + org.getName() + " §a中的角色已变更为 §f" + role.getDisplayName());
    }

    private void handlePerm(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /legacy organize perm <玩家> <权限> <grant|revoke>");
            player.sendMessage("§7权限列表: DELETE_OUTPOST, TRANSFER_OUTPOST, CLOSE_OUTPOST,");
            player.sendMessage("§7         ADD_OUTPOST_GOODS, SET_GOODS_PRICE, SET_GOODS_TYPE,");
            player.sendMessage("§7         SET_GOODS_QUANTITY, MANAGE_OUTPOST_MEMBERS");
            return;
        }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null || !org.isLeader(player.getUniqueId())) {
            player.sendMessage("§c只有领导者可以修改权限！"); return;
        }
        UUID targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
        if (!org.isMember(targetId)) { player.sendMessage("§c该玩家不在你的组织中！"); return; }
        OrganizationPermission perm;
        try { perm = OrganizationPermission.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException e) { player.sendMessage("§c无效权限名称！"); return; }
        boolean grant = args[2].equalsIgnoreCase("grant");
        orgManager.setMemberPermission(org, targetId, perm, grant);
        player.sendMessage("§a已" + (grant ? "授予" : "撤销") + " §f" + args[0] + " §a的权限 §f" + perm.getDisplayName());
    }

    private void handleInfo(Player player, String[] args) {
        Organization org;
        if (args.length >= 1) {
            org = orgManager.getOrganizationByName(args[0]);
            if (org == null) { player.sendMessage("§c找不到组织 §f" + args[0]); return; }
        } else {
            org = orgManager.getPlayerOrganization(player.getUniqueId());
            if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }
        }
        player.sendMessage("§6========== 组织信息 ==========");
        player.sendMessage("§e名称: §f" + org.getName());
        player.sendMessage("§e类型: §f" + (org.isNationalOrganization() ? "国家组织 (" + org.getNationName() + ")" : "非国家组织"));
        player.sendMessage("§e成员数: §f" + org.getMembers().size());
        player.sendMessage("§e据点数: §f" + orgManager.getOrganizationOutposts(org.getId()).size());
        player.sendMessage("§e成员列表:");
        for (OrganizationMember m : org.getAllMembers()) {
            String name = Bukkit.getOfflinePlayer(m.getPlayerId()).getName();
            player.sendMessage("  §7- §f" + name + " §8[" + m.getRole().getDisplayName() + "§8]");
        }
    }

    private void handleList(Player player) {
        var orgs = orgManager.getAllOrganizations();
        if (orgs.isEmpty()) { player.sendMessage("§7当前没有任何组织。"); return; }
        player.sendMessage("§6========== 组织列表 ==========");
        for (Organization org : orgs) {
            player.sendMessage("§e" + org.getName() + " §7- " + org.getMembers().size() + " 成员"
                    + (org.isNationalOrganization() ? " §8[" + org.getNationName() + "]" : ""));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6===== 组织命令帮助 =====");
        player.sendMessage("§e/legacy organize new <名称> §7- 创建组织");
        player.sendMessage("§e/legacy organize disband §7- 解散组织");
        player.sendMessage("§e/legacy organize invite <玩家> [MANAGER|MEMBER] §7- 邀请成员");
        player.sendMessage("§e/legacy organize kick <玩家> §7- 踢出成员");
        player.sendMessage("§e/legacy organize role <玩家> <角色> §7- 设置角色");
        player.sendMessage("§e/legacy organize perm <玩家> <权限> <grant|revoke> §7- 设置权限");
        player.sendMessage("§e/legacy organize info [名称] §7- 查看组织信息");
        player.sendMessage("§e/legacy organize list §7- 列出所有组织");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(List.of("new", "disband", "invite", "kick", "role", "perm", "info", "list"));
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "invite", "kick", "role", "perm" ->
                        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "info" ->
                        orgManager.getAllOrganizations().forEach(o -> completions.add(o.getName()));
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("role")) completions.addAll(List.of("MANAGER", "MEMBER"));
            else if (args[1].equalsIgnoreCase("perm")) {
                for (OrganizationPermission p : OrganizationPermission.values()) completions.add(p.name());
            } else if (args[1].equalsIgnoreCase("invite")) completions.addAll(List.of("MANAGER", "MEMBER"));
        } else if (args.length == 5 && args[1].equalsIgnoreCase("perm")) {
            completions.addAll(List.of("grant", "revoke"));
        }
        return completions;
    }
}
