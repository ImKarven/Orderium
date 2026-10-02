package me.karven.orderium.order;

import me.karven.orderium.api.events.OrderRemoveEvent;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.storage.StorageException;
import me.karven.orderium.storage.repository.OrderRepository;
import me.karven.orderium.utils.Log;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class OrderService {

    private final Storage storage;
    private final OrderRepository repository;
    private final OrderCache cache = new OrderCache();

    public OrderService(final Storage storage) {
        this.storage = storage;
        this.repository = storage.orders();
    }

    public void loadAll() {
        final List<Order> orders = repository.findAll();
        cache.setAll(orders);
        Log.info("Loaded " + orders.size() + " orders.");
    }

    public NavigableSet<Order> getSortedOrders(final SortType sortType) {
        return cache.getSorted(sortType);
    }

    public List<Order> getOrders(final UUID owner) {
        final List<Order> ownerOrders = new ArrayList<>();
        for (final Order order : cache.getSorted(SortType.RECENTLY_LISTED)) {
            if (order.shouldBeDeleted()) {
                removeIfFinished(order);
                continue;
            }
            if (order.getOwnerUniqueId().equals(owner)) ownerOrders.add(order);
        }
        return ownerOrders;
    }

    public <O extends Order> CompletableFuture<O> create(final OrderDraft<O> draft) {
        return storage.supplyAsync("create an order", () -> {
            final O order = repository.insert(draft);
            cache.add(order);
            return order;
        });
    }

    public CompletableFuture<Delivery> deliver(final Order order, final int units) {
        if (units <= 0) return CompletableFuture.completedFuture(Delivery.NONE);

        return change(order, "deliver to", Delivery.NONE, state -> {
            if (!state.isActive(System.currentTimeMillis())) return new Keep<>(Delivery.NONE);

            final int accepted = Math.min(units, state.remaining());
            final OrderState delivered = state
                    .withDelivered(state.delivered() + accepted)
                    .withInStorage(state.inStorage() + accepted);
            return new Update<>(delivered, new Delivery(accepted, accepted * state.moneyPer()));
        });
    }

    public CompletableFuture<Boolean> collect(final Order order, final int amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return change(order, "collect from", false, state -> {
            if (state.inStorage() < amount) return new Keep<>(false);

            final Update<Boolean> collected = new Update<>(state.withInStorage(state.inStorage() - amount), true);
            return collected.state().isRemovable(System.currentTimeMillis()) ? new Remove<>(true, collected) : collected;
        });
    }

    public CompletableFuture<OptionalDouble> cancel(final Order order) {
        return change(order, "cancel", OptionalDouble.empty(), state -> {
            final long now = System.currentTimeMillis();
            if (!state.isActive(now)) return new Keep<>(OptionalDouble.empty());

            final OptionalDouble refund = OptionalDouble.of(state.remaining() * state.moneyPer());
            final Update<OptionalDouble> expired = new Update<>(state.withExpiresAt(now - 1), refund);
            return state.inStorage() == 0 ? new Remove<>(refund, expired) : expired;
        });
    }

    public CompletableFuture<Boolean> edit(final Order order, final Order.Field field, final Number value) {
        return change(order, "edit", false, state -> {
            final OrderState edited = switch (field) {
                case DELIVERED -> state.withDelivered(value.intValue());
                case IN_STORAGE -> state.withInStorage(value.intValue());
                case AMOUNT -> state.withAmount(value.intValue());
                case MONEY_PER -> state.withMoneyPer(value.doubleValue());
            };
            final Update<Boolean> update = new Update<>(edited, true);
            return edited.isRemovable(System.currentTimeMillis()) ? new Remove<>(false, update) : update;
        });
    }

    public CompletableFuture<Boolean> removeIfFinished(final Order order) {
        return change(order, "remove", true, state ->
                state.isRemovable(System.currentTimeMillis()) ? new Remove<>(true, new Keep<>(false)) : new Keep<>(false)
        );
    }

    private <T> CompletableFuture<T> change(
            final Order order,
            final String action,
            final T ifMissing,
            final Function<OrderState, Change<T>> decide
    ) {
        return storage.supplyAsync(action + " order #" + order.getId(), () -> applyChange(order, ifMissing, decide));
    }

    private <T> T applyChange(final Order order, final T ifMissing, final Function<OrderState, Change<T>> decide) {
        final int id = order.getId();
        for (int attempt = 1; attempt <= 5; attempt++) { // 5 stands for the max attempts here
            final OrderState current = repository.findState(id);
            if (current == null) {
                cache.remove(order);
                return ifMissing;
            }

            Change<T> change = decide.apply(current);
            if (change instanceof Remove<T> remove) {
                if (new OrderRemoveEvent.Pre(order, !Bukkit.isPrimaryThread()).callEvent()) {
                    if (!repository.delete(id, current)) continue;

                    cache.remove(order);
                    new OrderRemoveEvent.Post(order, !Bukkit.isPrimaryThread()).callEvent();
                    return remove.result();
                }
                // A listener wants to keep the order, so apply the change without removing it
                change = remove.otherwise();
            }

            if (change instanceof Update<T> update) {
                final OrderState stored = repository.update(id, current, update.state());
                if (stored == null) continue;

                cache.update(order, stored);
                return update.result();
            }

            // Nothing to write, but the cache may be behind storage
            cache.update(order, current);
            return ((Keep<T>) change).result();
        }
        throw new StorageException("Order #" + id + " kept being changed concurrently, gave up after 5 attempts");
    }

    public record Delivery(int units, double payout) {
        public static final Delivery NONE = new Delivery(0, 0);
    }

    private sealed interface Change<T> {}

    private sealed interface Fallback<T> extends Change<T> {}

    private record Keep<T>(T result) implements Fallback<T> {}

    private record Update<T>(OrderState state, T result) implements Fallback<T> {}

    /// Remove the order, or apply `otherwise` if an [OrderRemoveEvent.Pre] listener cancels the removal
    private record Remove<T>(T result, Fallback<T> otherwise) implements Change<T> {}
}
