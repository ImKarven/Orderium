package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerCollectItemsEvent {
    private PlayerCollectItemsEvent() {}

    public static class Pre extends OrderiumPlayerEvent implements Cancellable {
        private static final HandlerList HANDLER_LIST =  new HandlerList();
        private boolean cancelled = false;
        private final int amount;

        public Pre(@NotNull Player player, int amount, @NotNull Order order, boolean isAsync) {
            super(player, order, isAsync);
            this.amount = amount;
        }

        public int getAmount() {
            return amount;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            cancelled = cancel;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }
    }

    public static class Post extends OrderiumPlayerEvent {
        private static final HandlerList HANDLER_LIST =  new HandlerList();
        private final int amount;

        public Post(@NotNull Player player, int amount, @NotNull Order order, boolean isAsync) {
            super(player, order, isAsync);
            this.amount = amount;
        }

        public int getAmount() {
            return amount;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }
    }
}
