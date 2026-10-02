package me.karven.orderium.order;


import java.util.UUID;
import java.util.function.IntFunction;

public record OrderDraft<O extends Order>(
        OrderType<O> type,
        UUID owner,
        byte[] data,
        OrderState state,
        IntFunction<O> factory
) {

    public O toOrder(int id) {
        return factory.apply(id);
    }
}
