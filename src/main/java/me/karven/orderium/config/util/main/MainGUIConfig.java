package me.karven.orderium.config.util.main;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.ButtonConfig;
import me.karven.orderium.config.util.OrderConfig;
import org.jetbrains.annotations.NotNull;

public class MainGUIConfig {
    public String title;
    public final @NotNull OrderConfig orderConfig = new OrderConfig();
    public final @NotNull ButtonConfig refreshButton = new ButtonConfig("gui.main.buttons.refresh");
    public final @NotNull ButtonConfig yourOrdersButton = new ButtonConfig("gui.main.buttons.your-orders");
    public final @NotNull ButtonConfig searchButton = new ButtonConfig("gui.main.buttons.search");
    public final @NotNull ButtonConfig backButton = new ButtonConfig("gui.main.buttons.back");
    public final @NotNull ButtonConfig nextButton = new ButtonConfig("gui.main.buttons.next");

    public void reload(final @NotNull ConfigFile config) {
        orderConfig.reload(config);
        refreshButton.reload(config);
        yourOrdersButton.reload(config);
        searchButton.reload(config);
        backButton.reload(config);
        nextButton.reload(config);
        title = config.getString("gui.main.title");
    }

    public void save(final @NotNull ConfigFile config) {
        orderConfig.save(config);
        refreshButton.save(config);
        yourOrdersButton.save(config);
        searchButton.save(config);
        backButton.save(config);
        nextButton.save(config);
        config.set("gui.main.title", title);
    }

    public void setDefault(final @NotNull ConfigFile config) {
        orderConfig.setDefault(config);
        refreshButton.setDefault(config);
        yourOrdersButton.setDefault(config);
        searchButton.setDefault(config);
        backButton.setDefault(config);
        nextButton.setDefault(config);
        config.addDefault("gui.main.title", title);
    }
}
