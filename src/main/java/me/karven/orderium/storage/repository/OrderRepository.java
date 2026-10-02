package me.karven.orderium.storage.repository;

import me.karven.orderium.order.Order;
import me.karven.orderium.order.OrderDraft;
import me.karven.orderium.order.OrderState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface OrderRepository {

    List<Order> findAll();

    @Nullable OrderState findState(int id);

    <O extends Order> O insert(OrderDraft<O> draft);

    // Only applies if the stored version is still expected.version(). Returns the stored state with its new version, or null if the order changed or is gone
    @Nullable OrderState update(int id, OrderState expected, OrderState updated);

    // Only applies if the stored version is still expected.version()
    boolean delete(int id, OrderState expected);
}
