package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface IConfigFile {

    void reload() throws IOException;

    void save();

    void setDefault() throws Exception;

    void migrateV5(final @NotNull ConfigFile oldConfig);

    void applyDefaultValues();
}
