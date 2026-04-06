package me.karven.orderium.folia;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui;
import lombok.val;
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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listens to events for {@link Gui}s. Only one instance of this class gets constructed.
 * (One instance per plugin, but plugins are supposed to shade and relocate IF.)
 *
 * @since 0.5.4
 */
public class GuiListener implements Listener {

    /**
     * The owning plugin of this listener.
     */
    @NotNull
    private final Plugin plugin;

    private final com.github.stefvanschie.inventoryframework.gui.GuiListener ifListener;

    /**
     * Creates a new listener for all guis for the provided {@code plugin}.
     *
     * @param plugin the owning plugin of this listener
     * @since 0.10.8
     */
    public GuiListener(@NotNull Plugin plugin, com.github.stefvanschie.inventoryframework.gui.GuiListener ifListener) {
        this.ifListener = ifListener;
        this.plugin = plugin;
    }

    /**
     * Handles clicks in inventories
     *
     * @param event the event fired
     * @since 0.5.4
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        try {
            ifListener.onInventoryClick(event);
        } catch (UnsupportedOperationException e) {

            val who = event.getWhoClicked();

            who.getScheduler().run(this.plugin, t -> {
                PlayerInventory playerInventory = who.getInventory();

                /* due to a client issue off-hand items appear as ghost items, this updates the off-hand correctly
                   client-side */
                playerInventory.setItemInOffHand(playerInventory.getItemInOffHand());
            }, null);
        }
    }

    /**
     * Handles users picking up items while their bottom inventory is in use.
     *
     * @param event the event fired when an entity picks up an item
     * @since 0.6.1
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityPickupItem(@NotNull EntityPickupItemEvent event) {
        ifListener.onEntityPickupItem(event);
    }

    /**
     * Handles drag events
     *
     * @param event the event fired
     * @since 0.6.1
     */
    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        ifListener.onInventoryDrag(event);
    }

    /**
     * Handles the selection of trades in merchant guis
     *
     * @param event the event fired
     */
    @EventHandler(ignoreCancelled = true)
    public void onTradeSelect(@NotNull TradeSelectEvent event) {
        ifListener.onTradeSelect(event);
    }

    /**
     * Handles closing in inventories
     *
     * @param event the event fired
     * @since 0.5.4
     */
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
            humanEntity.getScheduler().run(plugin, t -> gui.navigateToParent(humanEntity), null);
        }
    }
//
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
//        ifListener.onPlayerDropItem(event);
//    }

    /**
     * Registers newly opened inventories
     *
     * @param event the event fired
     * @since 0.5.19
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        ifListener.onInventoryOpen(event);
    }

    /**
     * Handles the disabling of the plugin
     *
     * @param event the event fired
     * @since 0.5.19
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        ifListener.onPluginDisable(event);
    }

    /**
     * Gets the gui from the inventory or null if the inventory isn't a gui
     *
     * @param inventory the inventory to get the gui from
     * @return the gui or null if the inventory doesn't have a gui
     * @since 0.8.1
     */
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