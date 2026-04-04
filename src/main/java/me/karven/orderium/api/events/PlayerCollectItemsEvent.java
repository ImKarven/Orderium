package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerCollectItemsEvent extends OrderiumPlayerEvent {
    private final int amount;

    /**
     * Fired when the player attempts to collect items from an order of theirs
     */
    public PlayerCollectItemsEvent(@NotNull Player player, @NotNull Order order, int amount) {
        super(player, order);
        this.amount = amount;
    }

}
