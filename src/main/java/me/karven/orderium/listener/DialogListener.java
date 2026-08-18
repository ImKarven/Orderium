package me.karven.orderium.listener;

import me.karven.orderium.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DialogListener {
    // TODO: probably should replace `Player` with `UUID`.
    private static final ConcurrentHashMap<Player, Collection<ItemStack>> pendingItems = new ConcurrentHashMap<>();

    public static Map<Player, Collection<ItemStack>> pendingItems() {
        return pendingItems;
    }

    public static void addItems(Player p, Collection<ItemStack> items) {
        pendingItems.put(p, items);
    }

    public static void removeItems(Player p) { pendingItems.remove(p); }

    public static void onCancel(Player p) {
        Collection<ItemStack> items = pendingItems.get(p);
        if (items == null) return;
        PlayerUtils.give(p, items, false);
        pendingItems.remove(p);
    }
}
