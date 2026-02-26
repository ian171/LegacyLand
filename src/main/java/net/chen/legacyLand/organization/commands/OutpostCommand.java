package net.chen.legacyLand.organization.commands;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.organization.*;
import net.chen.legacyLand.organization.outpost.Outpost;
import net.chen.legacyLand.organization.outpost.OutpostGoods;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 据点命令处理器
 * /legacy outpost <子命令>
 */
public class OutpostCommand implements CommandExecutor, TabCompleter {

    private final OrganizationManager orgManager;

    public OutpostCommand() {
        this.orgManager = OrganizationManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        if (args.length < 2) { sendHelp(player); return true; }

        String sub = args[1].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 2, args.length);

        switch (sub) {
            case "create"   -> handleCreate(player);
            case "delete"   -> handleDelete(player, subArgs);
            case "open"     -> handleToggle(player, subArgs, true);
            case "close"    -> handleToggle(player, subArgs, false);
            case "goods"    -> handleGoods(player, subArgs);
            case "list"     -> handleList(player);
            case "info"     -> handleInfo(player, subArgs);
            default         -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player) {
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }

        if (!org.isLeader(player.getUniqueId())
                && !org.hasPermission(player.getUniqueId(), OrganizationPermission.DELETE_OUTPOST)) {
            player.sendMessage("§c你没有创建据点的权限！"); return;
        }

        var result = orgManager.createOutpost(player, org);
        int radius = LegacyLand.getInstance().getConfig().getInt("organization.outpost-radius", 16);
        double cost  = LegacyLand.getInstance().getConfig().getDouble("organization.outpost-cost", 1000.0);
        switch (result) {
            case SUCCESS ->
                    player.sendMessage("§a据点已在你的位置创建！半径 " + radius + " 格，花费 " + cost + " 金币。");
            case NOT_WILDERNESS ->
                    player.sendMessage("§c据点只能建立在荒野！");
            case OVERLAPPING ->
                    player.sendMessage("§c此处已有其他据点！");
            case INSUFFICIENT_FUNDS ->
                    player.sendMessage("§c余额不足！创建据点需要 " + cost + " 金币。");
            case NO_PERMISSION ->
                    player.sendMessage("§c你没有创建据点的权限！");
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage("§c用法: /legacy outpost delete <据点ID>"); return; }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }
        if (!org.isLeader(player.getUniqueId()) && !org.hasPermission(player.getUniqueId(), OrganizationPermission.DELETE_OUTPOST)) {
            player.sendMessage("§c你没有删除据点的权限！"); return;
        }
        boolean ok = orgManager.deleteOutpost(org, args[0]);
        player.sendMessage(ok ? "§a据点已删除。" : "§c找不到该据点或无权操作！");
    }

    private void handleToggle(Player player, String[] args, boolean open) {
        if (args.length < 1) { player.sendMessage("§c用法: /legacy outpost " + (open ? "open" : "close") + " <据点ID>"); return; }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }
        if (!org.isLeader(player.getUniqueId()) && !org.hasPermission(player.getUniqueId(), OrganizationPermission.CLOSE_OUTPOST)) {
            player.sendMessage("§c你没有操作据点的权限！"); return;
        }
        boolean ok = orgManager.toggleOutpost(org, args[0], open);
        player.sendMessage(ok ? "§a据点已" + (open ? "开放" : "关闭") + "。" : "§c找不到该据点！");
    }

    private void handleGoods(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§c用法: /legacy outpost goods <add|remove|list> [参数]");
            return;
        }
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }

        switch (args[0].toLowerCase()) {
            case "add"    -> handleGoodsAdd(player, org, Arrays.copyOfRange(args, 1, args.length));
            case "remove" -> handleGoodsRemove(player, org, Arrays.copyOfRange(args, 1, args.length));
            case "list"   -> handleGoodsList(player, org, Arrays.copyOfRange(args, 1, args.length));
            default       -> player.sendMessage("§c无效子命令！用法: add | remove | list");
        }
    }

    private void handleGoodsAdd(Player player, Organization org, String[] args) {
        // /legacy outpost goods add <据点ID> <价格> <数量>
        if (args.length < 3) {
            player.sendMessage("§c用法: /legacy outpost goods add <据点ID> <价格> <数量>"); return;
        }
        if (!org.isLeader(player.getUniqueId()) && !org.hasPermission(player.getUniqueId(), OrganizationPermission.ADD_OUTPOST_GOODS)) {
            player.sendMessage("§c你没有添加货物的权限！"); return;
        }
        Outpost outpost = orgManager.getOutpostById(args[0]);
        if (outpost == null || !outpost.getOrganizationId().equals(org.getId())) {
            player.sendMessage("§c找不到该据点！"); return;
        }
        double price; int quantity;
        try {
            price = Double.parseDouble(args[1]);
            quantity = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c价格和数量必须是数字！"); return;
        }
        if (price <= 0 || quantity <= 0) { player.sendMessage("§c价格和数量必须大于0！"); return; }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { player.sendMessage("§c请手持要添加的物品！"); return; }

        if (!org.hasPermission(player.getUniqueId(), OrganizationPermission.SET_GOODS_PRICE)) {
            // 如果没有定价权限，默认不允许自定义价格
        }

        orgManager.addGoods(outpost, hand, price, quantity);
        player.sendMessage("§a已向据点 §f" + outpost.getId().substring(0, 8) + " §a添加货物: §f"
                + hand.getType().name() + " x" + quantity + " §7@§f" + price + " 金币/个");
    }

    private void handleGoodsRemove(Player player, Organization org, String[] args) {
        if (args.length < 2) { player.sendMessage("§c用法: /legacy outpost goods remove <据点ID> <货物ID>"); return; }
        if (!org.isLeader(player.getUniqueId()) && !org.hasPermission(player.getUniqueId(), OrganizationPermission.SET_GOODS_QUANTITY)) {
            player.sendMessage("§c你没有移除货物的权限！"); return;
        }
        Outpost outpost = orgManager.getOutpostById(args[0]);
        if (outpost == null || !outpost.getOrganizationId().equals(org.getId())) {
            player.sendMessage("§c找不到该据点！"); return;
        }
        boolean ok = orgManager.removeGoods(outpost, args[1]);
        player.sendMessage(ok ? "§a货物已移除。" : "§c找不到该货物！");
    }

    private void handleGoodsList(Player player, Organization org, String[] args) {
        if (args.length < 1) { player.sendMessage("§c用法: /legacy outpost goods list <据点ID>"); return; }
        Outpost outpost = orgManager.getOutpostById(args[0]);
        if (outpost == null || !outpost.getOrganizationId().equals(org.getId())) {
            player.sendMessage("§c找不到该据点！"); return;
        }
        List<OutpostGoods> goodsList = outpost.getAllGoods();
        if (goodsList.isEmpty()) { player.sendMessage("§7该据点暂无货物。"); return; }
        player.sendMessage("§6===== 据点货物列表 =====");
        for (OutpostGoods g : goodsList) {
            player.sendMessage("§7ID: §f" + g.getId().substring(0, 8)
                    + " §e" + g.getItem().getType().name()
                    + " §7x§f" + g.getQuantity()
                    + " §7@§6" + g.getPrice() + " §7金币/个");
        }
    }

    private void handleList(Player player) {
        Organization org = orgManager.getPlayerOrganization(player.getUniqueId());
        if (org == null) { player.sendMessage("§c你不在任何组织中！"); return; }
        List<Outpost> outposts = orgManager.getOrganizationOutposts(org.getId());
        if (outposts.isEmpty()) { player.sendMessage("§7你的组织暂无据点。"); return; }
        player.sendMessage("§6===== 据点列表 =====");
        for (Outpost o : outposts) {
            String loc = String.format("§7(%.0f,%.0f,%.0f)", o.getCenter().getX(), o.getCenter().getY(), o.getCenter().getZ());
            player.sendMessage("§fID: §e" + o.getId().substring(0, 8) + " " + loc
                    + " §8[" + o.getStatus().getDisplayName() + "§8]"
                    + " §7货物: " + o.getAllGoods().size());
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage("§c用法: /legacy outpost info <据点ID>"); return; }
        Outpost outpost = orgManager.getOutpostById(args[0]);
        if (outpost == null) {
            // 尝试短ID匹配
            outpost = orgManager.getAllOrganizations().stream()
                    .flatMap(o -> orgManager.getOrganizationOutposts(o.getId()).stream())
                    .filter(o -> o.getId().startsWith(args[0]))
                    .findFirst().orElse(null);
        }
        if (outpost == null) { player.sendMessage("§c找不到据点！"); return; }
        player.sendMessage("§6===== 据点信息 =====");
        player.sendMessage("§eID: §f" + outpost.getId().substring(0, 8));
        player.sendMessage("§e状态: §f" + outpost.getStatus().getDisplayName());
        player.sendMessage("§e位置: §f" + String.format("%.0f, %.0f, %.0f", outpost.getCenter().getX(), outpost.getCenter().getY(), outpost.getCenter().getZ()));
        player.sendMessage("§e世界: §f" + outpost.getCenter().getWorld().getName());
        player.sendMessage("§e半径: §f" + outpost.getRadius() + " 格");
        player.sendMessage("§e货物数: §f" + outpost.getAllGoods().size());
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6===== 据点命令帮助 =====");
        player.sendMessage("§e/legacy outpost create §7- 在当前位置创建据点");
        player.sendMessage("§e/legacy outpost delete <ID> §7- 删除据点");
        player.sendMessage("§e/legacy outpost open <ID> §7- 开放据点");
        player.sendMessage("§e/legacy outpost close <ID> §7- 关闭据点");
        player.sendMessage("§e/legacy outpost goods add <ID> <价格> <数量> §7- 手持物品添加货物");
        player.sendMessage("§e/legacy outpost goods remove <据点ID> <货物ID> §7- 移除货物");
        player.sendMessage("§e/legacy outpost goods list <ID> §7- 查看货物列表");
        player.sendMessage("§e/legacy outpost list §7- 查看我的组织的据点");
        player.sendMessage("§e/legacy outpost info <ID> §7- 查看据点详情");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(List.of("create", "delete", "open", "close", "goods", "list", "info"));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("goods")) {
            completions.addAll(List.of("add", "remove", "list"));
        }
        return completions;
    }
}
