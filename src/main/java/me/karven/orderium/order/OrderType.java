package me.karven.orderium.order;


import java.util.UUID;

public interface OrderType<O extends Order> {

    // Stored in the database, must never change once released
    String key();

    O decode(int id, UUID owner, byte[] data, OrderState state);
}
