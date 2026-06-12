package me.karven.orderium.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.GUIConfigFile;
import me.karven.orderium.config.util.component.ButtonConfig;
import me.karven.orderium.config.util.component.EnchantmentConfig;
import me.karven.orderium.utils.DecoratedText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnchantGUIConfig extends GUIConfigFile {
    public boolean enabled;
    public String title;
    public int rows;
    public int displayItemSlot;
    public final @NotNull EnchantmentConfig enchantmentConfig = new EnchantmentConfig("enchantment-book");
    public final @NotNull ButtonConfig confirmButton = new ButtonConfig("buttons.confirm");

    public EnchantGUIConfig() {
        super("enchant");
    }

    @Override
    public void reload() {
        enabled = config.getBoolean("enabled");
        title = config.getString("title");
        rows = config.getInteger("rows");
        displayItemSlot = config.getInteger("display-item-slot");
        enchantmentConfig.reload(config);
        confirmButton.reload(config);
    }

    @Override
    public void save() {
        config.set("enabled", enabled);
        config.set("title", title);
        config.set("rows", rows);
        config.set("display-item-slot", displayItemSlot);
        enchantmentConfig.save(config);
        confirmButton.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("enabled", enabled);
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        config.addDefault("display-item-slot", displayItemSlot);
        enchantmentConfig.setDefault(config);
        confirmButton.setDefault(config);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        enabled = oldConfig.getBoolean("enchantments");
        title = oldConfig.getString("gui.enchant-item.title");
        rows = 4;
        displayItemSlot = 0;
        enchantmentConfig.migrateV5(oldConfig);
        confirmButton.migrateV5(oldConfig, "gui.enchant-item.confirm-button", 0);
        save();
        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyDefaultValues() {
        enabled = false;
        title = "Enchant Your Item";
        rows = 4;
        displayItemSlot = 0;
        confirmButton.slot = 8;
        confirmButton.itemStack = ItemStack.of(Material.LIME_WOOL);
        confirmButton.itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Confirm").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(DecoratedText.buttonLore("Click to confirm your enchantments")));
        });
    }
}
