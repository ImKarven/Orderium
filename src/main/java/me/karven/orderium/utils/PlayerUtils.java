package me.karven.orderium.utils;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.papermc.paper.dialog.Dialog;
import lombok.val;
import me.karven.orderium.data.ConfigCache;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static me.karven.orderium.load.Orderium.plugin;

public class PlayerUtils {
    private static ConfigCache cache;

    public static void init() {
        PlayerUtils.cache = plugin.getConfigs();
    }

    /**
     *
     * @param p Player to give item to
     * @param items the items
     * @param safe whether to schedule this with the player's scheduler or not
     */
    public static void give(Player p, Collection<ItemStack> items, boolean safe) {
        if (!safe) {
            p.give(items, true);
            return;
        }
        val location = p.getLocation();
        val world = location.getWorld();
        p.getScheduler().run(plugin, _ -> p.give(items, true), () ->
                Bukkit.getRegionScheduler().run(plugin, location, _ ->
                        items.forEach(item ->
                                world.dropItem(location, item)
                        )
                )
        );

    }

    /**
     * give player an item stack
     * @param p the player
     * @param item the item stack
     * @param safe whether to schedule this task in the correct thread or not
     */
    public static void give(Player p, ItemStack item, boolean safe) {
        PlayerUtils.give(p, Collections.singleton(item), safe);
    }

    /**
     * give player a specific amount of an item stack, overrides item stack's amount
     * @param p the player
     * @param item the item stack
     * @param amount the amount
     * @param safe whether to schedule this task in the correct thread
     */
    public static void give(Player p, ItemStack item, int amount, boolean safe) {
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
        PlayerUtils.give(p, items, safe);
    }

    public static void playSound(Player p, Sound s) {
        p.playSound(s);
    }

    public static void openGui(Player p, ChestGui gui) {
        DispatchUtil.entity(p, () -> {
            gui.show(p);
            DispatchUtil.entity(p, p::updateInventory);
        });
    }

    public static void openDialog(Player p, Dialog dialog) {
        DispatchUtil.entity(p, () -> p.showDialog(dialog));
    }

    public static void closeInv(Player p) {
        DispatchUtil.entity(p, () -> p.closeInventory());
    }

    public static void clickNext(InventoryClickEvent e, ChestGui nextPage) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        PlayerUtils.openGui(p, nextPage);
        PlayerUtils.playSound(p, cache.getNextPageSound());
    }

    public static void clickBack(InventoryClickEvent e, ChestGui previousPage) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        PlayerUtils.openGui(p, previousPage);
        PlayerUtils.playSound(p, cache.getPreviousPageSound());
    }
}
