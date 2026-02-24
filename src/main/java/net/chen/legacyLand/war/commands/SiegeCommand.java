package net.chen.legacyLand.war.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import net.chen.legacyLand.war.War;
import net.chen.legacyLand.war.WarManager;
import net.chen.legacyLand.war.siege.Outpost;
import net.chen.legacyLand.war.siege.SiegeWar;
import net.chen.legacyLand.war.siege.SiegeWarManager;
import net.chen.legacyLand.war.siege.SupplyStation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 攻城战命令处理器
 */
public class SiegeCommand implements CommandExecutor, TabCompleter {

    private final WarManager warManager;
    private final SiegeWarManager siegeWarManager;
    private final TownyAPI townyAPI;

    public SiegeCommand() {
        this.warManager = WarManager.getInstance();
        this.siegeWarManager = SiegeWarManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "outpost" -> handleOutpost(player);
            case "station" -> handleStation(player, args);
            case "supply" -> handleSupply(player, args);
            case "core" -> handleCore(player, args);
            case "info" -> handleInfo(player);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handleOutpost(Player player) {
        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        if (war.getStatus().isEnded()) {
            player.sendMessage("§c战争已结束！");
            return true;
        }

        // 检查脚下是否有信标
        Location location = player.getLocation();
        location.setY(location.getY() - 1);
        if (location.getBlock().getType() != Material.BEACON) {
            player.sendMessage("§c/你必须站在信标上才能建立前哨战！");
            return true;
        }else {

        }

        // 获取攻城战
        SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
        if (siegeWar == null) {
            player.sendMessage("§c攻城战不存在！");
            return true;
        }

        // 检查是否已有前哨战
        if (siegeWar.getOutpost() != null && siegeWar.getOutpost().isActive()) {
            player.sendMessage("§c前哨战已存在！");
            return true;
        }

        // 创建前哨战
        Outpost outpost = new Outpost(war.getWarName(), location, player.getUniqueId());
        siegeWarManager.establishOutpost(siegeWar.getSiegeId(), outpost);

        // 触发事件
        net.chen.legacyLand.events.OutpostEstablishedEvent event =
            new net.chen.legacyLand.events.OutpostEstablishedEvent(outpost, player, war.getWarName());
        org.bukkit.Bukkit.getPluginManager().callEvent(event);

        player.sendMessage("§a前哨战已建立！");
        player.sendMessage("§e需要至少2人在附近维持1小时才能开战。");
        player.sendMessage("§e如果被敌方发现，前哨战将失效。");

        // 保存数据
        net.chen.legacyLand.LegacyLand.getInstance().getDatabaseManager().saveSiegeWar(siegeWar.toMap());

        return true;
    }

    private boolean handleStation(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /siege station <create|destroy>");
            return true;
        }

        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
        if (siegeWar == null) {
            player.sendMessage("§c攻城战不存在！");
            return true;
        }

        if (args[1].equalsIgnoreCase("create")) {
            return handleStationCreate(player, war, siegeWar);
        } else if (args[1].equalsIgnoreCase("destroy")) {
            return handleStationDestroy(player, siegeWar);
        } else {
            player.sendMessage("§c用法: /siege station <create|destroy>");
            return true;
        }
    }

    private boolean handleStationCreate(Player player, War war, SiegeWar siegeWar) {
        // 检查脚下是否有信标
        Location location = player.getLocation();
        if (location.getBlock().getType() != Material.BEACON) {
            player.sendMessage("§c你必须站在信标上才能建立补给站！");
            return true;
        }

        // 获取玩家所在城镇
        Town playerTown = townyAPI.getTown(player);
        if (playerTown == null) {
            player.sendMessage("§c你不在任何城镇中！");
            return true;
        }

        // 创建补给站
        SupplyStation station = new SupplyStation(
            war.getWarName(),
            playerTown.getName(),
            location,
            player.getUniqueId()
        );

        if (siegeWarManager.createSupplyStation(siegeWar.getSiegeId(), station)) {
            player.sendMessage("§a补给站已建立！");
            player.sendMessage("§e费用: 10金币");
            player.sendMessage("§e最多可建立8个补给站。");
        } else {
            player.sendMessage("§c建立失败！可能已达到上限（8个）。");
        }

        return true;
    }

    private boolean handleStationDestroy(Player player, SiegeWar siegeWar) {
        Location location = player.getLocation();

        if (siegeWarManager.destroySupplyStation(siegeWar.getSiegeId(), location)) {
            player.sendMessage("§a补给站已摧毁！");
        } else {
            player.sendMessage("§c这里没有补给站！");
        }

        return true;
    }

    private boolean handleSupply(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /siege supply <add|take>");
            return true;
        }

        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        player.sendMessage("§e补给系统功能开发中...");
        player.sendMessage("§7提示: 补给站可以存储食物和武器弹药。");

        return true;
    }

    private boolean handleCore(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /siege core <add|destroy> <核心名>");
            return true;
        }

        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
        if (siegeWar == null) {
            player.sendMessage("§c攻城战不存在！");
            return true;
        }

        String coreName = args[2];
        Location location = player.getLocation();

        if (args[1].equalsIgnoreCase("add")) {
            siegeWarManager.addCityCore(siegeWar.getSiegeId(), coreName, location);
            player.sendMessage("§a城市核心 " + coreName + " 已添加！");
        } else if (args[1].equalsIgnoreCase("destroy")) {
            siegeWarManager.destroyCore(siegeWar.getSiegeId(), coreName);
            player.sendMessage("§c核心 " + coreName + " 已被摧毁！");

            // 检查战争胜负
            warManager.checkWarConditions(war.getWarName(), siegeWar);
        } else {
            player.sendMessage("§c用法: /siege core <add|destroy> <核心名>");
        }

        return true;
    }

    private boolean handleInfo(Player player) {
        War war = warManager.getPlayerWar(player);

        if (war == null) {
            player.sendMessage("§c你没有参与任何战争！");
            return true;
        }

        SiegeWar siegeWar = siegeWarManager.getSiegeWarByWarId(war.getWarName());
        if (siegeWar == null) {
            player.sendMessage("§c攻城战不存在！");
            return true;
        }

        player.sendMessage("§6========== 攻城战信息 ==========");
        player.sendMessage("§e攻城战ID: §f" + siegeWar.getSiegeId());
        player.sendMessage("§e攻方城镇: §f" + siegeWar.getAttackerTown());
        player.sendMessage("§e守方城镇: §f" + siegeWar.getDefenderTown());

        // 前哨战信息
        Outpost outpost = siegeWar.getOutpost();
        if (outpost != null) {
            player.sendMessage("§e前哨战状态: §f" + (outpost.isActive() ? "活跃" : "失效"));
            player.sendMessage("§e前哨战准备: §f" + (outpost.isReady() ? "已就绪" : "未就绪"));
        } else {
            player.sendMessage("§e前哨战: §c未建立");
        }

        // 补给站信息
        player.sendMessage("§e攻方补给站: §f" + siegeWar.getSupplyStationCount(siegeWar.getAttackerTown()));
        player.sendMessage("§e守方补给站: §f" + siegeWar.getSupplyStationCount(siegeWar.getDefenderTown()));

        // 核心信息
        int totalCores = siegeWar.getCityCores().size() + siegeWar.getDistrictCores().size();
        int destroyedCores = siegeWar.getDestroyedCores().size();
        player.sendMessage("§e核心状态: §f" + (totalCores - destroyedCores) + "/" + totalCores);

        // 补给线信息
        boolean supplyLineCut = siegeWar.isSupplyLineCut();
        player.sendMessage("§e补给线: §f" + (supplyLineCut ? "§c已切断" : "§a正常"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== 攻城战命令帮助 ==========");
        player.sendMessage("§e/siege outpost §7- 建立前哨战（需站在信标上）");
        player.sendMessage("§e/siege station create §7- 建立补给站");
        player.sendMessage("§e/siege station destroy §7- 摧毁补给站");
        player.sendMessage("§e/siege supply <add|take> §7- 管理补给");
        player.sendMessage("§e/siege core <add|destroy> <名称> §7- 管理核心");
        player.sendMessage("§e/siege info §7- 查看攻城战信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("outpost", "station", "supply", "core", "info"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("station")) {
                completions.addAll(Arrays.asList("create", "destroy"));
            } else if (args[0].equalsIgnoreCase("supply")) {
                completions.addAll(Arrays.asList("add", "take"));
            } else if (args[0].equalsIgnoreCase("core")) {
                completions.addAll(Arrays.asList("add", "destroy"));
            }
        }

        return completions;
    }
}
