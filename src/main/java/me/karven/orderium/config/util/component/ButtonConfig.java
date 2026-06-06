package me.karven.orderium.config.util.component;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ButtonConfig extends ComponentConfig {
    public ItemStack itemStack;
    public int slot;

    public ButtonConfig(final @NotNull String path) {
        super(path);
    }

    @Override
    public void reload(final @NotNull ConfigFile config) {
        try {
            itemStack = ConvertUtils.deserializeItem(config.getConfigSection(path + ".item"));
        } catch (Exception e) {
            itemStack = ItemStack.of(Material.STONE);
        }
        slot = config.getInteger(path + ".slot");
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        config.set(path + ".item", itemStack.serialize());
        config.set(path + ".slot", slot);
    }

    @Override
    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(path + ".item", itemStack.serialize());
        config.addDefault(path + ".slot", slot);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile config, final @NotNull String oldPath) {
        migrateV5(config, oldPath, 45);
    }

    public @NotNull InventoryItem item(final @NotNull Consumer<InventoryClickEvent> action) {
        return new InventoryItem(itemStack.clone(), action);
    }

    // Migrate config version 4 -> 5
    public void migrateV5(final @NotNull ConfigFile config, final @NotNull String oldPath, final int slotOffset) {
        final ItemStack item = ConvertUtils.getItemType(config.getString(oldPath + ".type"))
                .createItemStack();
        final List<String> loreLines = config.getStringList(oldPath + ".lore");
        final String displayNameString = config.getString(oldPath + ".display-name");
        final int slot = config.getInteger(oldPath + ".slot");
        final String itemModel = config.getString(oldPath + ".item-model");
        item.editMeta(meta -> {
            final MiniMessage mm = MiniMessage.miniMessage();
            if (displayNameString != null) meta.displayName(mm.deserialize(displayNameString));
            final List<Component> lore = loreLines.stream().map(mm::deserialize).toList();
            meta.lore(lore);

            if (itemModel == null) return;
            final String[] components = itemModel.split(":");
            if (components.length != 2) return;
            meta.setItemModel(new NamespacedKey(components[0], components[1]));
        });

        this.itemStack = item;
        this.slot = slot + slotOffset;
    }
}