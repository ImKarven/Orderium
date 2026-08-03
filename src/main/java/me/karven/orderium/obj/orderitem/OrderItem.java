package me.karven.orderium.obj.orderitem;

import me.karven.orderium.utils.PDCUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public interface OrderItem {

    /**
     * Get a copy of the bukkit item stack of this item
     * @return the item stack
     */
    @NotNull ItemStack getItemStack();

    void setItemStack(@NotNull ItemStack itemStack);

    static OrderItem fromBytes(final byte @NotNull [] bytes) {
        final ItemStack itemStack = ItemStack.deserializeBytes(bytes);
        final ItemMeta meta = itemStack.getItemMeta();
        if (PDCUtils.hasCustomSearch(meta)) {
            return new CustomItem(bytes, itemStack, PDCUtils.getSearch(meta).split(","));
        }

        return new VanillaItem(itemStack, true);
    }
}
