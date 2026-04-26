package me.karven.orderium.obj.orderitem;

import org.jetbrains.annotations.NotNull;

/// Represent an item that is serialized as bytes
public interface SerializedItem {

    /**
     * Get a byte array representing the item stack, serialized using paper API
     * This byte array is either from the database or serialized from the item when adding a new custom item
     * @return the byte array
     */
    byte @NotNull [] getItemAsBytes();
}
