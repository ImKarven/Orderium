package me.karven.orderium.api.events;

import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerDeliverOrderEvent extends OrderiumPlayerEvent {
    private final int amount;

    public PlayerDeliverOrderEvent(@NotNull Player player, @NotNull Order order, int amount) {
        super(player, order);
        this.amount = amount;
    }

    public int getAmount() { return amount; }
}
