package net.chen.legacyLand.economy.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.economy.FuturesManager;
import net.chen.legacyLand.economy.LoanManager;
import net.chen.legacyLand.util.Translatable;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 金融命令（贷款、期货）
 * /finance <子命令>
 */
public class FinanceCommand implements CommandExecutor, TabCompleter {

    private final LoanManager loanManager;
    private final FuturesManager futuresManager;

    public FinanceCommand(LoanManager loanManager, FuturesManager futuresManager) {
        this.loanManager = loanManager;
        this.futuresManager = futuresManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Translatable.of("msg.player_only").send(sender);
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "loan" -> handleLoan(player, args);
            case "repay" -> handleRepay(player, args);
            case "loans" -> handleLoans(player, args);
            case "future" -> handleFuture(player, args);
            case "buy" -> handleBuy(player, args);
            case "deliver" -> handleDeliver(player, args);
            case "claim" -> handleClaim(player, args);
            case "futures" -> handleFutures(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * 申请贷款
     * /finance loan <金额> [国家]
     */
    private void handleLoan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /finance loan <金额> [国家]");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage("§c金额必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的金额！");
            return;
        }

        String nationName;
        if (args.length >= 3) {
            nationName = args[2];
        } else {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null) return;

            Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
            if (nation == null) {
                Translatable.of("nation.nobelongs").forLocale(player);
                return;
            }
            nationName = nation.getName();
        }

        boolean success = loanManager.applyLoan(player, nationName, amount);
        if (success) {
            player.sendMessage("§a贷款申请成功！");
            player.sendMessage("§7贷款金额: §e" + String.format("%.2f", amount));
            player.sendMessage("§7利率: §e5% 年化");
            player.sendMessage("§7期限: §e30 天");
            double totalDue = amount * 1.05;
            player.sendMessage("§7应还总额: §e" + String.format("%.2f", totalDue));
        } else {
            player.sendMessage("§c贷款申请失败！可能是银行资金不足或你有未还清的贷款");
        }
    }

    /**
     * 还款
     * /finance repay <贷款ID> <金额>
     */
    private void handleRepay(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /finance repay <贷款ID> <金额>");
            return;
        }

        int loanId;
        try {
            loanId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的贷款ID！");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage("§c金额必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的金额！");
            return;
        }

        boolean success = loanManager.repayLoan(player, loanId, amount);
        if (success) {
            player.sendMessage("§a还款成功！");
            player.sendMessage("§7还款金额: §e" + String.format("%.2f", amount));
        } else {
            player.sendMessage("§c还款失败！可能是余额不足或贷款不存在");
        }
    }

    /**
     * 查看贷款列表
     * /finance loans
     */
    private void handleLoans(Player player, String[] args) {
        List<LoanManager.Loan> loans = loanManager.getPlayerLoans(player.getUniqueId());

        if (loans.isEmpty()) {
            player.sendMessage("§7你没有任何贷款");
            return;
        }

        player.sendMessage("§6========== 我的贷款 ==========");
        for (LoanManager.Loan loan : loans) {
            double totalDue = loanManager.calculateTotalDue(loan);
            double remaining = totalDue - loan.repaidAmount;

            player.sendMessage("§eID: §f" + loan.id + " §7| §e" + loan.nationName);
            player.sendMessage("  §7本金: §f" + String.format("%.2f", loan.amount));
            player.sendMessage("  §7应还: §f" + String.format("%.2f", totalDue));
            player.sendMessage("  §7已还: §f" + String.format("%.2f", loan.repaidAmount));
            player.sendMessage("  §7剩余: §f" + String.format("%.2f", remaining));
            player.sendMessage("  §7状态: " + (loan.status.equals("active") ? "§c未还清" : "§a已还清"));
        }
        player.sendMessage("§6============================");
    }

    /**
     * 创建期货合约
     * /finance future <物品> <数量> <价格> <天数> [国家]
     */
    private void handleFuture(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage("§c用法: /finance future <物品> <数量> <价格> <天数> [国家]");
            return;
        }

        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的物品类型！");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                player.sendMessage("§c数量必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数量！");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[3]);
            if (price <= 0) {
                player.sendMessage("§c价格必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的价格！");
            return;
        }

        int days;
        try {
            days = Integer.parseInt(args[4]);
            if (days <= 0 || days > 90) {
                player.sendMessage("§c天数必须在 1-90 之间！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的天数！");
            return;
        }

        String nationName;
        if (args.length >= 6) {
            nationName = args[5];
        } else {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null) return;

            Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
            if (nation == null) {
                Translatable.of("nation.nobelongs").forLocale(player);
                return;
            }
            nationName = nation.getName();
        }

        int futureId = futuresManager.createFuture(player, nationName, material, amount, price, days);
        if (futureId > 0) {
            player.sendMessage("§a期货合约创建成功！");
            player.sendMessage("§7合约ID: §e" + futureId);
            player.sendMessage("§7物品: §e" + material.name() + " x" + amount);
            player.sendMessage("§7价格: §e" + String.format("%.2f", price));
            player.sendMessage("§7交割日期: §e" + days + " 天后");
        } else {
            player.sendMessage("§c创建期货合约失败！");
        }
    }

    /**
     * 购买期货
     * /finance buy <合约ID>
     */
    private void handleBuy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /finance buy <合约ID>");
            return;
        }

        int futureId;
        try {
            futureId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的合约ID！");
            return;
        }

        boolean success = futuresManager.buyFuture(player, futureId);
        if (success) {
            player.sendMessage("§a成功购买期货合约！");
            player.sendMessage("§7等待卖方交割物品");
        } else {
            player.sendMessage("§c购买失败！可能是余额不足或合约不存在");
        }
    }

    /**
     * 交割期货
     * /finance deliver <合约ID>
     */
    private void handleDeliver(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /finance deliver <合约ID>");
            return;
        }

        int futureId;
        try {
            futureId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的合约ID！");
            return;
        }

        boolean success = futuresManager.deliverFuture(player, futureId);
        if (success) {
            player.sendMessage("§a成功交割期货！");
        } else {
            player.sendMessage("§c交割失败！可能是未到交割日期或物品不足");
        }
    }

    /**
     * 领取期货物品
     * /finance claim <合约ID>
     */
    private void handleClaim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /finance claim <合约ID>");
            return;
        }

        int futureId;
        try {
            futureId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的合约ID！");
            return;
        }

        boolean success = futuresManager.claimFuture(player, futureId);
        if (success) {
            player.sendMessage("§a成功领取期货物品！");
        } else {
            player.sendMessage("§c领取失败！可能是卖方未交割或合约不存在");
        }
    }

    /**
     * 查看期货列表
     * /finance futures [open|my]
     */
    private void handleFutures(Player player, String[] args) {
        String filter = args.length >= 2 ? args[1].toLowerCase() : "open";

        List<FuturesManager.Future> futures;
        if (filter.equals("my")) {
            futures = futuresManager.getPlayerFutures(player.getUniqueId());
        } else {
            futures = futuresManager.getOpenFutures();
        }

        if (futures.isEmpty()) {
            player.sendMessage("§7没有找到期货合约");
            return;
        }

        player.sendMessage("§6========== 期货合约 ==========");
        for (FuturesManager.Future future : futures) {
            long daysLeft = (future.deliveryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);

            player.sendMessage("§eID: §f" + future.id + " §7| §e" + future.nationName);
            player.sendMessage("  §7物品: §f" + future.material + " x" + future.amount);
            player.sendMessage("  §7价格: §f" + String.format("%.2f", future.price));
            player.sendMessage("  §7交割: §f" + daysLeft + " 天后");
            player.sendMessage("  §7状态: §f" + future.status);
        }
        player.sendMessage("§6============================");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 金融系统 ==========");
        player.sendMessage("§e贷款系统:");
        player.sendMessage("  §7/finance loan <金额> - 申请贷款");
        player.sendMessage("  §7/finance repay <ID> <金额> - 还款");
        player.sendMessage("  §7/finance loans - 查看我的贷款");
        player.sendMessage("§e期货系统:");
        player.sendMessage("  §7/finance future <物品> <数量> <价格> <天数> - 创建期货");
        player.sendMessage("  §7/finance buy <ID> - 购买期货");
        player.sendMessage("  §7/finance deliver <ID> - 交割期货");
        player.sendMessage("  §7/finance claim <ID> - 领取期货");
        player.sendMessage("  §7/finance futures [open|my] - 查看期货列表");
        player.sendMessage("§6============================");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("loan", "repay", "loans", "future", "buy", "deliver", "claim", "futures");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("futures")) {
            return Arrays.asList("open", "my");
        }

        return new ArrayList<>();
    }
}
