package me.karven.orderium.config.util.gui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.ButtonConfig;
import me.karven.orderium.config.util.EnchantmentConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnchantGUIConfig extends CommonGUIConfig {
    public final @NotNull EnchantmentConfig enchantmentConfig = new EnchantmentConfig();
    public final @NotNull ButtonConfig confirmButton = new ButtonConfig("buttons.confirm");

    public EnchantGUIConfig() {
        super("enchant");
    }

    @Override
    public void reload() {
        title = config.getString("title");
        rows = config.getInteger("rows");
        enchantmentConfig.reload(config);
        confirmButton.reload(config);
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        enchantmentConfig.save(config);
        confirmButton.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        enchantmentConfig.setDefault(config);
        confirmButton.setDefault(config);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.enchant-item.title");
        rows = -1;
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
        title = "Enchant Your Item";
        rows = -1;
        confirmButton.slot = 8;
        confirmButton.itemStack = ItemStack.of(Material.LIME_WOOL);
        confirmButton.itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Confirm").color(NamedTextColor.GREEN));
            meta.lore(List.of(Component.text("Click to confirm your enchantments").color(NamedTextColor.WHITE)));
        });
    }
}
