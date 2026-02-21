package net.chen.legacyLand.events;

import lombok.Getter;
import net.chen.legacyLand.war.War;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * 战争开始事件
 */
@Getter
public class WarStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final War war;

    public WarStartEvent(War war) {
        this.war = war;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
