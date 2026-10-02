package me.karven.orderium.order;

import me.karven.orderium.obj.SortType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

final class OrderCache {
    private final NavigableSet<Order> mostMoneyPerItem = new ConcurrentSkipListSet<>(Comparator.comparingDouble(Order::getMoneyPer).reversed().thenComparingInt(Order::getId));
    private final NavigableSet<Order> recentlyListed = new ConcurrentSkipListSet<>(Comparator.comparingLong(Order::getExpiresAt).reversed().thenComparingInt(Order::getId));
    private final NavigableSet<Order> mostDelivered = new ConcurrentSkipListSet<>(Comparator.comparingInt(Order::getDelivered).reversed().thenComparingInt(Order::getId));
    private final NavigableSet<Order> mostPaid = new ConcurrentSkipListSet<>(Comparator.comparingDouble(Order::getPaid).reversed().thenComparingInt(Order::getId));

    private final List<NavigableSet<Order>> indexes = List.of(mostMoneyPerItem, recentlyListed, mostDelivered, mostPaid);

    void setAll(final Collection<? extends Order> orders) {
        for (final NavigableSet<Order> index : indexes) {
            index.clear();
            index.addAll(orders);
        }
    }

    void add(final Order order) {
        synchronized (order.getLock()) {
            for (final NavigableSet<Order> index : indexes) index.add(order);
        }
    }

    // Outdated states are ignored, so the cache never goes back in time when changes to the same order finish out of order
    void update(final Order order, final OrderState state) {
        synchronized (order.getLock()) {
            final OrderState current = order.getState();
            if (order.isRemoved() || state.version() < current.version() || state.equals(current)) return;

            // The indexes are sorted by the state, so the order has to leave them before its state changes
            for (final NavigableSet<Order> index : indexes) index.remove(order);
            order.setState(state);
            for (final NavigableSet<Order> index : indexes) index.add(order);
        }
    }

    void remove(final Order order) {
        synchronized (order.getLock()) {
            order.markRemoved();
            for (final NavigableSet<Order> index : indexes) index.remove(order);
        }
    }

    NavigableSet<Order> getSorted(final SortType sortType) {
        switch (sortType) {
            case MOST_MONEY_PER_ITEM -> { return mostMoneyPerItem; }
            case RECENTLY_LISTED -> { return recentlyListed; }
            case MOST_DELIVERED -> { return mostDelivered; }
            case MOST_PAID -> { return mostPaid; }
        }
        return mostMoneyPerItem;
    }
}
