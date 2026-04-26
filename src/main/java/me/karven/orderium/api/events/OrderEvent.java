package me.karven.orderium.api.events;

import me.karven.orderium.api.Order;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class OrderEvent extends Event {
    private final Order order;

    protected OrderEvent(Order order) {
        this(order, false);
    }

    protected OrderEvent(Order order, boolean isAsync) {
        super(isAsync);
        this.order = order;
    }

    public @NotNull Order getOrder() {
        return order;
    }
}
