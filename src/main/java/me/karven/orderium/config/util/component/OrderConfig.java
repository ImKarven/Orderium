package me.karven.orderium.config.util.component;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.utils.ConvertUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OrderConfig extends ComponentConfig {
    public final @NotNull List<@NotNull Integer> slots = new ArrayList<>();
    // This is a item with type 'minecraft:stone'. It is used to represent the amount and components in the order item.
    public ItemStack itemRepresentation = ItemStack.of(Material.STONE);
    public final @NotNull List<@NotNull String> lore = new ArrayList<>();

    public OrderConfig(@NotNull String path) {
        super(path);
    }

    public void reload(final @NotNull ConfigFile config) {
        slots.clear();
        lore.clear();
        final List<Integer> slotsList = config.getList(path + ".slots");
        slots.addAll(slotsList);

        final List<String> loreLines = config.getStringList(path + ".lore");
        lore.addAll(loreLines);
        try {
            itemRepresentation = ConvertUtils.deserializeItem(config.getConfigSection(path + ".item"));
        } catch (Exception e) {
            itemRepresentation = ItemStack.of(Material.STONE);
        }
    }

    public void save(final @NotNull ConfigFile config) {
        config.set(path + ".slots", slots);
        config.set(path + ".lore", lore);
        config.set(path + ".item", itemRepresentation.serialize());
    }

    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(path + ".slots", slots);
        config.addDefault(path + ".lore", lore);
        config.addDefault(path + ".item", itemRepresentation.serialize());
    }

    public void migrateV5(final @NotNull ConfigFile config, final @NotNull String oldPath) {
        // Old: "gui.[main/your-orders].order-lore"
        // New: "gui.[main/your-orders].order.lore"
        // oldPath should be "gui.[main/your-orders].order"
        // Hence a pretty unusual suffix, but it works
        final String OLD_LORE_PATH = oldPath + "-lore";
        final List<String> oldLore = config.getStringList(OLD_LORE_PATH);
        lore.clear();
        slots.clear();
        lore.addAll(oldLore);
        for (int i = 0; i < 45; i++) {
            slots.add(i);
        }
        itemRepresentation = ItemStack.of(Material.STONE);
    }
}
