package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class TextDialogInputConfig extends ComponentConfig {
    // COMMON - START Common fields for all dialog input types (separate to a different common class if other types of inputs are used in the future)
    public String label;
    // COMMON - END

    public int width;
    public boolean labelVisible;
    public String initial;
    public int maxLength;
    // multiline not present because we don't use it

    public TextDialogInputConfig(final @NotNull String path) {
        super(path);
    }

    public @NotNull TextDialogInput input(final @NotNull String key) {
        return DialogInput.text(key, width, MiniMessage.miniMessage().deserialize(label), labelVisible, initial, maxLength, null);
    }

    @Override
    public void reload(final @NotNull ConfigFile config) {
        // COMMON - START
        label = config.getString(path + ".label");
        // COMMON - END

        width = config.getInteger(path + ".width");
        labelVisible = config.getBoolean(path + ".label_visible");
        initial = config.getString(path + ".initial");
        maxLength = config.getInteger(path + ".max_length");
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        // COMMON - START
        config.set(path + ".label", label);
        // COMMON - END

        config.set(path + ".width", width);
        config.set(path + ".label_visible", labelVisible);
        config.set(path + ".initial", initial);
        config.set(path + ".max_length", maxLength);
    }

    @Override
    public void setDefault(final @NotNull ConfigFile config) {
        // COMMON - START
        config.addDefault(path + ".label", label);
        // COMMON - END

        config.addDefault(path + ".width", width);
        config.addDefault(path + ".label_visible", labelVisible);
        config.addDefault(path + ".initial", initial);
        config.addDefault(path + ".max_length", maxLength);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig, final @NotNull String path) {
        label = oldConfig.getString(path);
        if (path.startsWith("gui.new-order")) {
            width = oldConfig.getInteger("gui.new-order.button-width");
        } else width = 200;
        labelVisible = true;
        initial = "1";
        maxLength = 32;
    }
}
