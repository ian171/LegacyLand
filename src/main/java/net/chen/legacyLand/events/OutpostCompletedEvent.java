package net.chen.legacyLand.events;

import lombok.Getter;
import net.chen.legacyLand.war.siege.Outpost;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * 前哨战完成事件（维持1小时后）
 */
@Getter
public class OutpostCompletedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Outpost outpost;
    private final String warId;

    public OutpostCompletedEvent(Outpost outpost, String warId) {
        this.outpost = outpost;
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
