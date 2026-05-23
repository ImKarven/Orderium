package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// Sort buttons have placeholders in lore, we need a different way to handle them.
public class SortButtonConfig extends ButtonConfig {
    public final @NotNull List<@NotNull String> lore = new ArrayList<>();

    public SortButtonConfig(@NotNull String path) {
        super(path);
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        super.save(config);

        config.set(path + ".lore", lore);
    }

    @Override
    public void setDefault(final @NotNull ConfigFile config) {
        super.setDefault(config);

        config.set(path + ".lore", lore);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile config, final @NotNull String oldPath) {
        final ItemStack item = ConvertUtils.getItemType(config.getString(oldPath + ".type"))
                .createItemStack();
        final List<String> loreLines = config.getStringList(oldPath + ".lore");
        final String displayNameString = config.getString(oldPath + ".display-name");
        final int slot = config.getInteger(oldPath + ".slot");
        item.editMeta(meta -> {
            if (displayNameString != null) meta.displayName(MiniMessage.miniMessage().deserialize(displayNameString));
        });

        this.itemStack = item;
        this.slot = slot;
        this.lore.clear();
        this.lore.addAll(loreLines);
    }
}
