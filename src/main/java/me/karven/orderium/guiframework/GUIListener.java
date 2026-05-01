package me.karven.orderium.guiframework;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GUIListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        InventoryGUI gui = getGUI(event);
        if (gui == null) return;

        gui.callClickAction(event, InteractLocation.GLOBAL);
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            gui.callClickAction(event, InteractLocation.OUTSIDE);
        } else {
            InventoryHolder clickedHolder = clickedInventory.getHolder();
            if (gui.equals(clickedHolder)) gui.callClickAction(event, InteractLocation.TOP);
            else gui.callClickAction(event, InteractLocation.BOTTOM);
        }
        gui.click(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        InventoryGUI gui = getGUI(event);
        if (gui == null) return;
        gui.callDragAction(event, InteractLocation.GLOBAL);
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        InventoryGUI gui = getGUI(event);
        if (gui == null) return;
        gui.callOnClose(event);
    }

    private @Nullable InventoryGUI getGUI(@NotNull InventoryEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof InventoryGUI gui)) return null;
        return gui;
    }
}
