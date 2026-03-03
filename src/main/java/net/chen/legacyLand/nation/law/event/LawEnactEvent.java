package net.chen.legacyLand.nation.law.event;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import lombok.Getter;
import net.chen.legacyLand.nation.law.LawType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class LawEnactEvent extends Event {
    private final HandlerList list = new HandlerList();
    private final LawType lawType;
    private final Player who;
    private final Nation where;
    private final Town town;
    private final long startWhen;
    @Override
    public @NotNull HandlerList getHandlers() {
        return list;
    }
    public LawEnactEvent(@NotNull LawType lawType, @NotNull Player who, long startWhen) {
        this.lawType = lawType;
        this.who = who;
        this.where = TownyAPI.getInstance().getNation(who);
        this.town = TownyAPI.getInstance().getTown(who);
        this.startWhen = startWhen;
    }
}
