package me.karven.orderium.config.util.component;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

public abstract class ComponentConfig {
    protected final @NotNull String path;


    protected ComponentConfig(final @NotNull String path) {
        this.path = path;
    }

    public abstract void reload(final @NotNull ConfigFile config);

    public abstract void save(final @NotNull ConfigFile config);

    public abstract void setDefault(final @NotNull ConfigFile config);

    public void migrateV5(final @NotNull ConfigFile oldConfig, final @NotNull String path) {}
}
