package me.karven.orderium.folia;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui;
import me.karven.orderium.utils.DispatchUtil;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GuiListener implements Listener {
    private final com.github.stefvanschie.inventoryframework.gui.GuiListener ifListener;

    public GuiListener(com.github.stefvanschie.inventoryframework.gui.GuiListener ifListener) {
        this.ifListener = ifListener;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        try {
            ifListener.onInventoryClick(event);
        } catch (UnsupportedOperationException e) {

            HumanEntity who = event.getWhoClicked();

            DispatchUtil.entity(who, () -> {
                PlayerInventory playerInventory = who.getInventory();
                playerInventory.setItemInOffHand(playerInventory.getItemInOffHand());
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityPickupItem(@NotNull EntityPickupItemEvent event) { ifListener.onEntityPickupItem(event); }

    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) { ifListener.onInventoryDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onTradeSelect(@NotNull TradeSelectEvent event) { ifListener.onTradeSelect(event); }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        try {
            ifListener.onInventoryClose(event);
        } catch (UnsupportedOperationException e) {
            Gui gui = getGui(event.getInventory());

            if (gui == null || isNamedGuiUpdatingDirtily(gui)) {
                return;
            }

            HumanEntity humanEntity = event.getPlayer();
            DispatchUtil.entity(humanEntity, () -> gui.navigateToParent(humanEntity));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) { ifListener.onInventoryOpen(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPluginDisable(@NotNull PluginDisableEvent event) { ifListener.onPluginDisable(event); }

    @Nullable
    @Contract(pure = true)
    private Gui getGui(@NotNull Inventory inventory) {
        Gui gui = Gui.getGui(inventory);

        if (gui != null) {
            return gui;
        }

        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof Gui) {
            return (Gui) holder;
        }

        return null;
    }

    private boolean isNamedGuiUpdatingDirtily(@NotNull Gui gui) {
        boolean dirtyTitle = gui instanceof NamedGui && (((NamedGui) gui).isDirty());
        boolean dirtyRows = gui instanceof ChestGui && ((ChestGui) gui).isDirtyRows();
        return gui.isUpdating() && (dirtyTitle || dirtyRows);
    }

}