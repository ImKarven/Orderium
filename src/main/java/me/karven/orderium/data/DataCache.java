package me.karven.orderium.data;

import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.VanillaItem;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.Log;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static me.karven.orderium.Orderium.plugin;

public final class DataCache {
    private static final DataCache INSTANCE = new DataCache();

    public static @NotNull DataCache getInstance() {
        return INSTANCE;
    }

    private static final Registry<BlockType> BLOCK_REGISTRY = Registry.BLOCK;
    private final NavigableSet<OrderItem> itemsAZ = new ConcurrentSkipListSet<>(AlgoUtils.getComparator(SortType.A_Z));
    private final NavigableSet<OrderItem> itemsZA = new ConcurrentSkipListSet<>(AlgoUtils.getComparator(SortType.Z_A));

    private final Set<CustomItem> customItems = ConcurrentHashMap.newKeySet();
    private final Set<BlacklistedItem> blacklist = ConcurrentHashMap.newKeySet();

    private final NavigableSet<Order> mostMoneyPerItem = new ConcurrentSkipListSet<>(Comparator.comparingDouble(Order::getMoneyPer).reversed().thenComparing(Order::getId));
    private final NavigableSet<Order> recentlyListed = new ConcurrentSkipListSet<>(Comparator.comparingLong(Order::getExpiresAt).reversed().thenComparing(Order::getId));
    private final NavigableSet<Order> mostDelivered = new ConcurrentSkipListSet<>(Comparator.comparingInt(Order::getDelivered).reversed().thenComparing(Order::getId));
    private final NavigableSet<Order> mostPaid = new ConcurrentSkipListSet<>(Comparator.comparingDouble(Order::getPaid).reversed().thenComparing(Order::getId));

    private void setBlacklistAndCustomItems(Collection<BlacklistedItem> blacklist, Collection<CustomItem> customItems) {
        this.blacklist.clear();
        this.customItems.clear();
        this.blacklist.addAll(blacklist);
        this.customItems.addAll(customItems);
    }

    public void setItems(Collection<VanillaItem> vanillaItems, Collection<BlacklistedItem> blacklistedItems, Collection<CustomItem> customItems) {
        itemsAZ.clear();
        itemsZA.clear();
        itemsAZ.addAll(vanillaItems);
        itemsZA.addAll(vanillaItems);

        itemsAZ.addAll(customItems);
        itemsZA.addAll(customItems);

        for (BlacklistedItem e : blacklistedItems) {
            itemsAZ.removeIf(orderItem -> orderItem.getItemStack().equals(e.getItemStack()));
            itemsZA.removeIf(orderItem -> orderItem.getItemStack().equals(e.getItemStack()));
        }

        Log.info("Loaded " + itemsAZ.size() + " items.");
        setBlacklistAndCustomItems(blacklistedItems, customItems);
    }

    public void setOrders(Collection<Order> orders) {
        mostMoneyPerItem.clear();
        recentlyListed.clear();
        mostDelivered.clear();
        mostPaid.clear();
        mostMoneyPerItem.addAll(orders);
        recentlyListed.addAll(orders);
        mostDelivered.addAll(orders);
        mostPaid.addAll(orders);
    }
    public void updateOrder(Order order, double moneyPer, int amount, int delivered, int inStorage) {
        synchronized (order.getLock()) {
            mostMoneyPerItem.remove(order);
            recentlyListed.remove(order);
            mostDelivered.remove(order);
            mostPaid.remove(order);

            order.moneyPer = moneyPer;
            order.amount = amount;
            order.delivered = delivered;
            order.inStorage = inStorage;

            // Re-add the order to not mess up the sorted collections after updating
            mostMoneyPerItem.add(order);
            recentlyListed.add(order);
            mostDelivered.add(order);
            mostPaid.add(order);
        }
    }

    // TODO: Move event to storage update
    public void deleteOrder(Order order) {
        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
    }

    public void addOrder(Order order) {
        mostMoneyPerItem.add(order);
        recentlyListed.add(order);
        mostDelivered.add(order);
        mostPaid.add(order);
    }

    public List<Order> getOrders(UUID ownerId) {
        final List<Order> toDelete = new ArrayList<>();
        final List<Order> ownerOrders = recentlyListed.stream().filter(order -> {
            if (order.shouldBeDeleted()) {
                toDelete.add(order);
                return false;
            }

            return order.getOwnerUniqueId().equals(ownerId);
        }).toList();

        for (final Order order : toDelete) {
            plugin.getStorage().deleteOrder(order);
        }

        return ownerOrders;
    }

    public NavigableSet<Order> getSortedOrders(SortType sortType) {
        switch (sortType) {
            case MOST_MONEY_PER_ITEM -> { return mostMoneyPerItem; }
            case RECENTLY_LISTED -> { return recentlyListed; }
            case MOST_DELIVERED -> { return mostDelivered; }
            case MOST_PAID -> { return mostPaid; }
        }
        return mostMoneyPerItem;
    }

    public NavigableSet<OrderItem> getItems(SortType sortType) {
        switch (sortType) {
            case A_Z -> { return itemsAZ; }
            case Z_A -> { return itemsZA; }
        }
        return itemsAZ;
    }

    public Set<CustomItem> getCustomItems() { return customItems; }
    public Set<BlacklistedItem> getBlacklist() { return blacklist; }

    public BlockType getBlockType(@KeyPattern String identifier) {
        return BLOCK_REGISTRY.get(Key.key(identifier));
    }
}
