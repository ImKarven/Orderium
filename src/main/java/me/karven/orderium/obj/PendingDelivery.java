package me.karven.orderium.obj;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PendingDelivery {
    private final @NotNull Collection<ItemStack> items;
    private final AtomicBoolean claimed = new AtomicBoolean();

    public PendingDelivery(final @NotNull Collection<ItemStack> items) {
        this.items = items;
    }

    public @NotNull Collection<ItemStack> items() {
        return items;
    }

    // Returns true only for the first caller, who may then use the items
    public boolean claim() {
        return claimed.compareAndSet(false, true);
    }
}
