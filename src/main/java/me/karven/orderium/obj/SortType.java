package me.karven.orderium.obj;

import net.kyori.adventure.text.minimessage.tag.TagPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SortType {
    MOST_MONEY_PER_ITEM("most-money-per-item", "<aqua>Most Money Per Item", "Most Money Per Item"),
    RECENTLY_LISTED("recently-listed", "<aqua>Recently Listed", "Recently Listed"),
    MOST_DELIVERED("most-delivered", "<aqua>Most Delivered", "Most Delivered"),
    MOST_PAID("most-paid", "<aqua>Most Paid", "Most Paid"),
    A_Z("a-z", "<aqua>A - Z", "A - Z"),
    Z_A("z-a", "<aqua>Z - A", "Z - A"),
    ;

    private final @TagPattern String identifier;
    private String displayActive;
    private String displayInactive;

    public @NotNull @TagPattern String getIdentifier() {
        return identifier;
    }

    public String getDisplayActive() {
        return displayActive;
    }

    public String getDisplayInactive() {
        return displayInactive;
    }

    public void setDisplayActive(final @NotNull String displayActive) {
        this.displayActive = displayActive;
    }

    public void setDisplayInactive(final @NotNull String displayInactive) {
        this.displayInactive = displayInactive;
    }

    SortType(final @NotNull @TagPattern String identifier, final @NotNull String defaultDisplayActive, final @NotNull String defaultDisplayInactive) {
        this.identifier = identifier;
        this.displayActive = defaultDisplayActive;
        this.displayInactive = defaultDisplayInactive;
    }

    public static @Nullable SortType fromIdentifier(String identifier) {
        for (SortType sortType : SortType.values()) {
            if (sortType.identifier.equals(identifier)) return sortType;
        }
        return null;
    }

    private boolean is(String identifier) {
        return this.identifier.equals(identifier);
    }
}
