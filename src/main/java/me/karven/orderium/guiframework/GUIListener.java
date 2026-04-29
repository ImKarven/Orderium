package me.karven.orderium.guiframework;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class GUIListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof InventoryGUI gui)) return;

        gui.callClickAction(event, InteractLocation.GLOBAL);
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;
        InventoryHolder clickedHolder = clickedInventory.getHolder();
        if (holder.equals(clickedHolder)) {
            // Top click
            gui.callClickAction(event, InteractLocation.TOP);
        } else gui.callClickAction(event, InteractLocation.BOTTOM);
        gui.click(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof InventoryGUI gui)) return;

        gui.callDragAction(event, InteractLocation.GLOBAL);
    }
}
