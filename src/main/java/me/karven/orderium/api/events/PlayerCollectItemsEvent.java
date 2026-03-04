package me.karven.orderium.api.events;

import lombok.Getter;
import me.karven.orderium.api.Order;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerCollectItemsEvent extends OrderiumPlayerEvent {
    private final int amount;
    /**
     * This event is called even if the player is collecting more items than they have in their order storage.
     * This is because we do not know if the value is changed in the database. It should be fine if you use local database.
     */
    public PlayerCollectItemsEvent(@NotNull Player player, @NotNull Order order, int amount) {
        super(player, order);
        this.amount = amount;
    }

}
