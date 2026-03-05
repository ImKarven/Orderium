package me.karven.orderium.listener;

import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
/// This class helps things happen normally when the player disconnects
public class DisconnectListener implements Listener {

    // Properly give items back to the player in case they quit while still in the delivery GUI
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        val p = e.getPlayer();
        val view = p.getOpenInventory();
        Gui gui = getGui(view.getTopInventory());
        if (gui != null)
        // Bukkit decides to not fire InventoryCloseEvent when quitting so we do that ourselves
            gui.callOnClose(new InventoryCloseEvent(view));

        if (!DialogListener.pendingItems().containsKey(p)) return;
        DialogListener.onCancel(p);
    }


    private Gui getGui(@NotNull Inventory inventory) {
        Gui gui = Gui.getGui(inventory);

        if (gui != null) return gui;

        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Gui gui2) return gui2;

        return null;
    }
}
