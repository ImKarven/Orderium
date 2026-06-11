package me.karven.orderium.data;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.utils.Log;
import org.jetbrains.annotations.NotNull;

public class ConfigMigration {

    public static void migrateV4(final @NotNull ConfigFile config) {
        config.set("config-version", 4);
        try {
            config.save();
        } catch (Exception e) {
            Log.error("Failed to migrate config version 4", e);
        }
    }
}
