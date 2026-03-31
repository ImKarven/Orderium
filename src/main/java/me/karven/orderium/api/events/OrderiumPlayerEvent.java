package me.karven.orderium.api.events;

import lombok.Setter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public abstract class OrderiumPlayerEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Setter
    private boolean isCancelled = false;
    private final Order order;

    public static HandlerList getHandlerList() { return HANDLER_LIST; }

    protected OrderiumPlayerEvent(@NotNull Player player, @NotNull Order order) {
        super(player);
        this.order = order;
    }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLER_LIST; }

    public Order getOrder() { return order; }

    public boolean isCancelled() { return isCancelled; }
}
