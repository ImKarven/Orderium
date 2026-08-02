package me.karven.orderium.guiframework;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InventoryItem {
    private ItemStack item;
    private Consumer<InventoryClickEvent> onClick = null;

    public InventoryItem(ItemStack item, Consumer<InventoryClickEvent> onClick) {
        this(item);
        this.onClick = onClick;
    }

    public void callAction(@NotNull InventoryClickEvent event) {
        if (onClick != null) onClick.accept(event);
    }

    public InventoryItem(ItemStack item) {
        this.item = item;
    }

    public void setOnClick(@NotNull Consumer<@NotNull InventoryClickEvent> onClick) {
        this.onClick = onClick;
    }

    public void setItem(@NotNull ItemStack item) {
        this.item = item;
    }

    public @NotNull ItemStack getItem() {
        return item;
    }

    public @Nullable Consumer<@NotNull InventoryClickEvent> getOnClick() {
        return onClick;
    }
}
