package me.karven.orderium.config.util.component.discord;

import com.google.common.base.Preconditions;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.component.ComponentConfig;
import me.karven.orderium.utils.DiscordWebhook;
import org.jetbrains.annotations.NotNull;

public class WebhookOptionConfig extends ComponentConfig {
    public boolean enabled = false;
    public String webhook = "";
    public final EmbedConfig embedConfig = new EmbedConfig(path + ".embed");

    public WebhookOptionConfig(final @NotNull String path) {
        super(path);
    }

    public void send(final @NotNull String... replacements) {
        Preconditions.checkArgument(replacements.length % 2 == 0, "Placeholders length must be even. This is a bug.");
        DiscordWebhook.send(this, replacements);
    }

    public void send(final @NotNull String[] placeholders, final @NotNull String... placeholdersVararg) {
        final String[] result = new String[placeholders.length + placeholdersVararg.length];
        System.arraycopy(placeholders, 0, result, 0, placeholders.length);
        System.arraycopy(placeholdersVararg, 0, result, placeholders.length, placeholdersVararg.length);
        send(result);
    }

    @Override
    public void reload(@NotNull ConfigFile config) {
        enabled = config.getBoolean(path + ".enabled");
        webhook = config.getString(path + ".webhook");
        embedConfig.reload(config);
    }

    @Override
    public void save(@NotNull ConfigFile config) {
        config.set(path + ".enabled", enabled);
        config.set(path + ".webhook", webhook);
        embedConfig.save(config);
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault(path + ".enabled", enabled);
        config.addDefault(path + ".webhook", webhook);
        embedConfig.setDefault(config);
    }
}
