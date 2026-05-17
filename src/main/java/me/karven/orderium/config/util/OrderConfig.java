package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OrderConfig {
    public final @NotNull List<@NotNull Integer> slots = new ArrayList<>();
    public int amount = 1;
    public final @NotNull List<@NotNull String> lore = new ArrayList<>();

    private static final String SLOTS_PATH = "gui.main.order.slots";
    private static final String AMOUNT_PATH = "gui.main.order.amount";
    private static final String LORE_PATH = "gui.main.order.lore";

    public void reload(final @NotNull ConfigFile config) {
        slots.clear();
        lore.clear();
        final List<Integer> slotsList = config.getList(SLOTS_PATH);
        slots.addAll(slotsList);
        amount = config.getInteger(AMOUNT_PATH);

        final List<String> loreLines = config.getStringList(LORE_PATH);
        lore.addAll(loreLines);
    }

    public void save(final @NotNull ConfigFile config) {
        config.set(SLOTS_PATH, slots);
        config.set(AMOUNT_PATH, amount);
        config.set(LORE_PATH, lore);
    }

    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(SLOTS_PATH, slots);
        config.addDefault(AMOUNT_PATH, amount);
        config.addDefault(LORE_PATH, lore);
    }
}
