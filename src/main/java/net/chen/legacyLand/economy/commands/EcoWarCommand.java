package net.chen.legacyLand.economy.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.economy.EconomyStatsManager;
import net.chen.legacyLand.economy.EconomyWarManager;
import net.chen.legacyLand.util.Translatable;
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
import java.util.Map;

/**
 * 经济统计与战争命令
 * /ecowar <子命令>
 */
public class EcoWarCommand implements CommandExecutor, TabCompleter {

    private final EconomyStatsManager statsManager;
    private final EconomyWarManager warManager;

    public EcoWarCommand(EconomyStatsManager statsManager, EconomyWarManager warManager) {
        this.statsManager = statsManager;
        this.warManager = warManager;
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
            case "stats" -> handleStats(player, args);
            case "gdp" -> handleGDP(player, args);
            case "inflation" -> handleInflation(player, args);
            case "embargo" -> handleEmbargo(player, args);
            case "lift" -> handleLift(player, args);
            case "sanction" -> handleSanction(player, args);
            case "wars" -> handleWars(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * 查看经济统计
     * /ecowar stats [国家]
     */
    private void handleStats(Player player, String[] args) {
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

        EconomyStatsManager.EconomyStats stats = statsManager.getLatestStats(nationName);
        if (stats == null) {
            player.sendMessage("§c暂无统计数据");
            return;
        }

        player.sendMessage("§6§l========== " + nationName + " 经济统计 ==========");
        player.sendMessage("§7M0 (实体货币): §e" + String.format("%.2f", stats.m0));
        player.sendMessage("§7M1 (M0 + 存款): §e" + String.format("%.2f", stats.m1));
        player.sendMessage("§7M2 (M1 + 贷款): §e" + String.format("%.2f", stats.m2));
        player.sendMessage("§7GDP (24h): §e" + String.format("%.2f", stats.gdp));
        player.sendMessage("§7通胀率: " + formatInflationRate(stats.inflationRate));
        player.sendMessage("§7汇率: §e1 NC = " + String.format("%.4f", stats.exchangeRate) + " SBC");
        player.sendMessage("§6§l==========================================");
    }

    /**
     * 查看 GDP 排行
     * /ecowar gdp
     */
    private void handleGDP(Player player, String[] args) {
        Map<String, Double> gdpRanking = statsManager.getGDPRanking();

        if (gdpRanking.isEmpty()) {
            player.sendMessage("§c暂无数据");
            return;
        }

        player.sendMessage("§6§l========== GDP 排行榜 ==========");
        int rank = 1;
        for (Map.Entry<String, Double> entry : gdpRanking.entrySet()) {
            player.sendMessage("§e#" + rank + " §f" + entry.getKey() + ": §a" +
                String.format("%.2f", entry.getValue()));
            rank++;
        }
        player.sendMessage("§6§l================================");
    }

    /**
     * 查看通胀率
     * /ecowar inflation [国家]
     */
    private void handleInflation(Player player, String[] args) {
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

        EconomyStatsManager.EconomyStats stats = statsManager.getLatestStats(nationName);
        if (stats == null) {
            player.sendMessage("§c暂无统计数据");
            return;
        }

        player.sendMessage("§6========== " + nationName + " 通胀率 ==========");
        player.sendMessage("§7当前通胀率: " + formatInflationRate(stats.inflationRate));

        if (stats.inflationRate > 0.5) {
            player.sendMessage("§c§l警告: 严重通货膨胀！");
        } else if (stats.inflationRate < -0.3) {
            player.sendMessage("§c§l警告: 严重通货紧缩！");
        } else if (stats.inflationRate > 0.1) {
            player.sendMessage("§e注意: 温和通货膨胀");
        } else if (stats.inflationRate < -0.1) {
            player.sendMessage("§e注意: 温和通货紧缩");
        } else {
            player.sendMessage("§a经济稳定");
        }

        player.sendMessage("§6====================================");
    }

    /**
     * 实施禁运
     * /ecowar embargo <目标国家> <天数>
     */
    private void handleEmbargo(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /ecowar embargo <目标国家> <天数>");
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
            player.sendMessage("§c只有国王可以实施禁运！");
            return;
        }

        String targetNation = args[1];
        int days;
        try {
            days = Integer.parseInt(args[2]);
            if (days <= 0 || days > 365) {
                player.sendMessage("§c天数必须在 1-365 之间！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的天数！");
            return;
        }

        boolean success = warManager.imposeEmbargo(nation.getName(), targetNation, days);
        if (success) {
            player.sendMessage("§a成功对 " + targetNation + " 实施资源禁运！");
            player.sendMessage("§7禁运期限: " + days + " 天");
        } else {
            player.sendMessage("§c禁运失败！可能已经在禁运中");
        }
    }

    /**
     * 解除禁运
     * /ecowar lift <目标国家>
     */
    private void handleLift(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /ecowar lift <目标国家>");
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
            player.sendMessage("§c只有国王可以解除禁运！");
            return;
        }

        String targetNation = args[1];

        boolean success = warManager.liftEmbargo(nation.getName(), targetNation);
        if (success) {
            player.sendMessage("§a成功解除对 " + targetNation + " 的禁运！");
        } else {
            player.sendMessage("§c解除失败！可能没有在禁运中");
        }
    }

    /**
     * 实施经济制裁
     * /ecowar sanction <目标国家> <关税率>
     */
    private void handleSanction(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /ecowar sanction <目标国家> <关税率(0-1)>");
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
            player.sendMessage("§c只有国王可以实施制裁！");
            return;
        }

        String targetNation = args[1];
        double tariffRate;
        try {
            tariffRate = Double.parseDouble(args[2]);
            if (tariffRate < 0 || tariffRate > 1) {
                player.sendMessage("§c关税率必须在 0-1 之间！");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的关税率！");
            return;
        }

        boolean success = warManager.imposeSanction(nation.getName(), targetNation, tariffRate);
        if (success) {
            player.sendMessage("§a成功对 " + targetNation + " 实施经济制裁！");
            player.sendMessage("§7额外关税: " + String.format("%.0f%%", tariffRate * 100));
        } else {
            player.sendMessage("§c制裁失败！");
        }
    }

    /**
     * 查看经济战争列表
     * /ecowar wars
     */
    private void handleWars(Player player, String[] args) {
        player.sendMessage("§6§l========== 经济战争列表 ==========");
        player.sendMessage("§7功能开发中...");
        player.sendMessage("§6§l==================================");
    }

    /**
     * 格式化通胀率显示
     */
    private String formatInflationRate(double rate) {
        String color;
        if (rate > 0.5) {
            color = "§c§l"; // 红色加粗
        } else if (rate > 0.1) {
            color = "§e"; // 黄色
        } else if (rate < -0.3) {
            color = "§c§l"; // 红色加粗
        } else if (rate < -0.1) {
            color = "§e"; // 黄色
        } else {
            color = "§a"; // 绿色
        }

        return color + String.format("%.2f%%", rate * 100);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 经济战争系统 ==========");
        player.sendMessage("§e/ecowar stats [国家] §7- 查看经济统计");
        player.sendMessage("§e/ecowar gdp §7- 查看 GDP 排行");
        player.sendMessage("§e/ecowar inflation [国家] §7- 查看通胀率");
        player.sendMessage("§e/ecowar embargo <国家> <天数> §7- 实施禁运");
        player.sendMessage("§e/ecowar lift <国家> §7- 解除禁运");
        player.sendMessage("§e/ecowar sanction <国家> <关税> §7- 经济制裁");
        player.sendMessage("§e/ecowar wars §7- 查看经济战争");
        player.sendMessage("§6==================================");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("stats", "gdp", "inflation", "embargo", "lift", "sanction", "wars");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("stats") ||
            args[0].equalsIgnoreCase("inflation") ||
            args[0].equalsIgnoreCase("embargo") ||
            args[0].equalsIgnoreCase("lift") ||
            args[0].equalsIgnoreCase("sanction"))) {
            List<String> nations = new ArrayList<>();
            for (Nation nation : TownyAPI.getInstance().getNations()) {
                nations.add(nation.getName());
            }
            return nations;
        }

        return new ArrayList<>();
    }
}
