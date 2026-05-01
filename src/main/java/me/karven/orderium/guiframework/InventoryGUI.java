package me.karven.orderium.guiframework;

import com.google.common.base.Preconditions;
import me.karven.orderium.utils.PDCUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InventoryGUI implements InventoryHolder {
    private final int rows;
    private Component title;
    private Inventory handle;
    private final ConcurrentHashMap<Integer, InventoryItem> items = new ConcurrentHashMap<>();
    private Consumer<InventoryClickEvent> onTopClick = null;
    private Consumer<InventoryClickEvent> onBottomClick = null;
    private Consumer<InventoryClickEvent> onGlobalClick = null;
    private Consumer<InventoryClickEvent> onOutsideClick = null;
    private Consumer<InventoryDragEvent> onTopDrag = null;
    private Consumer<InventoryDragEvent> onBottomDrag = null;
    private Consumer<InventoryDragEvent> onGlobalDrag = null;

    private Consumer<InventoryCloseEvent> onClose = null;

    public InventoryGUI(int rows, @NotNull Component title) {
        Preconditions.checkArgument(rows >= 1 && rows <= 6, "Rows must be between 1 and 6");
        this.rows = rows;
        this.title = title;
        this.handle = Bukkit.createInventory(this, rows * 9, title);
    }

    public void open(@NotNull HumanEntity player) {
        player.openInventory(handle);
    }

    public @NotNull Component getTitle() { return title; }
    public void setTitle(@NotNull Component title) { this.title = title; }
    public @NotNull Map<@NotNull Integer, @NotNull InventoryItem> getItems() { return items; }
    public int getRows() { return rows; }

    public void click(@NotNull InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        int id = PDCUtils.getID(meta);
        if (id == -1) return;
        InventoryItem item = items.get(id);
        if (item == null) return;
        item.callAction(event);
    }

    public void addItem(@NotNull InventoryItem item, int slot) {
        items.put(item.getId(), item);
        handle.setItem(slot, item.getItem());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return handle;
    }

    public @Nullable Consumer<@NotNull InventoryClickEvent> getOnClick(InteractLocation interactLocation) {
        return switch (interactLocation) {
            case TOP -> onTopClick;
            case BOTTOM -> onBottomClick;
            case GLOBAL -> onGlobalClick;
            case OUTSIDE -> onOutsideClick;
        };
    }

    public @Nullable Consumer<@NotNull InventoryDragEvent> getOnDrag(InteractLocation interactLocation) {
        return switch (interactLocation) {
            case TOP -> onTopDrag;
            case BOTTOM ->  onBottomDrag;
            case GLOBAL -> onGlobalDrag;
            default -> throw new IllegalArgumentException("InteractLocation must not be OUTSIDE for drag events");
        };
    }

    public void setOnClick(@NotNull Consumer<@NotNull InventoryClickEvent> onClick, InteractLocation interactLocation) {
        switch (interactLocation) {
            case TOP -> onTopClick = onClick;
            case BOTTOM -> onBottomClick = onClick;
            case GLOBAL -> onGlobalClick = onClick;
            case OUTSIDE -> onOutsideClick = onClick;
        }
    }

    public void setOnDrag(@NotNull Consumer<@NotNull InventoryDragEvent> onDrag, InteractLocation interactLocation) {
        switch (interactLocation) {
            case TOP -> onTopDrag = onDrag;
            case BOTTOM -> onBottomDrag = onDrag;
            case GLOBAL -> onGlobalDrag = onDrag;
        }
    }

    public @Nullable Consumer<@NotNull InventoryCloseEvent> getOnClose() {
        return onClose;
    }

    public void setOnClose(@NotNull Consumer<@NotNull InventoryCloseEvent> onClose) {
        this.onClose = onClose;
    }

    public void callOnClose(InventoryCloseEvent event) {
        if (onClose != null) onClose.accept(event);
    }

    public void callClickAction(@NotNull InventoryClickEvent event, @NotNull InteractLocation interactLocation) {
        final Consumer<InventoryClickEvent> action = switch (interactLocation) {
            case TOP -> onTopClick;
            case BOTTOM -> onBottomClick;
            case GLOBAL -> onGlobalClick;
            case OUTSIDE -> onOutsideClick;
        };
        if (action == null) return;
        action.accept(event);
    }

    public void callDragAction(@NotNull InventoryDragEvent event, @NotNull InteractLocation interactLocation) {
        final Consumer<InventoryDragEvent> action = switch (interactLocation) {
            case TOP -> onTopDrag;
            case BOTTOM -> onBottomDrag;
            case GLOBAL -> onGlobalDrag;
            case OUTSIDE -> throw new IllegalArgumentException("InteractLocation must not be OUTSIDE for drag events");
        };
        if (action == null) return;
        action.accept(event);
    }

    public void update() {
        List<HumanEntity> oldViewers = handle.getViewers();
        ItemStack[] oldContents = handle.getContents();
        handle = Bukkit.createInventory(this, rows * 9, title);

        ItemStack[] newContents = new ItemStack[oldContents.length];
        for (int i = 0; i < oldContents.length; i++) {
            ItemStack item =  oldContents[i];
            if (item == null || item.isEmpty()) {
                newContents[i] = item;
                continue;
            }
            int id = PDCUtils.getID(item.getItemMeta());
            if (id == -1) {
                newContents[i] = item;
                continue;
            }
            newContents[i] = items.get(id).getItem();
        }

        handle.setContents(newContents);

        for (HumanEntity player : new ArrayList<>(oldViewers)) {
            player.openInventory(handle);
        }
    }
}
