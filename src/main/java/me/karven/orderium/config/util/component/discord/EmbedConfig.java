package me.karven.orderium.config.util.component.discord;

import com.google.gson.JsonObject;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.component.ComponentConfig;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.Log;
import org.jetbrains.annotations.NotNull;

public class EmbedConfig extends ComponentConfig {
    public String title;
    public String description;
    public String color;
    public final FooterConfig footerConfig = new FooterConfig(path + ".footer");

    public EmbedConfig(final @NotNull String path) {
        super(path);
    }

    public JsonObject jsonObject(final String... replacements) {
        final JsonObject result = new JsonObject();
        result.addProperty("title", ConvertUtils.replaceText(title, replacements));
        result.addProperty("description", ConvertUtils.replaceText(description, replacements));
        try {
            result.addProperty("color", Integer.parseInt(color, 16));
        } catch (NumberFormatException e) {
            Log.error("Invalid hex color " + color, e);
        }
        result.add("footer", footerConfig.jsonObject(replacements));
        return result;
    }

    @Override
    public void reload(final @NotNull ConfigFile config) {
        title = config.getString(path + ".title");
        description = config.getString(path + ".description");
        color = config.getString(path + ".color");
        footerConfig.reload(config);
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        config.set(path + ".title", title);
        config.set(path + ".description", description);
        config.set(path + ".color", color);
        footerConfig.save(config);
    }

    @Override
    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(path + ".title", title);
        config.addDefault(path + ".description", description);
        config.addDefault(path + ".color", color);
        footerConfig.setDefault(config);
    }
}
