package me.karven.orderium.data;

import lombok.Getter;
import lombok.val;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.Pair;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.Log;
import me.karven.orderium.utils.PDCUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class DataCache {

    private static final Registry<BlockType> BLOCK_REGISTRY = Registry.BLOCK;
    private final TreeSet<ItemStack> itemsAZ = new TreeSet<>(AlgoUtils.getComparator(SortTypes.A_Z));
    private final TreeSet<ItemStack> itemsZA = new TreeSet<>(AlgoUtils.getComparator(SortTypes.Z_A));

    @Getter
    private final List<Pair<byte[], String>> customItems = new ArrayList<>();
    @Getter
    private final HashSet<ItemStack> blacklist = new HashSet<>();

    private final TreeSet<Order> mostMoneyPerItem = new TreeSet<>(Comparator.comparingDouble(Order::getMoneyPer).reversed().thenComparing(Order::getId));
    private final TreeSet<Order> recentlyListed = new TreeSet<>(Comparator.comparingLong(Order::getExpiresAt).reversed().thenComparing(Order::getId));
    private final TreeSet<Order> mostDelivered = new TreeSet<>(Comparator.comparingInt(Order::getDelivered).reversed().thenComparing(Order::getId));
    private final TreeSet<Order> mostPaid = new TreeSet<>(Comparator.comparingDouble(Order::getPaid).reversed().thenComparing(Order::getId));

    private void setBlacklistAndCustomItems(Collection<ItemStack> blacklist, Collection<Pair<byte[], String>> customItems) {
        this.blacklist.clear();
        this.customItems.clear();
        this.blacklist.addAll(blacklist);
        this.customItems.addAll(customItems);
    }

    public void setItems(Collection<ItemStack> bukkitItems, Collection<ItemStack> blacklistedItems, Collection<Pair<byte[], String>> customItems) {
        itemsAZ.clear();
        itemsZA.clear();
        itemsAZ.addAll(bukkitItems);
        itemsZA.addAll(bukkitItems);

        final List<ItemStack> parsedCustomItems = customItems.stream().map(item -> {
            final ItemStack stack = ItemStack.deserializeBytes(item.first());
            stack.editMeta(meta -> PDCUtils.setSearch(meta, item.second));
            return stack;
        }).toList();

        itemsAZ.addAll(parsedCustomItems);
        itemsZA.addAll(parsedCustomItems);

        for (ItemStack e : blacklistedItems) {
            itemsAZ.remove(e);
            itemsZA.remove(e);
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
        order.moneyPer = moneyPer;
        order.amount = amount;
        order.delivered = delivered;
        order.inStorage = inStorage;

        // Re-add the order to not mess up the sorted collections after updating
        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);

        mostMoneyPerItem.add(order);
        recentlyListed.add(order);
        mostDelivered.add(order);
        mostPaid.add(order);
    }

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
        val toDel = new ArrayList<Order>();
        val ownerOrders = mostMoneyPerItem.stream().filter(order -> {
            if (!order.getOwnerUniqueId().equals(ownerId)) return false;
            if (order.shouldBeDeleted()) {
                toDel.add(order);
                return false;
            }
            return true;
        }).toList();
        toDel.forEach(this::deleteOrder);
        return ownerOrders;
    }

    public TreeSet<Order> getSortedOrders(SortTypes sortType) {
        switch (sortType) {
            case MOST_MONEY_PER_ITEM -> { return mostMoneyPerItem; }
            case RECENTLY_LISTED -> { return recentlyListed; }
            case MOST_DELIVERED -> { return mostDelivered; }
            case MOST_PAID -> { return mostPaid; }
        }
        return mostMoneyPerItem;
    }

    public TreeSet<ItemStack> getItems(SortTypes sortType) {
        switch (sortType) {
            case A_Z -> { return itemsAZ; }
            case Z_A -> { return itemsZA; }
        }
        return itemsAZ;
    }

    public BlockType getBlockType(@KeyPattern String identifier) {
        return BLOCK_REGISTRY.get(Key.key(identifier));
    }
}
