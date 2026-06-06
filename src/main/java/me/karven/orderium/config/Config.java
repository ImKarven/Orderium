package me.karven.orderium.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.SignGUIConfig;
import me.karven.orderium.config.util.chestgui.*;
import me.karven.orderium.config.util.dialog.ConfirmDeliveryDialogConfig;
import me.karven.orderium.config.util.dialog.ManageOrderDialogConfig;
import me.karven.orderium.config.util.dialog.NewOrderDialogConfig;
import me.karven.orderium.obj.OrderStatus;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.Log;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Config {
    public static Config config;
    public static final int CURRENT_CONFIG_VERSION = 5;
    public static final File dataFolder = new File("plugins" + File.separator + "Orderium");

    public ConfigFile configFile;

    public final MainGUIConfig mainGUIConfig = new MainGUIConfig();
    public final YourOrdersGUIConfig yourOrdersGUIConfig = new YourOrdersGUIConfig();
    public final ChooseItemGUIConfig chooseItemGUIConfig = new ChooseItemGUIConfig();
    public final SignGUIConfig signGUIConfig = new SignGUIConfig();
    public final EnchantGUIConfig enchantGUIConfig = new EnchantGUIConfig();
    public final DeliverGUIConfig deliverGUIConfig = new DeliverGUIConfig();
    public final NewOrderDialogConfig newOrderDialogConfig = new NewOrderDialogConfig();
    public final ConfirmDeliveryDialogConfig confirmDeliveryDialogConfig = new ConfirmDeliveryDialogConfig();
    public final ManageOrderDialogConfig manageOrderDialogConfig = new ManageOrderDialogConfig();

    public boolean bStats;
    public boolean checkForUpdates;

    public String invalidInput;
    public String orderCreationSuccessful;
    public String deliver;
    public String receiveDelivery;
    public String notEnoughMoney;
    public String deliverSelf;
    public String collectingTooFast;
    public String exceedMaxCollect;

    public Sound nextPageSound;
    public Sound previousPageSound;
    public Sound refreshSound;
    public Sound sortSound;
    public Sound newOrderSound;
    public Sound deliverSound;

    public boolean logTransactions;
    public long expiresAfter;
    public double minPrice;
    public int maxCollectPerMinute;
    public int maxCollect;
    public boolean shulkerDelivering;

    public final List<@NotNull String> orderCommandAliases = new ArrayList<>();

    public final List<NamespacedKey> similarityCheck = new ArrayList<>();

    public Config() throws Exception {
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            dataFolder.mkdirs();
        }
        try {
            configFile = new ConfigFile(new File(dataFolder, "config.yml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setDefaults();
        if (!configFile.getFile().exists()) {
            // save default config to file if it's newly created
            save();
            reload();
            // reload the config normally if no migration has been done
        }
//        else if (!ConfigMigration.perform(this)) {
//            reload();
//        }

        else reload();
    }

    public void save() throws Exception {
        configFile.set("config-version", 5);
        configFile.save();
        mainGUIConfig.saveToFile();
        yourOrdersGUIConfig.saveToFile();
        chooseItemGUIConfig.saveToFile();
        signGUIConfig.saveToFile();
        enchantGUIConfig.saveToFile();
        deliverGUIConfig.saveToFile();
        newOrderDialogConfig.saveToFile();
        confirmDeliveryDialogConfig.saveToFile();
        manageOrderDialogConfig.saveToFile();
    }

    public void setDefaults() {
        mainGUIConfig.applyDefaultValues();
        yourOrdersGUIConfig.applyDefaultValues();
        chooseItemGUIConfig.applyDefaultValues();
        signGUIConfig.applyDefaultValues();
        enchantGUIConfig.applyDefaultValues();
        deliverGUIConfig.applyDefaultValues();
        newOrderDialogConfig.applyDefaultValues();
        confirmDeliveryDialogConfig.applyDefaultValues();
        manageOrderDialogConfig.applyDefaultValues();

        mainGUIConfig.setDefault();
        yourOrdersGUIConfig.setDefault();
        chooseItemGUIConfig.setDefault();
        signGUIConfig.setDefault();
        enchantGUIConfig.setDefault();
        deliverGUIConfig.setDefault();
        newOrderDialogConfig.setDefault();
        confirmDeliveryDialogConfig.setDefault();
        manageOrderDialogConfig.setDefault();


        configFile.addDefault("bstats", true);
        configFile.addDefault("check-for-updates", true);
        configFile.addDefault("log-transactions", true);
        configFile.addDefault("expires-after", 7L * 24L * 60L * 60L * 1000L);
        configFile.addDefault("minimum-price", 0.1);
        configFile.addDefault("max-collect", 1000);
        configFile.addDefault("max-collect-per-minute", 1000);
        configFile.addDefault("similarity-check", List.of(
                "minecraft:enchantments",
                "minecraft:bundle_contents",
                "minecraft:container",
                "minecraft:fireworks",
                "minecraft:instrument",
                "minecraft:potion_contents",
                "minecraft:stored_enchantments",
                "minecraft:max_stack_size",
//                "minecraft:custom_data", // Custom data doesn't exist in the registry for some reason?
                "minecraft:custom_model_data",
                "minecraft:ominous_bottle_amplifier",
                "minecraft:damage",
                "minecraft:custom_name",
                "minecraft:item_model",
                "minecraft:bundle_contents",
                "minecraft:damage_type",
                "minecraft:consumable"
        ));

        configFile.addDefault("shulker-delivering", true);

        configFile.addDefault("order-command-aliases", List.of("order", "orders"));

        for (final SortType sort : SortType.values()) {
            configFile.addDefault("sorts-display.active." + sort.getIdentifier(), sort.getDisplayActive());
            configFile.addDefault("sorts-display.inactive." + sort.getIdentifier(), sort.getDisplayInactive());
        }

        for (final OrderStatus status : OrderStatus.values()) {
            configFile.addDefault("order-status." + status.getIdentifier(), status.getText());
        }

        setDefaultMessages();
        setDefaultSounds();
    }

    public CompletableFuture<Void> reloadAsync() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
            try {
                reload();
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
            future.complete(null);
        });
        return future;
    }

    public void reload() throws IOException {
        configFile.loadContent();
        mainGUIConfig.reload();
        yourOrdersGUIConfig.reload();
        chooseItemGUIConfig.reload();
        signGUIConfig.reload();
        enchantGUIConfig.reload();
        deliverGUIConfig.reload();
        newOrderDialogConfig.reload();
        confirmDeliveryDialogConfig.reload();
        manageOrderDialogConfig.reload();

        for (final SortType sort : SortType.values()) {
            final String displayActive = configFile.getString("sorts-display.active." + sort.getIdentifier());
            final String displayInactive = configFile.getString("sorts-display.inactive." + sort.getIdentifier());
            if (displayActive != null) sort.setDisplayActive(displayActive);
            if (displayInactive != null) sort.setDisplayInactive(displayInactive);
        }

        for (final OrderStatus status : OrderStatus.values()) {
            final String display = configFile.getString("order-status." + status.getIdentifier());
            if (display != null) status.setText(display);
        }

        bStats = configFile.getBoolean("bstats");
        checkForUpdates = configFile.getBoolean("check-for-updates");
        logTransactions = configFile.getBoolean("log-transactions");
        expiresAfter = configFile.getLong("expires-after");
        minPrice = configFile.getDouble("minimum-price");
        maxCollect = configFile.getInteger("max-collect");
        maxCollectPerMinute = configFile.getInteger(("max-collect-per-minute"));
        shulkerDelivering = configFile.getBoolean("shulker-delivering");

        final List<String> rawDataComponents = configFile.getStringList("similarity-check");
        similarityCheck.clear();
        for (final String s : rawDataComponents) {
            final String[] components = s.split(":");
            if (components.length != 2) {
                Log.warn("Invalid key for similarity-check: " + s);
                continue;
            }
            similarityCheck.add(new NamespacedKey(components[0], components[1]));
        }

        orderCommandAliases.clear();
        orderCommandAliases.addAll(configFile.getStringList("order-command-aliases"));

        orderCreationSuccessful = configFile.getString("messages.create-order-success");
        invalidInput = configFile.getString("messages.invalid-input");
        deliver = configFile.getString("messages.deliver");
        receiveDelivery = configFile.getString("messages.receive-delivery");
        notEnoughMoney = configFile.getString("messages.not-enough-money");
        deliverSelf = configFile.getString("messages.deliver-self");
        exceedMaxCollect = configFile.getString("messages.exceeded-max-collect");
        collectingTooFast = configFile.getString("messages.collecting-too-fast");

        nextPageSound = getSound("next-page");
        previousPageSound = getSound("previous-page");
        refreshSound = getSound("refresh");
        sortSound = getSound("sort");
        newOrderSound = getSound("new-order");
        deliverSound = getSound("deliver");
    }

    private Sound getSound(String name) {
        final String soundKey = configFile.getString("sounds." + name + ".sound");
        assert soundKey != null;
        final String[] components = soundKey.split(":");
        final String namespace, key;
        if (components.length != 2) {
            Log.warn("Invalid key for sound: " + soundKey + ". Using default: \"orderium:unknown_sound\"");
            namespace = "orderium";
            key = "unknown_sound";
        } else {
            namespace = components[0];
            key = components[1];
        }


        return Sound.sound(new NamespacedKey(namespace, key), Sound.Source.UI, configFile.getFloat("sounds." + name + ".volume"), configFile.getFloat("sounds." + name + ".pitch"));
    }

    private void setDefaultSound(String name, Sound sound) {
        configFile.addDefault("sounds." + name + ".sound", sound.name().asString());
        configFile.addDefault("sounds." + name + ".volume", sound.volume());
        configFile.addDefault("sounds." + name + ".pitch", sound.pitch());
    }

    private void setDefaultSounds() {
        final Sound clickSound = Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1, 1);
        setDefaultSound("next-page", clickSound);
        setDefaultSound("previous-page", clickSound);
        setDefaultSound("refresh", clickSound);
        setDefaultSound("sort", clickSound);
        setDefaultSound("new-order", Sound.sound(Key.key("minecraft:entity.villager.work_cartographer"), Sound.Source.UI, 1, 1));
        setDefaultSound("deliver", Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.UI, 1, 2));
    }

    private void setDefaultMessages() {
        configFile.addDefault("messages.create-order-success", "<gray>Your order has been created");
        configFile.addDefault("messages.invalid-input", "<red>Invalid number or format");
        configFile.addDefault("messages.deliver", "<gray>You earned <green>$<money><gray> from delivering an order");
        configFile.addDefault("messages.receive-delivery", "<aqua><deliverer> <gray>delivered you <aqua><amount> <item>");
        configFile.addDefault("messages.not-enough-money", "<red>You do not have enough money");
        configFile.addDefault("messages.deliver-self", "<red>You cannot deliver your own order");
        configFile.addDefault("messages.exceeded-max-collect", "<red>You are collecting too many items");
        configFile.addDefault("messages.collecting-too-fast", "<red>You are collecting items too fast. Wait a minute...");
    }
}
