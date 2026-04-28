package me.karven.orderium.obj.orderitem;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class BlacklistedItem implements SerializedItem, OrderItem {
    private final byte[] itemAsBytes;
    private ItemStack item;

    public BlacklistedItem(byte @NotNull [] itemAsBytes) {
        this.itemAsBytes = itemAsBytes;
        this.item = ItemStack.deserializeBytes(itemAsBytes);
    }

    @Override
    public byte @NotNull [] getItemAsBytes() {
        return itemAsBytes;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return item;
    }

    public @NotNull BlacklistedItem copy() {
        BlacklistedItem clone = new BlacklistedItem(itemAsBytes);
        clone.setItemStack(item);
        return clone;
    }

    @Override
    public void setItemStack(@NonNull ItemStack itemStack) {
        this.item = itemStack;
    }
}
