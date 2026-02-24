package net.chen.legacyLand.command;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.politics.PoliticalSystemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdministrationCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player){
            if (!sender.isOp()) {
                sender.sendMessage("ʡYou don't have permission to do that!");
                return true;
            }
        }
        if (args.length<1){
            sender.sendMessage("Usage:");
            sender.sendMessage("Reload the configurations: /legacylandadmin reload");
            return true;
        }else if (args.length == 1){
            LegacyLand.getInstance().getConfigManager().reloadConfig();
            PoliticalSystemManager.getInstance().load(LegacyLand.getInstance());
            sender.sendMessage("Loaded!");
            return true;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
        }
        return completions;
    }
}
