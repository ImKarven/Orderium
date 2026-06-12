package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static me.karven.orderium.Orderium.plugin;

public abstract class GUIConfigFile implements IConfigFile {
    protected @NotNull ConfigFile config;
    private final @NotNull File configFile;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected GUIConfigFile(final @NotNull String guiName) {
        final File guiFolder = new File(plugin.getDataFolder(), "gui");

        if (!guiFolder.exists() || !guiFolder.isDirectory()) {
            guiFolder.mkdirs();
        }

        this.configFile = new File(guiFolder, guiName + ".yml");
        try {
            config = ConfigFile.loadConfig(configFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadFromFile() {
        try {
            config = ConfigFile.loadConfig(configFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        reload();
    }

    public abstract void reload();
//            throws IOException {
//        try {
//            config = ConfigFile.loadConfig(configFile);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
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
