package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.Config;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public abstract class GUIConfigFile implements IConfigFile {
    protected final @NotNull ConfigFile config;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected GUIConfigFile(final @NotNull String guiName) {
        final File guiFolder = new File(Config.dataFolder, "gui");
        if (!guiFolder.exists() || !guiFolder.isDirectory()) {
            guiFolder.mkdirs();
        }
        try {
            config = new ConfigFile(new File(guiFolder, guiName + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void reload() throws IOException {
        config.getFile().createNewFile();
        config.loadContent();
    }
    public abstract void save();
    public abstract void setDefault() throws Exception;

    /**
     * This caches the values from the old config file to the objects
     * They then should be saved to respective files after the method call
     * @param oldConfig the old config file
     */
    public abstract void migrateV5(final @NotNull ConfigFile oldConfig);
    public abstract void applyDefaultValues();

    public void saveToFile() throws Exception {
        config.save();
    }
}
