package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

public abstract class ComponentConfig {
    protected final @NotNull String path;


    protected ComponentConfig(final @NotNull String path) {
        this.path = path;
    }

    abstract void reload(final @NotNull ConfigFile config);
    abstract void save(final @NotNull ConfigFile config);
    abstract void setDefault(final @NotNull ConfigFile config);
    abstract void migrateV5(final @NotNull ConfigFile oldConfig, final @NotNull String path);
}
