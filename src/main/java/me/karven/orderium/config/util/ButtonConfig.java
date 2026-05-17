package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static me.karven.orderium.load.Orderium.plugin;

public class ButtonConfig {
    private final @NotNull String path;
    public ItemStack itemStack;
    public int slot;

    public ButtonConfig(final @NotNull String path) {
        this.path = path;
    }

    public void reload(final @NotNull ConfigFile config) {
        itemStack = ItemStack.deserialize(config.getConfigSection(path));
        slot = config.getInteger(path + ".slot");
    }

    public void save(final @NotNull ConfigFile config) {
        config.set(path, itemStack.serialize());
        config.set(path + ".slot", slot);
    }

    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(path, itemStack.serialize());
        config.addDefault(path + ".slot", slot);
    }

    // Migrate config version 3 -> 4
    public void migrateV4(final @NotNull ConfigFile config) {
        final ItemStack item = ConvertUtils.getItemType(config.getString(path + ".type"))
                .createItemStack();
        final List<String> loreLines = config.getStringList(path + ".lore");
        final String displayNameString = config.getString(path + ".display-name");
        final int slot = config.getInteger(path + ".slot");
        item.editMeta(meta -> {
            if (displayNameString != null) meta.displayName(plugin.mm.deserialize(displayNameString));
            final List<Component> lore = loreLines.stream().map(plugin.mm::deserialize).toList();
            meta.lore(lore);
        });
        config.set(path, item.serialize());
        config.set(path + ".slot", slot);
    }
}
