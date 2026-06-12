package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SignGUIConfig extends GUIConfigFile {
    // We load the BlockType lazily instead of storing directly because the config loads before some APIs are available
    public NamespacedKey signTypeKey;
    public int queryLine;
    public final @NotNull List<@NotNull String> signLines = new ArrayList<>();

    public SignGUIConfig() {
        super("search");
    }

    public void reload() {
        final String typeKey = config.getString("type");
        assert typeKey != null;
        signTypeKey = getKey(typeKey);
        queryLine = config.getInteger("query-line");
        signLines.clear();
        signLines.addAll(config.getStringList("lines"));
    }

    public @NotNull BlockType signType() {
        final BlockType signType = Registry.BLOCK.get(signTypeKey);
        if (signType == null || !isSign(signType)) return BlockType.OAK_SIGN;
        return signType;
    }

    @Override
    public void save() {
        config.set("type", signTypeKey.toString());
        config.set("query-line", queryLine);
        config.set("lines", signLines);
    }

    @Override
    public void setDefault() {
        config.addDefault("type", signTypeKey.toString());
        config.addDefault("query-line", queryLine);
        config.addDefault("lines", signLines);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        signTypeKey = getKey(oldConfig.getString("gui.search-sign.type"));
        queryLine = oldConfig.getInteger("gui.search-sign.search-line") - 1;
        signLines.clear();
        signLines.addAll(oldConfig.getStringList("gui.search-sign.lines"));

        save();

        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull NamespacedKey getKey(final @Nullable String keyString) {
        if (keyString == null) return new NamespacedKey("minecraft", "oak_sign");
        final String[] components = keyString.split(":");
        if (components.length != 2) return new NamespacedKey("minecraft", "oak_sign");
        return new NamespacedKey(components[0], components[1]);
    }

    private boolean isSign(final @NotNull BlockType type) {
        return type.createBlockData().createBlockState() instanceof Sign;
    }

    @Override
    public void applyDefaultValues() {
        signTypeKey = new NamespacedKey("minecraft", "oak_sign");
        queryLine = 0;
        signLines.add("");
        signLines.add("↑↑↑↑↑↑↑↑↑↑↑↑");
        signLines.add("Search");
        signLines.add("");
    }
}
