package me.karven.orderium.obj.orderitem;

import me.karven.orderium.utils.Log;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public @Nullable OrderItem clone() {
        try {
            return (OrderItem) super.clone();
        } catch (CloneNotSupportedException e) {
            Log.error("clone failed", e);
        }
        return null;
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        this.item = itemStack;
    }
}
