package net.chen.legacyLand.events;

import lombok.Getter;
import net.chen.legacyLand.war.siege.Outpost;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Deprecated(forRemoval = true)
@Getter
public class OutpostDiscovered extends Event {
    private final Player player;
    private final Outpost outpost;
    private static final HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public OutpostDiscovered(Player player, Outpost outpost){
        this.outpost = outpost;
        this.player = player;
    }
}
