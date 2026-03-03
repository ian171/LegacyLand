package net.chen.legacyLand.economy.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.economy.BankManager;
import net.chen.legacyLand.economy.CurrencyItem;
import net.chen.legacyLand.util.Translatable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 银行命令
 * /bank <子命令>
 */
public class BankCommand implements CommandExecutor, TabCompleter {

    private final BankManager bankManager;

    public BankCommand(BankManager bankManager) {
        this.bankManager = bankManager;
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
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "balance" -> handleBalance(player, args);
            case "transfer" -> handleTransfer(player, args);
            case "exchange" -> handleExchange(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * 存款 - 将手持的货币存入银行
     * /bank deposit
     */
    private void handleDeposit(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!CurrencyItem.isCurrency(item)) {
            player.sendMessage("§c请手持货币物品！");
            return;
        }

        String nationName = CurrencyItem.getNation(item);
        double amount = CurrencyItem.getDenomination(item);
        int count = item.getAmount();
        double totalAmount = amount * count;

        // 存款
        boolean success = bankManager.deposit(player, nationName, totalAmount);
        if (success) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage("§a成功存入 " + String.format("%.2f", totalAmount) + " " + nationName + " 货币！");
            player.sendMessage("§7当前余额: " + String.format("%.2f", bankManager.getBalance(player.getUniqueId(), nationName)));
        } else {
            player.sendMessage("§c存款失败！");
        }
    }

    /**
     * 取款 - 将电子余额转换为实体货币
     * /bank withdraw <金额> [国家]
     */
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /bank withdraw <金额> [国家]");
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

        // 取款
        boolean success = bankManager.withdraw(player, nationName, amount);
        if (success) {
            // 生成货币物品
            ItemStack currency = CurrencyItem.createCurrency(nationName, amount,
                "BANK-" + System.currentTimeMillis());
            player.getInventory().addItem(currency);
            player.sendMessage("§a成功取出 " + String.format("%.2f", amount) + " " + nationName + " 货币！");
            player.sendMessage("§7剩余余额: " + String.format("%.2f", bankManager.getBalance(player.getUniqueId(), nationName)));
        } else {
            player.sendMessage("§c余额不足！");
        }
    }

    /**
     * 查询余额
     * /bank balance [国家]
     */
    private void handleBalance(Player player, String[] args) {
        if (args.length >= 2) {
            String nationName = args[1];
            double balance = bankManager.getBalance(player.getUniqueId(), nationName);
            player.sendMessage("§6========== 银行余额 ==========");
            player.sendMessage("§7国家: §e" + nationName);
            player.sendMessage("§7余额: §e" + String.format("%.2f", balance));
            player.sendMessage("§6============================");
        } else {
            // 显示所有国家的余额
            player.sendMessage("§6========== 银行余额 ==========");
            bankManager.getAllBalances(player.getUniqueId()).forEach((nation, balance) -> {
                if (balance > 0) {
                    player.sendMessage("§e" + nation + ": §f" + String.format("%.2f", balance));
                }
            });
            player.sendMessage("§6============================");
        }
    }

    /**
     * 转账
     * /bank transfer <玩家> <金额> [国家]
     */
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /bank transfer <玩家> <金额> [国家]");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore()) {
            player.sendMessage("§c玩家不存在: " + targetName);
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

        String nationName;
        if (args.length >= 4) {
            nationName = args[3];
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

        boolean success = bankManager.transfer(player, target.getUniqueId(), nationName, amount);
        if (success) {
            player.sendMessage("§a成功转账 " + String.format("%.2f", amount) + " " + nationName + " 货币给 " + targetName + "！");
            player.sendMessage("§7剩余余额: " + String.format("%.2f", bankManager.getBalance(player.getUniqueId(), nationName)));

            if (target.isOnline()) {
                target.getPlayer().sendMessage("§a你收到了来自 " + player.getName() + " 的转账: " +
                    String.format("%.2f", amount) + " " + nationName + " 货币");
            }
        } else {
            player.sendMessage("§c余额不足！");
        }
    }

    /**
     * 货币兑换
     * /bank exchange <金额> <源国家> <目标国家>
     */
    private void handleExchange(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§c用法: /bank exchange <金额> <源国家> <目标国家>");
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

        String fromNation = args[2];
        String toNation = args[3];

        double exchanged = bankManager.exchangeCurrency(player, fromNation, toNation, amount);
        if (exchanged > 0) {
            player.sendMessage("§a成功兑换！");
            player.sendMessage("§7支付: " + String.format("%.2f", amount) + " " + fromNation);
            player.sendMessage("§7获得: " + String.format("%.2f", exchanged) + " " + toNation);
        } else {
            player.sendMessage("§c兑换失败！余额不足或汇率无效");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l========== 银行系统 ==========");
        player.sendMessage("§e/bank deposit §7- 存入手持的货币");
        player.sendMessage("§e/bank withdraw <金额> [国家] §7- 取出货币");
        player.sendMessage("§e/bank balance [国家] §7- 查询余额");
        player.sendMessage("§e/bank transfer <玩家> <金额> [国家] §7- 转账");
        player.sendMessage("§e/bank exchange <金额> <源国家> <目标国家> §7- 货币兑换");
        player.sendMessage("§6§l==============================");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("deposit", "withdraw", "balance", "transfer", "exchange");
        }
        return new ArrayList<>();
    }
}
