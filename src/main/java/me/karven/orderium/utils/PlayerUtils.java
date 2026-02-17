package me.karven.orderium.utils;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.load.Orderium;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerUtils {
    private static Orderium plugin;
    public static void init(Orderium plugin) {
        PlayerUtils.plugin = plugin;
    }

    public static void give(Player p, Collection<ItemStack> items, boolean dropIfFull) {
        p.getScheduler().run(plugin, t -> p.give(items, dropIfFull), null);
    }

    public static void give(Player p, ItemStack item, int amount) {
        final int maxStackSize = item.getMaxStackSize();

        final ItemStack copy = item.clone();
        copy.setAmount(maxStackSize);
        final int fullStackAmount = amount / maxStackSize;
        final List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < fullStackAmount; i++) {
            items.add(copy.clone());
        }
        final int rem = amount % maxStackSize;

        if (rem > 0) {
            copy.setAmount(rem);
            items.add(copy);
        }
        PlayerUtils.give(p, items, true);
    }

    public static void openGui(Player p, ChestGui gui) {
        p.getScheduler().run(plugin, t -> gui.show(p), null);
    }

    public static void openDialog(Player p, Dialog dialog) {
        p.getScheduler().run(plugin, t -> p.showDialog(dialog), null);
    }

    public static void closeInv(Player p) {
        p.getScheduler().run(plugin, t -> p.closeInventory(), null);
    }
}
