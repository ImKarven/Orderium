package me.karven.orderium.folia;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.GuiListener;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import lombok.val;
import me.karven.orderium.load.Orderium;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

/// This class implements a hacky method to support Folia on the dependency inventory framework
public class IFFolia extends Gui {

    public IFFolia(Orderium plugin) {
        // extends Gui and super to set hasRegisteredListeners to true
        super(plugin);

        GuiListener ifListener = null;
        for (val listener : HandlerList.getRegisteredListeners(plugin)) {
            if (!(listener.getListener() instanceof GuiListener guiListener)) continue;
            ifListener = guiListener;
            break;
        }

        if (ifListener == null) throw new NullPointerException("Inventory Framework did not register any listeners");
        // Unregister IF's registered GuiListener and replace with our own
        HandlerList.unregisterAll(ifListener);
        Bukkit.getPluginManager().registerEvents(new me.karven.orderium.folia.GuiListener(plugin, ifListener), plugin);
    }

    public void show(@NotNull HumanEntity humanEntity) { throw new UnsupportedOperationException("Use of prohibited method"); }

    public @NonNull Gui copy() { throw new UnsupportedOperationException("Use of prohibited method"); }

    public void click(@NotNull InventoryClickEvent event) { throw new UnsupportedOperationException("Use of prohibited method"); }

    public boolean isPlayerInventoryUsed() { throw new UnsupportedOperationException("Use of prohibited method"); }

    public int getViewerCount() { throw new UnsupportedOperationException("Use of prohibited method"); }

    public @NotNull List<HumanEntity> getViewers() { throw new UnsupportedOperationException("Use of prohibited method"); }

    @Override
    public void update() { throw new UnsupportedOperationException("Use of prohibited method"); }

    @Override
    public @NotNull Iterable<? extends GuiItem> getItems() { throw new UnsupportedOperationException("Use of prohibited method"); }
}
