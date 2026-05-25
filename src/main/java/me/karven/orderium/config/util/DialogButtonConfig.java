package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

public class DialogButtonConfig extends ComponentConfig {
    public String label;
    public String tooltip;
    public int width;

    public DialogButtonConfig(@NotNull String path) {
        super(path);
    }

    @Override
    public void reload(@NotNull ConfigFile config) {
        label = config.getString(path + ".label");
        tooltip = config.getString(path + ".tooltip");
        width = config.getInteger(path + ".width");
    }

    @Override
    public void save(@NotNull ConfigFile config) {
        config.set(path + ".label", label);
        config.set(path + ".tooltip", tooltip);
        config.set(path + ".width", width);
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault(path + ".label", label);
        config.addDefault(path + ".tooltip", tooltip);
        config.addDefault(path + ".width", width);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig, @NotNull String path) {
        label = oldConfig.getString(path + "-button");
        tooltip = oldConfig.getString(path + "-tooltip");
        if (path.startsWith("gui.new-order."))
            width = oldConfig.getInteger("gui.new-order.button-width");
        else
            width = 150;
    }
}
