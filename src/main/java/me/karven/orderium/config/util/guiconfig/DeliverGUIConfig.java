package me.karven.orderium.config.util.guiconfig;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

public class DeliverGUIConfig extends CommonGUIConfig {
    public DeliverGUIConfig() {
        super("deliver");
    }

    @Override
    public void reload() {
        title = config.getString("title");
        rows = config.getInteger("rows");
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.deliver.title");
        rows = oldConfig.getInteger("gui.deliver.rows");

        save();

        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyDefaultValues() {
        title = "Delivering...";
        rows = 6;
    }
}
