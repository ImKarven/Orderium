package me.karven.orderium.config.util.component;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

public class SearchGUITypeConfig extends ComponentConfig {
    public @NotNull SearchGUIType searchGUIType = SearchGUIType.DIALOG;

    public SearchGUITypeConfig(final @NotNull String path) {
        super(path);
    }

    @Override
    public void reload(final @NotNull ConfigFile config) {
        final String stringType = config.getString(path);
        if (stringType == null) return;
        try {
            searchGUIType = SearchGUIType.valueOf(stringType.toUpperCase());
        } catch (final IllegalArgumentException _) {}
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        config.set(path, searchGUIType.toString());
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault(path, searchGUIType.toString());
    }

    public enum SearchGUIType {
        SIGN,
        DIALOG
    }
}
