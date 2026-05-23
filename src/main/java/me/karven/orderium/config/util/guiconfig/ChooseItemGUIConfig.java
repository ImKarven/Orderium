package me.karven.orderium.config.util.guiconfig;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.ButtonConfig;
import me.karven.orderium.config.util.SortButtonConfig;
import me.karven.orderium.config.util.SortsOrder;
import me.karven.orderium.obj.SortTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChooseItemGUIConfig extends CommonGUIConfig {
    public final @NotNull SortsOrder sortsOrder = new SortsOrder("sorts-order");
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
        sortsOrder.reload(config);
        sortButton.reload(config);
        nextButton.reload(config);
        backButton.reload(config);
        searchButton.reload(config);
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        sortsOrder.save(config);
        sortButton.save(config);
        nextButton.save(config);
        backButton.save(config);
        searchButton.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        sortsOrder.setDefault(config);
        sortButton.setDefault(config);
        nextButton.setDefault(config);
        backButton.setDefault(config);
        searchButton.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.choose-item.title");
        rows = 6;
        sortsOrder.migrateV5(oldConfig, "gui.choose-item.sorts-order");
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
        sortsOrder.orderArray.add(SortTypes.A_Z);
        sortsOrder.orderArray.add(SortTypes.Z_A);
        sortButton.slot = 48;
        sortButton.lore.add("");
        sortButton.lore.add("<white> • <a-z>");
        sortButton.lore.add("<white> • <z-a>");
        sortButton.itemStack = ItemStack.of(Material.HOPPER);
        sortButton.itemStack.editMeta(meta -> meta.displayName(Component.text("Sort").color(NamedTextColor.AQUA)));
        nextButton.slot = 53;
        nextButton.itemStack = ItemStack.of(Material.ARROW);
        nextButton.itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Next").color(NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Click to go to the next page").color(NamedTextColor.WHITE)));
        });
        backButton.slot = 45;
        backButton.itemStack = ItemStack.of(Material.ARROW);
        backButton.itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Back").color(NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Click to go to previous page").color(NamedTextColor.WHITE)));
        });
        searchButton.slot = 50;
        searchButton.itemStack = ItemStack.of(Material.OAK_SIGN);
        searchButton.itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Search").color(NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Click to search").color(NamedTextColor.WHITE)));
        });
    }
}
