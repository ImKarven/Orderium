package me.karven.orderium.listener;

import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.guiframework.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;

/// This class helps things happen normally when the player disconnects
@SuppressWarnings("UnstableApiUsage")
public class DisconnectListener implements Listener {

    // Properly give items back to the player in case they quit while still in the delivery GUI
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        SignGUI.completeSession(p, ""); // Close the sign GUI manually if the player is in one

        if (DialogListener.pendingItems().containsKey(p))
            DialogListener.onCancel(p); // Close the dialog manually if the player is in one

        InventoryView view = p.getOpenInventory();
        if (view.getTopInventory().getHolder() instanceof InventoryGUI gui)
            gui.callOnClose(new InventoryCloseEvent(view)); // Close the inventory GUI manually if the player is in one

    }
}
