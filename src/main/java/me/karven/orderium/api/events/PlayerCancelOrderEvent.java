package me.karven.orderium.api.events;

import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerCancelOrderEvent extends OrderiumPlayerEvent {

    public PlayerCancelOrderEvent(@NotNull Player player, @NotNull Order order) {
        super(player, order);
    }
}
