package me.karven.orderium.config.util.guiconfig;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public abstract class CommonGUIConfig {
    protected final @NotNull ConfigFile config;
    public String title;
    public int rows;

    protected CommonGUIConfig(final @NotNull String guiName) {
        try {
            config = ConfigFile.loadConfig(new File("plugins" + File.separator + "Orderium" + File.separator + "gui" + File.separator + guiName + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    abstract void reload();
    abstract void save();
    abstract void setDefault();

    /**
     * This caches the values from the old config file to the objects
     * They then should be saved to respective files after the method call
     * @param oldConfig the old config file
     */
    abstract void migrateV5(final @NotNull ConfigFile oldConfig);
    abstract void applyDefaultValues();
}
