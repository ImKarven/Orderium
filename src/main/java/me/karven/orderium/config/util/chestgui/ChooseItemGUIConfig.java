package me.karven.orderium.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.GUIConfigFile;
import me.karven.orderium.config.util.component.ButtonConfig;
import me.karven.orderium.config.util.component.SortButtonConfig;
import me.karven.orderium.config.util.component.SortsOrderConfig;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.DecoratedText;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

// TODO: Add scrollable version in the future
// https://github.com/PaperMC/Paper/pull/13898
public class ChooseItemGUIConfig extends GUIConfigFile {
    public String title;
    public int rows;
    public final @NotNull List<Integer> slots = new ArrayList<>();
    public final @NotNull SortsOrderConfig sortsOrderConfig = new SortsOrderConfig("sorts-order");
    public final @NotNull SortButtonConfig sortButton = new SortButtonConfig("buttons.sort");
    public final @NotNull ButtonConfig nextButton = new ButtonConfig("buttons.next");
    public final @NotNull ButtonConfig backButton = new ButtonConfig("buttons.back");
    public final @NotNull ButtonConfig searchButton = new ButtonConfig("buttons.search");

    public ChooseItemGUIConfig() {
        super("choose-item");
    }

    @Override
    public void reload() {
        title = config.getString("title");
        rows = config.getInteger("rows");
        slots.clear();
        config.getList("slots").forEach(slot -> slots.add((Integer) slot));
        sortsOrderConfig.reload(config);
        sortButton.reload(config);
        nextButton.reload(config);
        backButton.reload(config);
        searchButton.reload(config);
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        config.set("slots", slots);
        sortsOrderConfig.save(config);
        sortButton.save(config);
        nextButton.save(config);
        backButton.save(config);
        searchButton.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        config.addDefault("slots", slots);
        sortsOrderConfig.setDefault(config);
        sortButton.setDefault(config);
        nextButton.setDefault(config);
        backButton.setDefault(config);
        searchButton.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.choose-item.title");
        rows = 6;
        slots.addAll(IntStream.range(0, 45).boxed().toList());
        sortsOrderConfig.migrateV5(oldConfig, "gui.choose-item.sorts-order");
        sortButton.migrateV5(oldConfig, "gui.choose-item.buttons.sort");
        nextButton.migrateV5(oldConfig, "gui.choose-item.buttons.next");
        backButton.migrateV5(oldConfig, "gui.choose-item.buttons.back");
        searchButton.migrateV5(oldConfig, "gui.choose-item.buttons.search");
        save();
        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyDefaultValues() {
        title = "Choose Your Item";
        rows = 6;
        slots.addAll(IntStream.range(0, 45).boxed().toList());
        sortsOrderConfig.orderArray.add(SortType.A_Z);
        sortsOrderConfig.orderArray.add(SortType.Z_A);
        sortButton.slot = 48;
        sortButton.lore.add("");
        sortButton.lore.add("<!i><white> • <a-z>");
        sortButton.lore.add("<!i><white> • <z-a>");
        sortButton.itemStack = ItemStack.of(Material.HOPPER);
        sortButton.itemStack.editMeta(meta -> meta.displayName(DecoratedText.buttonName("Sort")));
        nextButton.slot = 53;
        nextButton.itemStack = ItemStack.of(Material.ARROW);
        nextButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Next"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to go to the next page")));
        });
        backButton.slot = 45;
        backButton.itemStack = ItemStack.of(Material.ARROW);
        backButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Back"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to go to the previous page")));
        });
        searchButton.slot = 50;
        searchButton.itemStack = ItemStack.of(Material.OAK_SIGN);
        searchButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Search"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to search")));
        });
    }
}
