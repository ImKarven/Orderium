package me.karven.orderium.guiframework;

import me.karven.orderium.utils.PDCUtils;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class InventoryItem {
    private static final AtomicInteger ID_COUNT = new AtomicInteger();
    private ItemStack item;
    private Consumer<InventoryClickEvent> onClick = null;
    private final int id;

    public InventoryItem(ItemStack item, Consumer<InventoryClickEvent> onClick) {
        this(item);
        this.onClick = onClick;
    }

    public int getId() {
        return id;
    }

    public void callAction(@NotNull InventoryClickEvent event) {
        if (onClick != null) onClick.accept(event);
    }

    public void addToGUI(InventoryGUI gui, int slot) {
        gui.getItems().put(this.id, this);
        gui.getInventory().setItem(slot, item);
    }

    public InventoryItem(ItemStack item) {
        this.id = incrementAndGetID();
        item.editMeta(meta -> {
            PDCUtils.setID(meta, id);
        });
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

    private int incrementAndGetID() {
        return ID_COUNT.incrementAndGet();
    }
}
