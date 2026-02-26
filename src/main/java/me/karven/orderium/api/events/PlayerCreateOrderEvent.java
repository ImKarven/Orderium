package me.karven.orderium.api.events;

import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerCreateOrderEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean isCancelled = false;

    private final ItemStack item;
    private final double moneyPer;
    private final int amount;


    public PlayerCreateOrderEvent(@NotNull Player player, @NotNull ItemStack item, double moneyPer, int amount) {
        super(player);
        this.item = item;
        this.moneyPer = moneyPer;
        this.amount = amount;
    }

    public ItemStack getItem() { return item; }
    public double getMoneyPer() { return moneyPer; }
    public int getAmount() { return amount; }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() { return HANDLER_LIST; }
}
