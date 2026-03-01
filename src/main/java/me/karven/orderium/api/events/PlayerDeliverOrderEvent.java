package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerDeliverOrderEvent extends PlayerEvent {
    /**
     * Get the amount of items the player is trying to order
     */
    private final int amount;

    /**
     * Get the order involved in this event
     */
    private final Order order;

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PlayerDeliverOrderEvent(@NotNull Player player, @NotNull Order order, int amount) {
        super(player);
        this.order = order;
        this.amount = amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() { return HANDLER_LIST; }
}
