package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public abstract class GUIConfigFile {
    protected final @NotNull ConfigFile config;

    protected GUIConfigFile(final @NotNull String guiName) {
        try {
            config = ConfigFile.loadConfig(new File("plugins" + File.separator + "Orderium" + File.separator + "gui" + File.separator + guiName + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void reload();
    public abstract void save();
    public abstract void setDefault();

    /**
     * This caches the values from the old config file to the objects
     * They then should be saved to respective files after the method call
     * @param oldConfig the old config file
     */
    public abstract void migrateV5(final @NotNull ConfigFile oldConfig);
    public abstract void applyDefaultValues();
}
