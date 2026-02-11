package me.karven.orderium.obj;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum OrderStatus {
    EXPIRED("expired"),
    COMPLETED("completed"),
    AVAILABLE("available");

    private final String identifier;
    @Setter
    private String text;

     OrderStatus(String identifier) {
        this.identifier = identifier;
    }
}
