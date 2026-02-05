package me.karven.orderium.obj;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum SortTypes {
    MOST_MONEY_PER_ITEM("most-money-per-item"),
    RECENTLY_LISTED("recently-listed"),
    MOST_DELIVERED("most-delivered"),
    MOST_PAID("most-paid"),
    DEFAULT("default"),
    A_Z("a-z"),
    Z_A("z-a");

    private final String identifier;
    @Setter
    private String display;

    SortTypes(String identifier) {
        this.identifier = identifier;
    }

    public static SortTypes fromIdentifier(String identifier) {
        List<SortTypes> sortType = Arrays.stream(SortTypes.values()).filter(s -> s.is(identifier)).toList();
        if (sortType.isEmpty()) return null;
        return sortType.getFirst();
    }

    private boolean is(String identifier) {
        return this.identifier.equals(identifier);
    }
}
