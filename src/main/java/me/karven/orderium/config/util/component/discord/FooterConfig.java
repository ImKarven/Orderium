package me.karven.orderium.config.util.component.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.component.ComponentConfig;
import me.karven.orderium.utils.ConvertUtils;
import org.jetbrains.annotations.NotNull;

public class FooterConfig extends ComponentConfig {
    public String iconUrl = "https://wsrv.nl/?url=https://raw.githubusercontent.com/ImKarven/Orderium/refs/heads/master/assets/OrderiumIcon.png";
    public String text = "Orderium";

    public FooterConfig(final @NotNull String path) {
        super(path);
    }

    public JsonElement jsonObject(final String... replacements) {
        final JsonObject result = new JsonObject();
        result.addProperty("icon_url", ConvertUtils.replaceText(iconUrl, replacements));
        result.addProperty("text", ConvertUtils.replaceText(text, replacements));
        return result;
    }

    @Override
    public void reload(final @NotNull ConfigFile config) {
        iconUrl = config.getString(path + ".icon-url");
        text = config.getString(path + ".text");
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        config.set(path + ".icon-url", iconUrl);
        config.set(path + ".text", text);
    }

    @Override
    public void setDefault(final @NotNull ConfigFile config) {
        config.addDefault(path + ".icon-url", iconUrl);
        config.addDefault(path + ".text", text);
    }
}
