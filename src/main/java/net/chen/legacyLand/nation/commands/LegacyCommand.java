package net.chen.legacyLand.nation.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.events.PlayerObtainsRoleEvent;
import net.chen.legacyLand.nation.GovernmentType;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationRole;
import net.chen.legacyLand.nation.politics.PoliticalSystem;
import net.chen.legacyLand.nation.politics.PoliticalSystemManager;
import net.chen.legacyLand.organization.commands.OrganizeCommand;
import net.chen.legacyLand.organization.commands.OutpostCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.block.Block;
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
 * LegacyLand 扩展命令 - 用于设置政体、角色等扩展功能
 */
public class LegacyCommand implements CommandExecutor, TabCompleter {

    private final NationManager nationManager;
    private final TownyAPI townyAPI;
    private final PoliticalSystemManager politicalSystemManager;
    private final OrganizeCommand organizeCommand;
    private final OutpostCommand outpostCommand;

    public LegacyCommand() {
        this.nationManager = NationManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
        this.politicalSystemManager = PoliticalSystemManager.getInstance();
        this.organizeCommand = new OrganizeCommand();
        this.outpostCommand = new OutpostCommand();
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
            case "politics"   -> handlePolitics(player, args);
            case "role"       -> handleRole(player, args);
            case "info"       -> handleInfo(player);
            case "trade"      -> handleTrade(player, args);
            case "treasury"   -> handleTreasury(player, args);
            case "organize"   -> organizeCommand.onCommand(sender, command, label, args);
            case "outpost"    -> outpostCommand.onCommand(sender, command, label, args);
            default           -> sendHelp(player);
        }

        return true;
    }
    private void handleTrade(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /legacy trade <目标国家> <价格>");
            player.sendMessage("§7手持要交易的物品执行此命令");
            return;
        }

        Nation source = TownyAPI.getInstance().getNation(player);
        if (source == null) {
            player.sendMessage("§c你不属于任何国家。");
            return;
        }

        Nation target = TownyAPI.getInstance().getNation(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到国家: " + args[1]);
            return;
        }

        if (source.equals(target)) {
            player.sendMessage("§c不能与自己的国家交易。");
            return;
        }

        int price;
        try {
            price = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c价格必须是整数。");
            return;
        }

        if (price <= 0) {
            player.sendMessage("§c价格必须大于 0。");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§c你手上没有物品。");
            return;
        }

        LegacyLand.getInstance().getNationTradeManager().purchaseFrom(source, target, player, item, price);
    }

    /**
     * 设定国库箱子
     */
    private void handleTreasury(Player player, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
            player.sendMessage("§c用法: /legacy treasury set");
            player.sendMessage("§7看向一个箱子执行此命令以设定国库");
            return;
        }

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

        NationRole role = nationManager.getPlayerRole(nation.getName(), player.getUniqueId());
        if (!role.isLeader()) {
            player.sendMessage("§c只有国家领导人才能设定国库！");
            return;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            player.sendMessage("§c请看向一个箱子！");
            return;
        }

        if (nationManager.setTreasury(nation.getName(), block.getLocation())) {
            player.sendMessage("§a成功设定国库位置！");
        } else {
            player.sendMessage("§c设定失败！请确保你看向的是首都城镇领土内的箱子。");
        }
    }

    /**
     * 设置国家政体（旧命令，保留兼容）
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
     * 设置国家政治体制（新命令，配置驱动）
     */
    private void handlePolitics(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            handlePoliticsList(player);
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
            handlePoliticsSet(player, args);
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("info")) {
            handlePoliticsInfo(player);
            return;
        }

        player.sendMessage("§6========== 政治体制命令 ==========");
        player.sendMessage("§e/legacy politics list §7- 查看所有可用政体");
        player.sendMessage("§e/legacy politics set <政体ID> §7- 设置国家政治体制");
        player.sendMessage("§e/legacy politics info §7- 查看当前政体详情");
    }

    /**
     * 列出所有可用政体
     */
    private void handlePoliticsList(Player player) {
        player.sendMessage("§6========== 可用政治体制 ==========");
        for (PoliticalSystem system : politicalSystemManager.getSystems().values()) {
            player.sendMessage("§e" + system.id() + " §7- §f" + system.displayName());
            player.sendMessage("  §7" + system.description());
            player.sendMessage("  §7税收效率: §f" + formatPercent(system.getTaxEfficiency())
                    + " §7| 军事力量: §f" + formatPercent(system.getMilitaryStrength())
                    + " §7| 国库收入: §f" + formatPercent(system.getTreasuryIncome()));
        }
    }

    /**
     * 设置国家政治体制
     */
    private void handlePoliticsSet(Player player, String[] args) {
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

        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国王才能设置政治体制！");
            if (resident.isMayor()){
                player.sendMessage("§c你只是一个小小的城主罢了...喵");
            }
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§c用法: /legacy politics set <政体ID>");
            player.sendMessage("§e使用 /legacy politics list 查看可用政体");
            return;
        }

        String systemId = args[2].toUpperCase();

        if (!politicalSystemManager.isValidSystem(systemId)) {
            player.sendMessage("§c无效的政体类型: " + systemId);
            player.sendMessage("§e使用 /legacy politics list 查看可用政体");
            return;
        }

        // 检查冷却
        if (politicalSystemManager.isOnCooldown(nation.getName())) {
            long remaining = politicalSystemManager.getRemainingCooldown(nation.getName());
            long hours = remaining / 3600;
            long minutes = (remaining % 3600) / 60;
            player.sendMessage("§c政体切换冷却中！剩余时间: " + hours + "小时" + minutes + "分钟");
            return;
        }

        // 检查是否与当前政体相同
        String currentId = nationManager.getPoliticalSystemId(nation.getName());
        if (currentId.equals(systemId)) {
            player.sendMessage("§c当前国家已经是该政体！");
            return;
        }

        // 检查费用
        double cost = politicalSystemManager.getChangeCost();
        Economy econ = LegacyLand.getEcon();
        if (econ != null && cost > 0) {
            if (econ.getBalance(player) < cost) {
                player.sendMessage("§c切换政体需要 §e" + cost + " §c金币，你的余额不足！");
                return;
            }
            econ.withdrawPlayer(player, cost);
            player.sendMessage("§7已扣除 §e" + cost + " §7金币。");
        }

        nationManager.setPoliticalSystem(nation.getName(), systemId);
        PoliticalSystem system = politicalSystemManager.getSystem(systemId);
        player.sendMessage("§a成功将国家政治体制设置为: §e" + system.displayName());
        player.sendMessage("§7" + system.description());
    }

    /**
     * 查看当前政体详情
     */
    private void handlePoliticsInfo(Player player) {
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

        PoliticalSystem system = nationManager.getPoliticalSystem(nation.getName());
        if (system == null) {
            player.sendMessage("§c当前国家未设置政治体制。");
            return;
        }

        player.sendMessage("§6========== " + nation.getName() + " 政治体制 ==========");
        player.sendMessage("§e政体: §f" + system.displayName() + " §7(" + system.id() + ")");
        player.sendMessage("§7" + system.description());
        player.sendMessage("§e--- 效果 ---");
        player.sendMessage("§7税收效率: §f" + formatPercent(system.getTaxEfficiency()));
        player.sendMessage("§7军事力量: §f" + formatPercent(system.getMilitaryStrength()));
        player.sendMessage("§7国库收入: §f" + formatPercent(system.getTreasuryIncome()));
        player.sendMessage("§7战争冷却: §f" + formatPercent(system.getWarCooldownModifier()));
        player.sendMessage("§7同盟上限修正: §f" + (system.getMaxAllianceModifier() >= 0 ? "+" : "") + system.getMaxAllianceModifier());
        player.sendMessage("§7城镇上限修正: §f" + (system.getMaxTownModifier() >= 0 ? "+" : "") + system.getMaxTownModifier());

        player.sendMessage("§e--- 可用角色 ---");
        for (NationRole role : system.getAllowedRoles()) {
            player.sendMessage("§7- §f" + role.getDisplayName() + " §7(" + role.name() + ")");
        }
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

        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国家领袖才能设置角色！");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§c用法: /legacy role <玩家名> <角色>");
            // 根据当前政体显示可用角色
            PoliticalSystem system = nationManager.getPoliticalSystem(nation.getName());
            if (system != null) {
                player.sendMessage("§e当前政体可用角色:");
                for (NationRole role : system.getAllowedRoles()) {
                    if (!role.isLeader()) {
                        player.sendMessage("§7- " + role.name() + " (" + role.getDisplayName() + ")");
                    }
                }
            }
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

        // 检查角色是否在当前政体允许范围内
        PoliticalSystem system = nationManager.getPoliticalSystem(nation.getName());
        if (system != null && !system.isRoleAllowed(role)) {
            player.sendMessage("§c当前政体 §e" + system.displayName() + " §c不允许设置该角色！");
            player.sendMessage("§e使用 /legacy politics info 查看当前政体可用角色。");
            return;
        }

        nationManager.setPlayerRole(nation.getName(), target.getUUID(), role);
        PlayerObtainsRoleEvent event = new PlayerObtainsRoleEvent(nation, role, player);
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

        NationRole role = nationManager.getPlayerRole(nation.getName(), player.getUniqueId());
        PoliticalSystem system = nationManager.getPoliticalSystem(nation.getName());

        player.sendMessage("§6========== " + nation.getName() + " 扩展信息 ==========");
        if (system != null) {
            player.sendMessage("§e政治体制: §f" + system.displayName());
        } else {
            GovernmentType govType = nationManager.getGovernmentType(nation.getName());
            player.sendMessage("§e政体: §f" + (govType != null ? govType.getDisplayName() : "未设置"));
        }
        player.sendMessage("§e你的角色: §f" + (role != null ? role.getDisplayName() : "公民"));
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== LegacyLand 命令 ==========");
        player.sendMessage("§e/legacy government <政体> §7- 设置国家政体（旧）");
        player.sendMessage("§e/legacy politics <list|set|info> §7- 政治体制管理");
        player.sendMessage("§e/legacy role <玩家> <角色> §7- 设置玩家角色");
        player.sendMessage("§e/legacy trade <目标国家> <价格> §7- 手持物品与其他国家交易");
        player.sendMessage("§e/legacy treasury set §7- 设定国库箱子（看向箱子执行）");
        player.sendMessage("§e/legacy info §7- 查看国家扩展信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("government", "politics", "role", "info", "trade", "treasury"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "government" -> completions.addAll(Arrays.asList("FEUDAL", "REPUBLIC"));
                case "politics" -> completions.addAll(Arrays.asList("list", "set", "info"));
                case "treasury" -> completions.add("set");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("politics") && args[1].equalsIgnoreCase("set")) {
                completions.addAll(politicalSystemManager.getSystemIds());
            } else if (args[0].equalsIgnoreCase("role")) {
                // 根据当前政体过滤可用角色
                if (sender instanceof Player player) {
                    PoliticalSystem system = getNationSystem(player);
                    if (system != null) {
                        for (NationRole role : system.getAllowedRoles()) {
                            if (!role.isLeader()) {
                                completions.add(role.name());
                            }
                        }
                    } else {
                        for (NationRole role : NationRole.values()) {
                            if (!role.isLeader()) {
                                completions.add(role.name());
                            }
                        }
                    }
                }
            }
        }

        // 过滤匹配输入
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }

    private PoliticalSystem getNationSystem(Player player) {
        Resident resident = townyAPI.getResident(player);
        if (resident == null || !resident.hasNation()) return null;
        Nation nation = resident.getNationOrNull();
        if (nation == null) return null;
        return nationManager.getPoliticalSystem(nation.getName());
    }

    private String formatPercent(double value) {
        return String.format("%.0f%%", value * 100);
    }
}
