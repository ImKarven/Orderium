package me.karven.orderium.config;

import com.google.common.io.Files;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.SignGUIConfig;
import me.karven.orderium.config.util.gui.*;

import java.io.File;
import java.io.IOException;

public class Config {
    private static final String CONFIG_PATH = "plugins" + File.separator + "Orderium" + File.separator + "config.yml";
    private static final String GUI_PATH = "plugins" + File.separator + "Orderium" + File.separator + "gui";
    private static final String MAIN_GUI_PATH = GUI_PATH + File.separator + "main.yml";
    private static final String YOUR_ORDERS_GUI_PATH = GUI_PATH + File.separator + "your-orders.yml";

    public final ConfigFile configFile;
    public final ConfigFile mainGUIFile;
    public final ConfigFile yourOrdersGUIFile;

    public final MainGUIConfig mainGUIConfig = new MainGUIConfig();
    public final YourOrdersGUIConfig yourOrdersGUIConfig = new YourOrdersGUIConfig();
    public final ChooseItemGUIConfig chooseItemGUIConfig = new ChooseItemGUIConfig();
    public final SignGUIConfig signGUIConfig = new SignGUIConfig();
    public final EnchantGUIConfig enchantGUIConfig = new EnchantGUIConfig();
    public final DeliverGUIConfig deliverGUIConfig = new DeliverGUIConfig();

    public Config() {
        try {
            configFile = ConfigFile.loadConfig(new File(CONFIG_PATH));
            mainGUIFile = ConfigFile.loadConfig(new File(MAIN_GUI_PATH));
            yourOrdersGUIFile = ConfigFile.loadConfig(new File(YOUR_ORDERS_GUI_PATH));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final int configVersion = configFile.getInteger("config-version", -1);

        // Migrating old config file
        if (configFile.isNew() || configVersion >= 5) {
            setDefaults();
            return;
        }

        final File backupConfig = new File(CONFIG_PATH + ".old");
        try {
            Files.copy(new File(CONFIG_PATH), backupConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Add missing values to config
        ConfigMigration.migrateV4(configFile);

        // Convert GUI config sections to their respective files
        mainGUIConfig.migrateV5(configFile);
        yourOrdersGUIConfig.migrateV5(configFile);
        chooseItemGUIConfig.migrateV5(configFile);
        signGUIConfig.migrateV5(configFile);
        enchantGUIConfig.migrateV5(configFile);
        deliverGUIConfig.migrateV5(configFile);

        // TODO: Missing some dialogs...

        // Remove the gui section entirely after migration
        configFile.set("gui", null);
    }

    private void setDefaults() {
        mainGUIConfig.applyDefaultValues();
        yourOrdersGUIConfig.applyDefaultValues();
        chooseItemGUIConfig.applyDefaultValues();
        signGUIConfig.applyDefaultValues();
        enchantGUIConfig.applyDefaultValues();
        deliverGUIConfig.applyDefaultValues();
    }
}
