package net.chen.legacyLand.events;

import com.palmergames.bukkit.towny.object.Nation;
import lombok.Getter;
import net.chen.legacyLand.nation.NationRole;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerObtainsRoleEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final NationRole nationRole;
    private final Player player;
    private final Nation nation;
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public PlayerObtainsRoleEvent(Nation nation,NationRole nationRole, Player player){
        this.nation = nation;
        this.nationRole = nationRole;
        this.player = player;
    }

}
