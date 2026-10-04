package me.karven.orderium.order;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderTypes {
    private static final Map<String, OrderType<?>> TYPES = index(
            ItemOrder.TYPE
    );

    private OrderTypes() {}

    public static @Nullable OrderType<?> get(String key) {
        return TYPES.get(key);
    }

    private static Map<String, OrderType<?>> index(final OrderType<?>... types) {
        final Map<String, OrderType<?>> byKey = new LinkedHashMap<>();
        for (final OrderType<?> type : types) {
            if (byKey.putIfAbsent(type.key(), type) != null) {
                throw new IllegalStateException("Duplicate order type key: " + type.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }
}
