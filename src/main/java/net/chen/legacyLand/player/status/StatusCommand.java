package net.chen.legacyLand.player.status;

import net.chen.legacyLand.player.PlayerData;
import net.chen.legacyLand.player.PlayerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 状态命令
 */
public class StatusCommand implements CommandExecutor, TabCompleter {

    private final PlayerStatusManager statusManager;
    private final TemperatureManager temperatureManager;
    private final PlayerManager playerManager;

    public StatusCommand() {
        this.statusManager = PlayerStatusManager.getInstance();
        this.temperatureManager = TemperatureManager.getInstance();
        this.playerManager = PlayerManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (args.length == 0) {
            showStatus(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info", "查看" -> showStatus(player);
            case "heal", "治疗" -> {
                if (!player.hasPermission("legacyland.status.heal")) {
                    player.sendMessage("§c你没有权限使用此命令！");
                    return true;
                }
                healPlayer(player);
            }
            default -> {
                player.sendMessage("§c未知的子命令！");
                player.sendMessage("§e用法: /status <info|heal>");
            }
        }

        return true;
    }

    /**
     * 显示玩家状态
     */
    private void showStatus(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage("§c无法获取玩家数据！");
            return;
        }

        player.sendMessage("§6========== §e玩家状态 §6==========");

        // 基础信息
        double healthPercent = player.getHealth() / player.getMaxHealth() * 100;
        int foodLevel = player.getFoodLevel();
        double foodPercent = foodLevel / 20.0 * 100;

        player.sendMessage("§a生命值: §f" + String.format("%.1f", player.getHealth()) + "/" +
                          String.format("%.1f", player.getMaxHealth()) + " §7(" + String.format("%.0f", healthPercent) + "%)");
        player.sendMessage("§a饱食度: §f" + foodLevel + "/20 §7(" + String.format("%.0f", foodPercent) + "%)");
        player.sendMessage("§a饮水值: §f" + playerData.getHydration() + "/10");

        // 温度
        double temperature = playerData.getTemperature();
        String tempColor = temperatureManager.getTemperatureColor(temperature);
        String tempDesc = temperatureManager.getTemperatureDescription(temperature);
        player.sendMessage("§a体温: " + tempColor + String.format("%.1f", temperature) + "°C §7(" + tempDesc + ")");

        // 身体状态
        BodyStatus bodyStatus = statusManager.getPlayerBodyStatus().get(player.getUniqueId());
        if (bodyStatus != null && bodyStatus != BodyStatus.NORMAL) {
            player.sendMessage("§c身体状态: §f" + bodyStatus.getDisplayName() + " §7- " + bodyStatus.getDescription());
        } else {
            player.sendMessage("§a身体状态: §f正常");
        }

        // 受伤状态（战场）
        Set<InjuryStatus> injuries = statusManager.getPlayerInjuryStatus().get(player.getUniqueId());
        if (injuries != null && !injuries.isEmpty()) {
            player.sendMessage("§c战场伤势:");
            for (InjuryStatus injury : injuries) {
                player.sendMessage("  §7- §f" + injury.getDisplayName());
            }
        }

        // 受伤状态（生活）
        Set<LifeInjuryStatus> lifeInjuries = statusManager.getPlayerLifeInjuryStatus().get(player.getUniqueId());
        if (lifeInjuries != null && !lifeInjuries.isEmpty()) {
            player.sendMessage("§c生活伤势:");
            for (LifeInjuryStatus injury : lifeInjuries) {
                player.sendMessage("  §7- §f" + injury.getDisplayName());
            }
        }

        player.sendMessage("§6================================");
    }

    /**
     * 治疗玩家
     */
    private void healPlayer(Player player) {
        // 恢复生命值
        player.setHealth(player.getMaxHealth());

        // 恢复饱食度
        player.setFoodLevel(20);

        // 恢复饮水值
        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        if (playerData != null) {
            playerData.setHydration(10);
            playerData.setTemperature(22.0);
        }

        // 清除所有状态效果
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // 清除受伤状态
        statusManager.getPlayerInjuryStatus().remove(player.getUniqueId());
        statusManager.getPlayerLifeInjuryStatus().remove(player.getUniqueId());
        statusManager.getPlayerBodyStatus().put(player.getUniqueId(), BodyStatus.NORMAL);

        player.sendMessage("§a你已被完全治愈！");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            completions.add("heal");
            completions.add("查看");
            completions.add("治疗");
        }

        return completions;
    }
}
