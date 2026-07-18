package me.karven.orderium.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.SignGUIConfig;
import me.karven.orderium.config.util.WebhookConfig;
import me.karven.orderium.config.util.chestgui.*;
import me.karven.orderium.config.util.dialog.ConfirmDeliveryDialogConfig;
import me.karven.orderium.config.util.dialog.ManageOrderDialogConfig;
import me.karven.orderium.config.util.dialog.NewOrderDialogConfig;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.OrderStatus;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.Log;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.utils.Values.ERROR_TRACKER;

public class Config {
    private static final AtomicBoolean reloading = new AtomicBoolean(false);
    public static volatile Config config;
    public static final int CURRENT_CONFIG_VERSION = 6;
    public final File javaConfigFile = new File(plugin.getDataFolder(), "config.yml");

    public ConfigFile configFile;

    public final WebhookConfig webhookConfig = new WebhookConfig();

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
    public String orderCreationBroadcast;

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
    public boolean broadcastOrderCreation;

    public final List<@NotNull String> orderCommandAliases = new ArrayList<>();

    public final List<NamespacedKey> similarityCheck = new ArrayList<>();

    public final Map<String, Integer> ordersLimit = new HashMap<>();

    public Config() throws Exception {

        try {
            configFile = ConfigFile.loadConfig(javaConfigFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (configFile.isNew()) {
            setDefaults();
            save();
            load();
        } else ConfigMigration.perform(this);
    }

    public void save() throws Exception {
        configFile.set("config-version", CURRENT_CONFIG_VERSION);
        configFile.save();
        webhookConfig.saveToFile();
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
        webhookConfig.applyDefaultValues();
        mainGUIConfig.applyDefaultValues();
        yourOrdersGUIConfig.applyDefaultValues();
        chooseItemGUIConfig.applyDefaultValues();
        signGUIConfig.applyDefaultValues();
        enchantGUIConfig.applyDefaultValues();
        deliverGUIConfig.applyDefaultValues();
        newOrderDialogConfig.applyDefaultValues();
        confirmDeliveryDialogConfig.applyDefaultValues();
        manageOrderDialogConfig.applyDefaultValues();

        webhookConfig.setDefault();
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
        configFile.addDefault("broadcast-order-creation", false);

        configFile.addDefault("order-command-aliases", List.of("order", "orders"));

        for (final SortType sort : SortType.values()) {
            configFile.addDefault("sorts-display.active." + sort.getIdentifier(), sort.getDisplayActive());
            configFile.addDefault("sorts-display.inactive." + sort.getIdentifier(), sort.getDisplayInactive());
        }

        for (final OrderStatus status : OrderStatus.values()) {
            configFile.addDefault("order-status." + status.getIdentifier(), status.getText());
        }

        configFile.addDefault("orders-limit", List.of(Map.of("permission", "default", "limit", 27)));

        setDefaultMessages();
        setDefaultSounds();
    }

    public static CompletableFuture<Void> reloadAsync() {
        if (reloading.get()) return null;
        reloading.set(true);
        final CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {

            try {
                reload();
            } catch (Exception e) {
                Log.error("Failed to reload config", e);
                future.completeExceptionally(e);
                return;
            }

            future.complete(null);
            if (!reloading.get()) {
                final AssertionError error = new AssertionError("Reloading is false. This should never happen.");
                ERROR_TRACKER.trackError(error);
                Log.error("", error);
                throw error;
            }
            reloading.set(false);
        });
        return future;
    }

    public static void reload() throws Exception {
        config = new Config();

        plugin.reloadBStats(config);
        ChooseItemGUI.init();

        AdminToolGUI.createBlacklist();
        AdminToolGUI.createCustomItems();

        for (final Order order : DataCache.getInstance().getSortedOrders(SortType.MOST_MONEY_PER_ITEM)) {
            order.reload();
        }
    }

    public void reloadGUIs() {
        mainGUIConfig.reload();
        yourOrdersGUIConfig.reload();
        chooseItemGUIConfig.reload();
        signGUIConfig.reload();
        enchantGUIConfig.reload();
        deliverGUIConfig.reload();
        newOrderDialogConfig.reload();
        confirmDeliveryDialogConfig.reload();
        manageOrderDialogConfig.reload();
    }

    public void reloadGUIsFromFile() {
        mainGUIConfig.reloadFromFile();
        yourOrdersGUIConfig.reloadFromFile();
        chooseItemGUIConfig.reloadFromFile();
        signGUIConfig.reloadFromFile();
        enchantGUIConfig.reloadFromFile();
        deliverGUIConfig.reloadFromFile();
        newOrderDialogConfig.reloadFromFile();
        confirmDeliveryDialogConfig.reloadFromFile();
        manageOrderDialogConfig.reloadFromFile();
    }


    public void load() {
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
        broadcastOrderCreation = configFile.getBoolean("broadcast-order-creation");

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

        ordersLimit.clear();
        final List<Map<String, Object>> ordersLimitList = configFile.getList("orders-limit");
        for (final Map<String, Object> limit : ordersLimitList) {
            ordersLimit.put((String) limit.get("permission"), (int) limit.get("limit"));
        }

        orderCreationSuccessful = configFile.getString("messages.create-order-success");
        invalidInput = configFile.getString("messages.invalid-input");
        deliver = configFile.getString("messages.deliver");
        receiveDelivery = configFile.getString("messages.receive-delivery");
        notEnoughMoney = configFile.getString("messages.not-enough-money");
        deliverSelf = configFile.getString("messages.deliver-self");
        exceedMaxCollect = configFile.getString("messages.exceeded-max-collect");
        collectingTooFast = configFile.getString("messages.collecting-too-fast");
        orderCreationBroadcast = configFile.getString("messages.order-creation-broadcast");

        nextPageSound = getSound("next-page");
        previousPageSound = getSound("previous-page");
        refreshSound = getSound("refresh");
        sortSound = getSound("sort");
        newOrderSound = getSound("new-order");
        deliverSound = getSound("deliver");

        plugin.reloadBStats(this);
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
        configFile.addDefault("messages.order-creation-broadcast", "<green><player> <white>has just created a new order for <green><item> <white>in <gray>/orders");
    }
}
