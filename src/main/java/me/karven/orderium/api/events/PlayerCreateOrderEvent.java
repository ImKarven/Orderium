package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerCreateOrderEvent {
    private PlayerCreateOrderEvent() {}

    public static class Pre extends PlayerEvent implements Cancellable {
        private static final HandlerList HANDLER_LIST = new HandlerList();
        private boolean isCancelled = false;

        private final ItemStack item;
        private final double moneyPer;
        private final int amount;

        /**
         * Fired when a player attempts to create an order
         */
        public Pre(@NotNull Player player, @NotNull ItemStack item, double moneyPer, int amount, boolean isAsync) {
            super(player, isAsync);
            this.item = item;
            this.moneyPer = moneyPer;
            this.amount = amount;
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.isCancelled = cancel;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }

        public @NotNull ItemStack getItem() {
            return item;
        }

        public double getMoneyPer() {
            return moneyPer;
        }

        public int getAmount() {
            return amount;
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
