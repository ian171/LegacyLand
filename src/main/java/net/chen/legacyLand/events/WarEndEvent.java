package net.chen.legacyLand.events;

import lombok.Getter;
import net.chen.legacyLand.war.War;
import net.chen.legacyLand.war.WarStatus;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 战争结束事件
 */
@Getter
public class WarEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final War war;
    private final WarStatus endStatus;
    private final String winner;
    private final String loser;

    public WarEndEvent(War war, WarStatus endStatus, String winner, String loser) {
        this.war = war;
        this.endStatus = endStatus;
        this.winner = winner;
        this.loser = loser;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
