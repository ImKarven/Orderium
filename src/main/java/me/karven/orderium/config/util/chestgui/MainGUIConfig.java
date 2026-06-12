package me.karven.orderium.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.GUIConfigFile;
import me.karven.orderium.config.util.component.ButtonConfig;
import me.karven.orderium.config.util.component.OrderConfig;
import me.karven.orderium.config.util.component.SortButtonConfig;
import me.karven.orderium.config.util.component.SortsOrderConfig;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.DecoratedText;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

// TODO: Add scrollable version in the future
// https://github.com/PaperMC/Paper/pull/13898
public class MainGUIConfig extends GUIConfigFile {
    public String title;
    public int rows;
    public final @NotNull OrderConfig orderConfig = new OrderConfig("order");
    public final @NotNull SortsOrderConfig sortsOrderConfig = new SortsOrderConfig("sorts-order");
    public final @NotNull SortButtonConfig sortButton = new SortButtonConfig("buttons.sort");
    public final @NotNull ButtonConfig refreshButton = new ButtonConfig("buttons.refresh");
    public final @NotNull ButtonConfig yourOrdersButton = new ButtonConfig("buttons.your-orders");
    public final @NotNull ButtonConfig searchButton = new ButtonConfig("buttons.search");
    public final @NotNull ButtonConfig backButton = new ButtonConfig("buttons.back");
    public final @NotNull ButtonConfig nextButton = new ButtonConfig("buttons.next");

    public MainGUIConfig() {
        super("main");
    }

    @Override
    public void reload() {
        orderConfig.reload(config);
        sortsOrderConfig.reload(config);
        sortButton.reload(config);
        refreshButton.reload(config);
        yourOrdersButton.reload(config);
        searchButton.reload(config);
        backButton.reload(config);
        nextButton.reload(config);
        title = config.getString("title");
        rows = config.getInteger("rows");
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        orderConfig.save(config);
        sortsOrderConfig.save(config);
        sortButton.save(config);
        refreshButton.save(config);
        yourOrdersButton.save(config);
        searchButton.save(config);
        backButton.save(config);
        nextButton.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        orderConfig.setDefault(config);
        sortsOrderConfig.setDefault(config);
        sortButton.setDefault(config);
        refreshButton.setDefault(config);
        yourOrdersButton.setDefault(config);
        searchButton.setDefault(config);
        backButton.setDefault(config);
        nextButton.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.main.title");
        orderConfig.migrateV5(oldConfig, "gui.main.order");
        sortsOrderConfig.migrateV5(oldConfig, "gui.main.sorts-order");
        sortButton.migrateV5(oldConfig, "gui.main.buttons.sort");
        refreshButton.migrateV5(oldConfig, "gui.main.buttons.refresh");
        yourOrdersButton.migrateV5(oldConfig, "gui.main.buttons.your-orders");
        searchButton.migrateV5(oldConfig, "gui.main.buttons.search");
        backButton.migrateV5(oldConfig, "gui.main.buttons.back");
        nextButton.migrateV5(oldConfig, "gui.main.buttons.next");
        rows = 6;
        save();
        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyDefaultValues() {
        title = "Orders";
        rows = 6;
        orderConfig.lore.add("");
        orderConfig.lore.add("<!i><#786500>$<paid><gray>/<#017800>$<total> <gray>Paid");
        orderConfig.lore.add("<!i><#786500><delivered><gray>/<#017800><amount> <gray>Delivered");
        orderConfig.lore.add("<!i><green>$<money-per> <white>each");
        orderConfig.lore.add("");
        orderConfig.lore.add("<!i><white>Click to deliver <aqua><player><white>'s order");
        orderConfig.slots.addAll(IntStream.range(0, 45).boxed().toList());

        sortsOrderConfig.orderArray.add(SortType.MOST_MONEY_PER_ITEM);
        sortsOrderConfig.orderArray.add(SortType.RECENTLY_LISTED);
        sortsOrderConfig.orderArray.add(SortType.MOST_DELIVERED);
        sortsOrderConfig.orderArray.add(SortType.MOST_PAID);

        backButton.slot = 45;
        backButton.itemStack = ItemStack.of(Material.ARROW);
        backButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Back"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to go to the previous page")));
        });

        nextButton.slot = 53;
        nextButton.itemStack = ItemStack.of(Material.ARROW);
        nextButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Next"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to go to the next page")));
        });

        refreshButton.slot = 49;
        refreshButton.itemStack = ItemStack.of(Material.PAPER);
        refreshButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Refresh"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to refresh")));
        });

        searchButton.slot = 50;
        searchButton.itemStack = ItemStack.of(Material.OAK_SIGN);
        searchButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Search"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to search")));
        });

        sortButton.slot = 48;
        sortButton.itemStack = ItemStack.of(Material.HOPPER);
        sortButton.itemStack.editMeta(meta -> meta.displayName(DecoratedText.buttonName("Sort")));
        sortButton.lore.add("");
        sortButton.lore.add("<!i><white> • <most-money-per-item>");
        sortButton.lore.add("<!i><white> • <recently-listed>");
        sortButton.lore.add("<!i><white> • <most-delivered>");
        sortButton.lore.add("<!i><white> • <most-paid>");

        yourOrdersButton.slot = 51;
        yourOrdersButton.itemStack = ItemStack.of(Material.CHEST);
        yourOrdersButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Your Orders"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to view your orders")));
        });
    }
}
