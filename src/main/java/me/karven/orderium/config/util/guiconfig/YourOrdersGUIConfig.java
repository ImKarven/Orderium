package me.karven.orderium.config.util.guiconfig;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.ButtonConfig;
import me.karven.orderium.config.util.OrderConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

public class YourOrdersGUIConfig extends CommonGUIConfig {
    public final @NotNull OrderConfig orderConfig = new OrderConfig("order");
    public final @NotNull ButtonConfig newOrderButton = new ButtonConfig("buttons.new-order");
    
    public YourOrdersGUIConfig() {
        super("your-orders");
    }

    @Override
    public void reload() {
        orderConfig.reload(config);
        newOrderButton.reload(config);
        title = config.getString("title");
    }

    @Override
    public void save() {
        orderConfig.save(config);
        newOrderButton.save(config);
        config.set("title", title);
    }

    @Override
    public void setDefault() {
        orderConfig.setDefault(config);
        newOrderButton.setDefault(config);
        config.addDefault("title", title);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.your-orders.title");
        orderConfig.migrateV5(oldConfig, "gui.your-orders.order");
        newOrderButton.migrateV5(oldConfig, "gui.your-orders.buttons.new-order", 0);

        save();
        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyDefaultValues() {
        title = "Your Orders";
        orderConfig.lore.add("");
        orderConfig.lore.add("<#786500>$<paid><gray>/<#017800>$<total> <gray>Paid");
        orderConfig.lore.add("<#786500><delivered><gray>/<#017800><amount> <gray>Delivered");
        orderConfig.lore.add("<green>$<money-per> <white>each");
        orderConfig.lore.add("");
        orderConfig.lore.add("<order-status>");
        orderConfig.slots.addAll(IntStream.range(0, 27).boxed().toList());
        orderConfig.itemRepresentation = ItemStack.of(Material.STONE);

        newOrderButton.slot = -1;
        newOrderButton.itemStack = ItemStack.of(Material.MAP);
        newOrderButton.itemStack.editMeta(meta -> {
            meta.displayName(Component.text("New Order").color(NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Click to create a new order").color(NamedTextColor.WHITE)));
        });
    }
}
