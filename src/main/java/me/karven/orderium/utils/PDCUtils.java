package me.karven.orderium.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PDCUtils {
    private static final NamespacedKey collectedKey = new NamespacedKey("orderium", "collected"); // String namespace isn't perfect but it works
    private static final NamespacedKey blacklistKey = new NamespacedKey("orderium", "blacklist");
    private static final NamespacedKey searchKey = new NamespacedKey("orderium", "search");
    private static final NamespacedKey idKey =  new NamespacedKey("orderium", "item-id");

    public static final List<NamespacedKey> KEYS = List.of(collectedKey, blacklistKey, searchKey, idKey);

    public static void setID(ItemMeta meta, int id) {
        meta.getPersistentDataContainer().set(collectedKey, PersistentDataType.INTEGER, id);
    }

    public static int getID(ItemMeta meta) {
        return meta.getPersistentDataContainer().getOrDefault(collectedKey, PersistentDataType.INTEGER, -1);
    }

    public static void setCollected(Player p, int amount) {
        DispatchUtil.entity(p, () -> p.getPersistentDataContainer().set(collectedKey, PersistentDataType.INTEGER, amount));
    }

    public static ItemMeta removeOrderiumPD(ItemMeta meta) {
        for  (NamespacedKey key : KEYS) {
            meta.getPersistentDataContainer().remove(key);
        }
        return meta;
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
        DispatchUtil.entity(p, () -> future.complete(getCollected(p)));
        return future;
    }

    /**
     * Remove all persistent data registered by Orderium from this holder
     * @param holder the holder; can be player, item meta, etc
     */
    public static void clear(@NotNull PersistentDataHolder holder) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        for (NamespacedKey key : KEYS) {
            pdc.remove(key);
        }
    }
}
