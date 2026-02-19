package me.karven.orderium.utils;

import me.karven.orderium.load.Orderium;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PDCUtils {
    private static Orderium plugin;

    private static final NamespacedKey collectedKey = new NamespacedKey("orderium", "collected"); // String namespace isn't perfect but it works
    private static final NamespacedKey blacklistKey = new NamespacedKey("orderium", "blacklist");
    private static final NamespacedKey searchKey = new NamespacedKey("orderium", "search");

    public static void init(Orderium plugin) {
        PDCUtils.plugin = plugin;
    }

    public static void setCollected(Player p, int amount) {
        p.getScheduler().run(plugin, t -> p.getPersistentDataContainer().set(collectedKey, PersistentDataType.INTEGER, amount), null);
    }

    public static void removeCollected(Player p) {
        p.getPersistentDataContainer().remove(collectedKey);
    }

    public static int getCollected(Player p) {
        return p.getPersistentDataContainer().getOrDefault(collectedKey, PersistentDataType.INTEGER, 0);
    }

    public static void setBlacklist(ItemMeta meta) {
        final byte b = 1; // Weird thing I can't just put 1 in the method
        meta.getPersistentDataContainer().set(blacklistKey, PersistentDataType.BYTE, b);
    }

    public static boolean isBlacklist(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(blacklistKey);
    }

    public static void setSearch(ItemMeta meta, String search) {
        meta.getPersistentDataContainer().set(searchKey, PersistentDataType.STRING, search);
    }

    public static String getSearch(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(searchKey, PersistentDataType.STRING);
    }

    public static boolean hasCustomSearch(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(searchKey);
    }

    public static CompletableFuture<Integer> getCollectedSafe(Player p) {
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        p.getScheduler().run(plugin, t -> future.complete(getCollected(p)), null);

        return future;
    }
}
