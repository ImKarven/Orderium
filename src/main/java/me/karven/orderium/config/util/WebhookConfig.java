package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.component.discord.WebhookOptionConfig;
import me.karven.orderium.utils.Colors;

import java.io.File;
import java.io.IOException;

import static me.karven.orderium.Orderium.plugin;

public class WebhookConfig implements IConfigFile {
    private final ConfigFile config;

    public final WebhookOptionConfig createOrderOption = new WebhookOptionConfig("create-order");
    public final WebhookOptionConfig collectItemsOption = new WebhookOptionConfig("collect-items");
    public final WebhookOptionConfig cancelOrderOption = new WebhookOptionConfig("cancel-order");
    public final WebhookOptionConfig deliverOrderOption = new WebhookOptionConfig("deliver-order");
    public final WebhookOptionConfig orderCompleteOption = new WebhookOptionConfig("order-complete");

    public WebhookConfig() {
        try {
            config = ConfigFile.loadConfig(new File(plugin.getDataFolder(), "webhook.yml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reload() throws IOException {
        createOrderOption.reload(config);
        collectItemsOption.reload(config);
        cancelOrderOption.reload(config);
        deliverOrderOption.reload(config);
        orderCompleteOption.reload(config);
    }

    @Override
    public void save() {
        createOrderOption.save(config);
        collectItemsOption.save(config);
        cancelOrderOption.save(config);
        deliverOrderOption.save(config);
        orderCompleteOption.save(config);
    }

    @Override
    public void setDefault() {
        createOrderOption.setDefault(config);
        collectItemsOption.setDefault(config);
        cancelOrderOption.setDefault(config);
        deliverOrderOption.setDefault(config);
        orderCompleteOption.setDefault(config);
    }

    @Override
    public void applyDefaultValues() {
        createOrderOption.embedConfig.title = "New Order Created";
        createOrderOption.embedConfig.color = Colors.GREEN;
        createOrderOption.embedConfig.description = """
                Player **<player>** created an order of **<item>**.
                Quantity: <amount>
                Money: $<money-per> each
                """;

        collectItemsOption.embedConfig.title = "Items Collected";
        collectItemsOption.embedConfig.color = Colors.AQUA;
        collectItemsOption.embedConfig.description = """
                Player **<player>** collected **<collect-amount>** items from their <item> order.
                """;

        cancelOrderOption.embedConfig.title = "Order Cancelled";
        cancelOrderOption.embedConfig.color = Colors.RED;
        cancelOrderOption.embedConfig.description = """
                Player **<player>** cancelled an order of **<item>** and earned back $<earn>.
                """;

        deliverOrderOption.embedConfig.title = "Order Delivered";
        deliverOrderOption.embedConfig.color = Colors.AQUA;
        deliverOrderOption.embedConfig.description = """
                Player **<deliverer>** delivered items to **<player>**'s order.
                """;

        orderCompleteOption.embedConfig.title = "Order Completed";
        orderCompleteOption.embedConfig.color = Colors.GREEN;
        orderCompleteOption.embedConfig.description = """
                Player **<player>**'s **<item>** order is completed.
                """;
    }

    public void saveToFile() throws Exception {
        config.save();
    }
}
