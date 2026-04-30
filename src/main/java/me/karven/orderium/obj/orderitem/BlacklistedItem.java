package me.karven.orderium.obj.orderitem;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class BlacklistedItem implements SerializedItem, OrderItem {
    private final byte[] itemAsBytes;
    private ItemStack item;

    public BlacklistedItem(byte @NotNull [] itemAsBytes, @NotNull ItemStack item) {
        this.itemAsBytes = itemAsBytes;
        this.item = item;
    }

    public BlacklistedItem(byte @NotNull [] itemAsBytes) {
        this(itemAsBytes, ItemStack.deserializeBytes(itemAsBytes));
    }

    @Override
    public byte @NotNull [] getItemAsBytes() {
        return itemAsBytes;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return item.clone();
    }

    public @NotNull BlacklistedItem copy() {
        return new BlacklistedItem(itemAsBytes, item.clone());
    }

    @Override
    public void setItemStack(@NonNull ItemStack itemStack) {
        this.item = itemStack;
    }
}
