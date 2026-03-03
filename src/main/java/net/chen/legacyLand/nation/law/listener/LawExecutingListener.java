package net.chen.legacyLand.nation.law.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.law.LawManager;
import net.chen.legacyLand.nation.law.LawType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class LawExecutingListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent moveEvent){
        Player player = moveEvent.getPlayer();
        LawManager lawManager = LegacyLand.getInstance().getLawManager();
        Nation nation = TownyAPI.getInstance().getNation(player);
        if (nation != null){
            if (lawManager.hasActiveLaw(nation.getName(),LawType.CURFEW)){
                Location from = moveEvent.getFrom();
                Location to = moveEvent.getTo();
                Town town =  TownyAPI.getInstance().getTown(player);
                if (town != null && !town.isInsideTown(to)) {
                    if (town.isInsideTown(from)){
                        try {
                            player.teleport(town.getSpawn());
                        } catch (TownyException ignored) {
                        }
                        player.sendMessage("你不可以在宵禁期间走出城镇");
                    }else {
                        player.sendMessage("都宵禁了，快点回城");
                        try {
                            player.teleport(town.getSpawn(), PlayerTeleportEvent.TeleportCause.COMMAND);
                        } catch (TownyException ignored) {

                        }
                    }
                }
            }
        }
    }
}
