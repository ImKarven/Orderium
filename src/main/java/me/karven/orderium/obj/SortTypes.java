package me.karven.orderium.obj;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SortTypes {
    MOST_MONEY_PER_ITEM("most-money-per-item"),
    RECENTLY_LISTED("recently-listed"),
    MOST_DELIVERED("most-delivered"),
    MOST_PAID("most-paid"),
    A_Z("a-z"),
    Z_A("z-a");

    private final String identifier;
    private String display;

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(final @NotNull String display) {
        this.display = display;
    }

    SortTypes(String identifier) {
        this.identifier = identifier;
    }

    public static @Nullable SortTypes fromIdentifier(String identifier) {
        for (SortTypes sortType : SortTypes.values()) {
            if (sortType.identifier.equals(identifier)) return sortType;
        }
        return null;
    }

    private boolean is(String identifier) {
        return this.identifier.equals(identifier);
    }
}
