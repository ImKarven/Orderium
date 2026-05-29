package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ItemlessItemDialogBodyConfig extends ComponentConfig {
    public final @NotNull MessageDialogBodyConfig description = new MessageDialogBodyConfig(path + ".description");
    public boolean showDecoration;
    public boolean showTooltip;
    public int width;
    public int height;

    public ItemlessItemDialogBodyConfig(final @NotNull String path) {
        super(path);
    }

    public @NotNull ItemDialogBody body(final @NotNull ItemStack item, final @NotNull TagResolver... tagResolvers) {
        return DialogBody.item(item, description.body(tagResolvers), showDecoration, showTooltip, width, height);
    }

    @Override
    public void reload(@NotNull ConfigFile config) {
        description.reload(config);
        showDecoration = config.getBoolean(path + ".show-decoration");
        showTooltip = config.getBoolean(path + ".show-tooltip");
        width = config.getInteger(path + ".width");
        height = config.getInteger(path + ".height");
    }

    @Override
    public void save(@NotNull ConfigFile config) {
        description.save(config);
        config.set(path + ".show-decoration", showDecoration);
        config.set(path + ".show-tooltip", showTooltip);
        config.set(path + ".width", width);
        config.set(path + ".height", height);
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        description.setDefault(config);
        config.addDefault(path + ".show-decoration", showDecoration);
        config.addDefault(path + ".show-tooltip", showTooltip);
        config.addDefault(path + ".width", width);
        config.addDefault(path + ".height", height);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig, @NotNull String path) {
        description.migrateV5(oldConfig, path);
        showDecoration = true;
        showTooltip = true;
        width = 16;
        height = 16;
    }
}
