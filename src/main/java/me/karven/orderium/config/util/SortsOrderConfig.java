package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.obj.SortTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SortsOrderConfig {
    private final String path;
    public final @NotNull ArrayList<@NotNull SortTypes> orderArray = new ArrayList<>();

    public SortsOrderConfig(final @NotNull String path) {
        this.path = path;
    }

    public void reload(final @NotNull ConfigFile config) {
        final List<String> sortOrderString = config.getStringList(path);
        reload(sortOrderString);
    }

    public void reload(final @NotNull List<@Nullable String> sortOrderString) {
        orderArray.clear();
        for (final String sortString : sortOrderString) {
            final SortTypes sortType = SortTypes.fromIdentifier(sortString);
            if (sortType == null) continue;
            orderArray.add(sortType);
        }
    }

    public void save(final @NotNull ConfigFile config) {
        config.set(path, stringSortsOrder());
    }

    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(path, stringSortsOrder());
    }

    public @NotNull SortTypes index(final int index) {
        return orderArray.get(index % orderArray.size());
    }

    private @NotNull List<String> stringSortsOrder() {
        return orderArray.stream().map(SortTypes::getIdentifier).toList();
    }

    public void migrateV5(final @NotNull ConfigFile config, final @NotNull String oldPath) {
        final List<String> sortOrderString = config.getStringList(oldPath);
        reload(sortOrderString);
    }
}
