package net.chen.legacyLand.events;

import lombok.Getter;
import net.chen.legacyLand.war.siege.Outpost;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * 前哨战被发现事件
 */
@Getter
public class OutpostDiscoveredEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Outpost outpost;
    private final Player discoverer;
    private final String warId;

    public OutpostDiscoveredEvent(Outpost outpost, Player discoverer, String warId) {
        this.outpost = outpost;
        this.discoverer = discoverer;
        this.warId = warId;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
