package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerDeliverOrderEvent extends OrderiumPlayerEvent {
    /**
     * the amount of items the player is trying to order
     */
    private final int amount;

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /**
     * Fired when the player attempts to deliver an order
     */
    public PlayerDeliverOrderEvent(@NotNull Player player, @NotNull Order order, int amount) {
        super(player, order);
        this.amount = amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() { return HANDLER_LIST; }
}
