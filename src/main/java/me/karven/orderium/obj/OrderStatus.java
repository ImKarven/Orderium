package me.karven.orderium.obj;

import org.jetbrains.annotations.NotNull;

public enum OrderStatus {
    EXPIRED("expired", "<red>Order Expired"),
    COMPLETED("completed", "<green>Order Completed"),
    AVAILABLE("available", "<gray>Expires after <day>d <hour>h <minute>m <second>s");

    private final String identifier;
    private String text;

    public @NotNull String getIdentifier() { return identifier; }
    public String getText() { return text; }
    public void setText(final @NotNull String text) { this.text = text; }

    OrderStatus(final String identifier, final String defaultText) {
        this.identifier = identifier;
        this.text = defaultText;
    }
}
