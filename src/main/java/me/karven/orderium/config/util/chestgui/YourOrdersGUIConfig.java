package me.karven.orderium.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.GUIConfigFile;
import me.karven.orderium.config.util.component.ButtonConfig;
import me.karven.orderium.config.util.component.OrderConfig;
import me.karven.orderium.utils.DecoratedText;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

// TODO: Make this GUI paginated
// And scrollable: https://github.com/PaperMC/Paper/pull/13898
public class YourOrdersGUIConfig extends GUIConfigFile {
    public String title;
    public int rows;
    public final @NotNull OrderConfig orderConfig = new OrderConfig("order");
    public final @NotNull ButtonConfig newOrderButton = new ButtonConfig("buttons.new-order");
    
    public YourOrdersGUIConfig() {
        super("your-orders");
    }

    @Override
    public void reload() throws IOException {
        super.reload();
        title = config.getString("title");
        rows = config.getInteger("rows");
        newOrderButton.reload(config);
        orderConfig.reload(config);
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        newOrderButton.save(config);
        orderConfig.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        newOrderButton.setDefault(config);
        orderConfig.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.your-orders.title");
        rows = 3;
        orderConfig.migrateV5(oldConfig, "gui.your-orders.order");
        newOrderButton.migrateV5(oldConfig, "gui.your-orders.buttons.new-order", 0);
        newOrderButton.slot = 26;

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
        rows = 3;
        orderConfig.lore.add("");
        orderConfig.lore.add("<!i><#786500>$<paid><gray>/<#017800>$<total> <gray>Paid");
        orderConfig.lore.add("<!i><#786500><delivered><gray>/<#017800><amount> <gray>Delivered");
        orderConfig.lore.add("<!i><green>$<money-per> <white>each");
        orderConfig.lore.add("");
        orderConfig.lore.add("<!i><order-status>");
        orderConfig.slots.addAll(IntStream.range(0, 27).boxed().toList());
        orderConfig.itemRepresentation = ItemStack.of(Material.STONE);

        newOrderButton.slot = 26;
        newOrderButton.itemStack = ItemStack.of(Material.MAP);
        newOrderButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("New Order"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to create a new order")));
        });
    }
}
