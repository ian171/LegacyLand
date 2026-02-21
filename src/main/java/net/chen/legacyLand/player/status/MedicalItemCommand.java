package net.chen.legacyLand.player.status;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 医疗物品命令处理器
 * 用于处理 ItemsAdder 触发的命令
 */
public class MedicalItemCommand implements CommandExecutor {

    private final MedicalItemHandler medicalItemHandler;

    public MedicalItemCommand() {
        this.medicalItemHandler = new MedicalItemHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 这个命令只能由控制台执行（ItemsAdder触发）
        if (args.length < 2) {
            return false;
        }

        String playerName = args[0];
        String itemType = args[1];

        Player player = org.bukkit.Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }

        // 处理医疗物品
        if (command.getName().equalsIgnoreCase("legacyland:heal")) {
            MedicalItemType medicalType = MedicalItemType.fromItemId(itemType);
            if (medicalType != null) {
                boolean success = medicalItemHandler.useMedicalItem(player, medicalType);
                if (success) {
                    // 消耗物品（ItemsAdder会自动处理）
                    return true;
                }
            }
        }
        // 处理水壶
        else if (command.getName().equalsIgnoreCase("legacyland:drink")) {
            return medicalItemHandler.useWaterCanteen(player);
        }

        return false;
    }
}
