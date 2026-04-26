package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class OrderiumPlayerEvent extends PlayerEvent {
    private final Order order;

    protected OrderiumPlayerEvent(@NotNull Player player, @NotNull Order order) {
        this(player, order, false);
    }

    protected OrderiumPlayerEvent(@NotNull Player player, @NotNull Order order, boolean isAsync) {
        super(player, isAsync);
        this.order = order;
    }

}
