package net.chen.legacyLand.events;

import lombok.Getter;
import net.chen.legacyLand.war.siege.Outpost;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 前哨战建立事件
 */
@Getter
public class OutpostEstablishedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Outpost outpost;
    private final Player establisher;
    private final String warId;

    public OutpostEstablishedEvent(Outpost outpost, Player establisher, String warId) {
        this.outpost = outpost;
        this.establisher = establisher;
        this.warId = warId;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
