package me.karven.orderium.obj.orderitem;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface OrderItem {

    /**
     * Get a copy of the bukkit item stack of this item
     * @return the item stack
     */
    @NotNull ItemStack getItemStack();

    void setItemStack(@NotNull ItemStack itemStack);
}
