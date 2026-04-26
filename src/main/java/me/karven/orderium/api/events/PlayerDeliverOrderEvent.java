package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerDeliverOrderEvent {
    private PlayerDeliverOrderEvent() {};

    // TODO: Allow user to retrieve amount of items being delivered
    public static class Pre extends OrderiumPlayerEvent implements Cancellable {
        private static final HandlerList HANDLER_LIST = new HandlerList();
        private boolean isCancelled = false;

        public Pre(@NotNull Player player, @NotNull Order order, boolean isAsync) {
            super(player, order, isAsync);
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.isCancelled = cancel;
        }
    }

    public static class Post extends OrderiumPlayerEvent {
        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Post(@NotNull Player player, @NotNull Order order, boolean isAsync) {
            super(player, order, isAsync);
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }
    }
}
