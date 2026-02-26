package me.karven.orderium.api.events;

import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerCollectItemsEvent extends OrderiumPlayerEvent {
    private final int amount;

    public PlayerCollectItemsEvent(@NotNull Player player, @NotNull Order order, int amount) {
        super(player, order);
        this.amount = amount;
    }

    public int getAmount() { return amount; }
}
