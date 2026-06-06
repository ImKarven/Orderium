package me.karven.orderium.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.GUIConfigFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DeliverGUIConfig extends GUIConfigFile {
    public String title;
    public int rows;
    public DeliverGUIConfig() {
        super("deliver");
    }

    @Override
    public void reload() throws IOException {
        super.reload();
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
        title = oldConfig.getString("gui.delivery.title");
        rows = oldConfig.getInteger("gui.delivery.rows");

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
