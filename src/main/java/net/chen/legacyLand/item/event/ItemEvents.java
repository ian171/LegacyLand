package net.chen.legacyLand.item.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class ItemEvents {
    public static class ItemClickEvent extends Event {
        @Getter
        private ItemStack itemStack;
        @Getter
        private Player player;
        @Getter
        private Date date;
        private final HandlerList handlerList = new HandlerList();
        @Override
        public @NotNull HandlerList getHandlers() {
            return handlerList;
        }
        public ItemClickEvent(ItemStack itemStack, Player player, Date date){
            this.itemStack = itemStack;
            this.player = player;
            this.date = date;
        }

    }
}
