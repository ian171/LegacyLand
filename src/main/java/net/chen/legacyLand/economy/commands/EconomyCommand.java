package net.chen.legacyLand.economy.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.economy.CurrencyItem;
import net.chen.legacyLand.economy.TreasuryManager;
import net.chen.legacyLand.util.Translatable;
import org.bukkit.Material;
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
 * 经济系统命令
 * /economy <子命令>
 */
public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final TreasuryManager treasuryManager;

    public EconomyCommand(TreasuryManager treasuryManager) {
        this.treasuryManager = treasuryManager;
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
            case "create" -> handleCreate(player, args);
            case "seal" -> handleSeal(player, args);
            case "mint" -> handleMint(player, args);
            case "info" -> handleInfo(player, args);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "rate" -> handleRate(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * 创建国库
     * /economy create
     */
    private void handleCreate(Player player, String[] args) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            player.sendMessage("§c你必须是 Towny 居民！");
            return;
        }

        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null) {
            Translatable.of("nation.nobelongs").forLocale(player);
            return;
        }

        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国王可以创建国库！");
            return;
        }

        if (treasuryManager != null) {
            if (treasuryManager.getTreasury(nation.getName()) != null) {
                player.sendMessage("§c国库已存在！");
                return;
            }
        }else {
            return;
        }

        boolean success = treasuryManager.createTreasury(nation.getName(), player.getLocation());
        if (success) {
            player.sendMessage("§a国库创建成功！位置: " + formatLocation(player.getLocation()));
            player.sendMessage("§e使用 /economy seal 获取国库印章");
        } else {
            player.sendMessage("§c创建国库失败！");
        }
    }

    /**
     * 获取国库印章
     * /economy seal
     */
    private void handleSeal(Player player, String[] args) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null) {
            Translatable.of("nation.nobelongs").forLocale(player);
            return;
        }

        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国王可以获取国库印章！");
            return;
        }
        TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nation.getName());
        if (treasury == null) {
            player.sendMessage("§c请先创建国库！使用 /economy create");
            return;
        }

        ItemStack seal = CurrencyItem.createTreasurySeal(nation.getName());
        player.getInventory().addItem(seal);
        player.sendMessage("§a已获得国库印章！");
        player.sendMessage("§7使用印章 + 纸张 + 墨囊在工作台合成货币");
    }

    /**
     * 铸造货币
     * /economy mint <面值>
     */
    private void handleMint(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /economy mint <面值>");
            return;
        }

        double denomination;
        try {
            denomination = Double.parseDouble(args[1]);
            if (denomination <= 0) {
                player.sendMessage("§c面值必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的面值！");
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null) {
            Translatable.of("nation.nobelongs").forLocale(player);
            return;
        }

        // 检查是否持有国库印章
        boolean hasSeal = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (CurrencyItem.isTreasurySeal(item) &&
                nation.getName().equals(CurrencyItem.getNation(item))) {
                hasSeal = true;
                break;
            }
        }

        if (!hasSeal) {
            player.sendMessage("§c你需要持有国库印章才能铸造货币！");
            return;
        }

        // 检查是否有纸张和墨囊
        if (!player.getInventory().contains(Material.PAPER, 1) ||
            !player.getInventory().contains(Material.INK_SAC, 1)) {
            player.sendMessage("§c铸造货币需要: 1x 纸张 + 1x 墨囊");
            return;
        }

        // 扣除材料
        player.getInventory().removeItem(new ItemStack(Material.PAPER, 1));
        player.getInventory().removeItem(new ItemStack(Material.INK_SAC, 1));

        // 发行货币
        ItemStack currency = treasuryManager.issueCurrency(nation.getName(),
            denomination, player.getUniqueId().toString());

        if (currency != null) {
            player.getInventory().addItem(currency);
            player.sendMessage("§a成功铸造 " + denomination + " 金锭面值的货币！");
            player.sendMessage("§7序列号: " + CurrencyItem.getSerialNumber(currency));
        } else {
            player.sendMessage("§c铸造货币失败！");
        }
    }

    /**
     * 查看国库信息
     * /economy info [国家名]
     */
    private void handleInfo(Player player, String[] args) {
        String nationName;

        if (args.length >= 2) {
            nationName = args[1];
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

        TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nationName);
        if (treasury == null) {
            player.sendMessage("§c国库不存在: " + nationName);
            return;
        }

        player.sendMessage("§6§l========== " + nationName + " 国库 ==========");
        player.sendMessage("§7位置: §f" + formatLocation(treasury.getLocation()));
        player.sendMessage("§7储备金 (SBC): §e" + String.format("%.2f", treasury.getSbcReserve()) + " 金锭");
        player.sendMessage("§7已发行货币: §e" + String.format("%.2f", treasury.getCurrencyIssued()));
        player.sendMessage("§7信用系数: §e" + String.format("%.2f", treasury.getCreditScore()));
        //player.sendMessage("§7汇率: §e1 NC = " + String.format("%.4f", treasury.calculateExchangeRate()) + " SBC");
        player.sendMessage("§6§l=====================================");
    }

    /**
     * 存入储备金
     * /economy deposit <数量>
     */
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /economy deposit <数量>");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage("§c数量必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数量！");
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null) {
            Translatable.of("nation.nobelongs").forLocale(player);
            return;
        }

        // 检查是否有足够的金锭
        if (!player.getInventory().contains(Material.GOLD_INGOT, amount)) {
            player.sendMessage("§c你没有足够的金锭！");
            return;
        }

        // 扣除金锭
        player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, amount));

        // 增加储备金
        boolean success = treasuryManager.addReserve(nation.getName(), amount);
        if (success) {
            player.sendMessage("§a成功存入 " + amount + " 金锭到国库！");
            TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nation.getName());
            player.sendMessage("§7当前储备: " + String.format("%.2f", treasury.getSbcReserve()) + " 金锭");
        } else {
            player.sendMessage("§c存入失败！");
            player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, amount));
        }
    }

    /**
     * 提取储备金
     * /economy withdraw <数量>
     */
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /economy withdraw <数量>");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage("§c数量必须大于 0！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数量！");
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        if (nation == null) {
            Translatable.of("nation.nobelongs").forLocale(player);
            return;
        }

        if (!nation.isKing(resident)) {
            player.sendMessage("§c只有国王可以提取储备金！");
            return;
        }

        boolean success = treasuryManager.removeReserve(nation.getName(), amount);
        if (success) {
            player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, amount));
            player.sendMessage("§a成功提取 " + amount + " 金锭！");
            TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nation.getName());
            player.sendMessage("§7剩余储备: " + String.format("%.2f", treasury.getSbcReserve()) + " 金锭");
        } else {
            player.sendMessage("§c储备金不足！");
        }
    }

    /**
     * 查看汇率
     * /economy rate [国家名]
     */
    private void handleRate(Player player, String[] args) {
        if (args.length >= 2) {
            String nationName = args[1];
            TreasuryManager.Treasury treasury = treasuryManager.getTreasury(nationName);
            if (treasury == null) {
                player.sendMessage("§c国库不存在: " + nationName);
                return;
            }
            displayRate(player, treasury);
        } else {
            // 显示所有国家汇率
            player.sendMessage("§6§l========== 全服汇率 ==========");
            treasuryManager.getAllTreasuries().values().forEach(treasury -> {
                double rate = treasury.calculateExchangeRate();
                player.sendMessage("§e" + treasury.getNationName() + ": §f1 NC = " +
                    String.format("%.4f", rate) + " SBC");
            });
            player.sendMessage("§6§l============================");
        }
    }

    private void displayRate(Player player, TreasuryManager.Treasury treasury) {
        double rate = treasury.calculateExchangeRate();
        player.sendMessage("§6§l========== " + treasury.getNationName() + " 汇率 ==========");
        player.sendMessage("§71 " + treasury.getNationName() + " 货币 = §e" +
            String.format("%.4f", rate) + " §7金锭");
        player.sendMessage("§71 金锭 = §e" + String.format("%.2f", 1 / rate) + " §7" +
            treasury.getNationName() + " 货币");
        player.sendMessage("§6§l=====================================");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l========== 经济系统 ==========");
        player.sendMessage("§e/economy create §7- 创建国库");
        player.sendMessage("§e/economy seal §7- 获取国库印章");
        player.sendMessage("§e/economy mint <面值> §7- 铸造货币");
        player.sendMessage("§e/economy info [国家] §7- 查看国库信息");
        player.sendMessage("§e/economy deposit <数量> §7- 存入储备金");
        player.sendMessage("§e/economy withdraw <数量> §7- 提取储备金");
        player.sendMessage("§e/economy rate [国家] §7- 查看汇率");
        player.sendMessage("§6§l============================");
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("%s (%d, %d, %d)",
            loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "seal", "mint", "info", "deposit", "withdraw", "rate");
        }
        return new ArrayList<>();
    }
}
