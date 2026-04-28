package me.karven.orderium.api.events;

import me.karven.orderium.api.Order;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is fired when an order is deleted
 * However, the order is never deleted in the storage currently
 * It is only deleted in memory
 * An order is only checked if it should be deleted when the owner enters your orders GUI
 */
public class OrderRemoveEvent {
    private OrderRemoveEvent() {} // For avoiding construction

    public static class Pre extends OrderEvent implements Cancellable {
        private boolean cancelled = false;
        private static final HandlerList HANDLER_LIST = new HandlerList();

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }

        public Pre(Order order, boolean isAsync) {
            super(order, isAsync);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }

    public static class Post extends OrderEvent {
        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Post(Order order, boolean isAsync) {
            super(order, isAsync);
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }
    }
}
