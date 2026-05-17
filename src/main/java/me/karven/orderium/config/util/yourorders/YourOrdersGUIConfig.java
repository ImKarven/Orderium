package me.karven.orderium.config.util.yourorders;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.ButtonConfig;
import me.karven.orderium.config.util.OrderConfig;
import org.jetbrains.annotations.NotNull;

public class YourOrdersGUIConfig {
    public String title;
    public final @NotNull OrderConfig orderConfig = new OrderConfig();
    public final @NotNull ButtonConfig newOrderButton = new ButtonConfig("gui.your-orders.buttons.new-order");

    public void reload(final @NotNull ConfigFile config) {
        orderConfig.reload(config);
        newOrderButton.reload(config);
        title = config.getString("gui.your-orders.title");
    }

    public void save(final @NotNull ConfigFile config) {
        orderConfig.save(config);
        newOrderButton.save(config);
        config.set("gui.your-orders.title", title);
    }

    public void setDefault(final @NotNull ConfigFile config) {
        orderConfig.setDefault(config);
        newOrderButton.setDefault(config);
        config.addDefault("gui.your-orders.title", title);
    }
}
