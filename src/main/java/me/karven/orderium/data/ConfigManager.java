package me.karven.orderium.data;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.datacomponent.DataComponentType;
import lombok.Getter;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.OrderStatus;
import me.karven.orderium.obj.SlotInfo;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.NMSUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemType;
import org.intellij.lang.annotations.Subst;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Getter
@SuppressWarnings("UnstableApiUsage")
public class ConfigManager {
    private final File configFile;
    private ConfigFile config;
    private final Orderium plugin;

    private boolean bStats = true;
    private boolean checkForUpdates = true;

    private String mainGuiTitle;
    private List<String> orderLore;
    private List<SortTypes> ordersSortsOrder;
    private final SlotInfo
            refreshButton = new SlotInfo(-1, null, null, null),
            yoButton = new SlotInfo(-1, null, null, null),
            ordersSortButton = new SlotInfo(-1, null, null, null),
            ordersBackButton = new SlotInfo(-1, null, null, null),
            ordersNextButton = new SlotInfo(-1, null, null, null),
            ordersSearchButton = new SlotInfo(-1, null, null, null);

    private String yoGuiTitle;
    private List<String> yoLore;
    private final SlotInfo
            newOrderButton = new SlotInfo(-1, null, null, null);

    private String chooseItemTitle;
    private List<SortTypes> chooseSortsOrder;
    private final SlotInfo
            chooseBackButton = new SlotInfo(-1, null, null, null),
            chooseNextButton = new SlotInfo(-1, null, null, null),
            chooseSearchButton = new SlotInfo(-1, null, null, null),
            chooseSortButton = new SlotInfo(-1, null, null, null);

    private int searchLine = -1;
    private BlockType signBlock;
    private List<String> lines;

    private String deliverTitle;
    private int deliverRows = 6;

    private String newOrderDialogTitle;
    private String itemDescription;
    private String amountLabel;
    private String moneyPerLabel;
    private String changeItemButton;
    private String changeItemTooltip;
    private String confirmButton;
    private String confirmTooltip;
    private int descriptionWidth = -1;
    private int inputWidth = -1;
    private int buttonWidth = -1;

    private String confirmDeliveryTitle;
    private String confirmDeliveryBody;
    private String confirmDeliveryTransactionMessage;
    private String confirmDeliveryConfirmLabel;
    private String confirmDeliveryConfirmHover;
    private String confirmDeliveryCancelLabel;
    private String confirmDeliveryCancelHover;

    private String manageOrderTitle;
    private String manageOrderBody;
    private String collectItemsLabel;
    private String collectItemsHover;
    private String cancelOrderLabel;
    private String cancelOrderHover;

    private String collectItemsTitle;
    private String collectItemsBody;
    private String collectItemsAmountLabel;
    private String collectItemsCancelLabel;
    private String collectItemsCancelHover;
    private String collectItemsConfirmLabel;
    private String collectItemsConfirmHover;

    private String cancelOrderTitle;
    private String cancelOrderBody;
    private String cancelOrderCancelLabel;
    private String cancelOrderCancelHover;
    private String cancelOrderConfirmLabel;
    private String cancelOrderConfirmHover;

    private String invalidInput;
    private String orderCreationSuccessful;
    private String delivered;
    private String receiveDelivery;
    private String notEnoughMoney;
    private String deliverSelf;
    private String collectingTooFast;
    private String exceedMaxCollect;

    private Sound nextPageSound;
    private Sound previousPageSound;
    private Sound refreshSound;
    private Sound sortSound;
    private Sound newOrderSound;
    private Sound deliverSound;

    private boolean logTransactions = true;
    private long expiresAfter = -1;
    private String sortPrefix;
    private int maxCollectPerMinute = 1000;
    private int maxCollect = 1000;
    private TagResolver[] sortPlaceholders;

    private final List<DataComponentType.Valued<?>> similarityCheck = new ArrayList<>();

    public ConfigManager(Orderium plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!reload(false)) {
            plugin.getLogger().severe("Failed to load config.");
        }
    }

    public boolean reload(boolean async) {
        if (!async) {
            try {
                loadCfg();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch item from database", e);
                return false;
            }
            return true;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            try {
                loadCfg();
                NMSUtils.init(plugin).thenAccept(ignored -> ChooseItemGUI.init(plugin));

                AdminToolGUI.createBlacklist();
                AdminToolGUI.createCustomItems();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reload", e);
            }
        });
        return true;
    }

    public void reload(Runnable cb) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            try {
                loadCfg();
                NMSUtils.init(plugin).thenAccept(ignored -> ChooseItemGUI.init(plugin));

                AdminToolGUI.createBlacklist();
                AdminToolGUI.createCustomItems();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reload", e);
            }
            cb.run();
        });
    }

    public void loadCfg() throws Exception {
        checkFiles();
        try {
            this.config = ConfigFile.loadConfig(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load config file", e);
            return;
        }
        // CONFIG
        config.addDefault("bstats", true, "Whether to let bStats collect data anonymously or not");
        config.addDefault("check-for-updates", true, "Whether to check for updates or not");
        config.addDefault("log-transactions", true, "Whether to log money changes of players or not");
        config.addDefault("expires-after", 7L * 24L * 60L * 60L * 1000L, "After this amount of millisecond(s), the order will be expired");
        config.addDefault("sort-prefix", "<aqua>", "This will be put at the beginning of the sort type that is being selected");
        config.addDefault("max-collect", 1000, "Maximum amount of items to collect, this shouldn't be confused with max-collect-per-minute");
        config.addDefault("max-collect-per-minute", 1000,
                "The maximum amount of items a player can collect every minute\n" +
                "Setting this too high might allow players to lag the server with large orders\n" +
                "The 1-minute timer is global, not per-player.");
        config.addDefault("similarity-check", List.of(
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
                "minecraft:ominous_bottle_amplifier"
        ), "This define how should two items to be similar.\n" +
                "If all of the following data component types are equal on both items beside their item types, they are similar.\n" +
                "This similarity check happens when a player deliver an order, it accepts items in the delivery inventory that are similar to the one in the order\n" +
                "See a list of data components here, note that only use ones that exist on your server version: https://minecraft.wiki/w/Data_component_format#List_of_components");

        // MESSAGES
        config.addDefault("messages.create-order-success", "<gray>Your order has been created");
        config.addDefault("messages.invalid-input", "<red>Invalid number or format");
        config.addDefault("messages.delivery", "<gray>You earned <green>$<money><gray> from delivering an order");
        config.addDefault("messages.receive-delivery", "<aqua><deliverer> <gray>delivered you <aqua><amount> <item>");
        config.addDefault("messages.not-enough-money", "<red>You do not have enough money");
        config.addDefault("messages.deliver-self", "<red>You cannot deliver your own order");
        config.addDefault("messages.exceeded-max-collect", "<red>You are collecting too many items", "Message for max-collect");
        config.addDefault("messages.collecting-too-fast", "<red>You are collecting items too fast. Wait a minute...", "Message for max-collect-per-minute");

        // SOUNDS
        config.addDefault("sounds.next-page.sound", "minecraft:ui.button.click");
        config.addDefault("sounds.next-page.volume", 1.0);
        config.addDefault("sounds.next-page.pitch", 1.0);

        config.addDefault("sounds.previous-page.sound", "minecraft:ui.button.click");
        config.addDefault("sounds.previous-page.volume", 1.0);
        config.addDefault("sounds.previous-page.pitch", 1.0);

        config.addDefault("sounds.refresh.sound", "minecraft:ui.button.click");
        config.addDefault("sounds.refresh.volume", 1.0);
        config.addDefault("sounds.refresh.pitch", 1.0);

        config.addDefault("sounds.sort.sound", "minecraft:ui.button.click");
        config.addDefault("sounds.sort.volume", 1.0);
        config.addDefault("sounds.sort.pitch", 1.0);

        config.addDefault("sounds.new-order.sound", "minecraft:entity.villager.work_cartographer");
        config.addDefault("sounds.new-order.volume", 1.0);
        config.addDefault("sounds.new-order.pitch", 1.0);

        config.addDefault("sounds.deliver.sound", "minecraft:entity.player.levelup");
        config.addDefault("sounds.deliver.volume", 1.0);
        config.addDefault("sounds.deliver.pitch", 2.0);

        // SORT TYPES
        config.addComment("sort-types", "How should different types of sorting appear");
        config.addDefault("sort-types." + SortTypes.MOST_MONEY_PER_ITEM.getIdentifier(), "Most Money Per Item");
        config.addDefault("sort-types." + SortTypes.RECENTLY_LISTED.getIdentifier(), "Recently Listed");
        config.addDefault("sort-types." + SortTypes.MOST_DELIVERED.getIdentifier(), "Most Delivered");
        config.addDefault("sort-types." + SortTypes.MOST_PAID.getIdentifier(), "Most Paid");
        config.addDefault("sort-types." + SortTypes.A_Z.getIdentifier(), "A - Z");
        config.addDefault("sort-types." + SortTypes.Z_A.getIdentifier(), "Z - A");

        // ORDER STATUS
        config.addComment("order-status", "How should different types of order status appear\nThey will be used with <order-status> placeholder");
        config.addDefault("order-status." + OrderStatus.AVAILABLE.getIdentifier(), "<gray>Expires after <day>d <hour>h <minute>m <second>s");
        config.addDefault("order-status." + OrderStatus.EXPIRED.getIdentifier(), "<red>Order Expired");
        config.addDefault("order-status." + OrderStatus.COMPLETED.getIdentifier(), "<green>Order Completed");

        // MAIN GUI
        config.addDefault("gui.main.title", "Orders");
        config.addDefault("gui.main.order-lore", List.of(
                "",
                "<#786500>$<paid><gray>/<#017800>$<total> <gray>Paid",
                "<#786500><delivered><gray>/<#017800><amount> <gray>Delivered",
                "<green>$<money-per> <white>each",
                "",
                "<white>Click to deliver <aqua><player><white>'s order"
        ));
        config.addDefault("gui.main.sorts-order", List.of(
                "most-money-per-item",
                "recently-listed",
                "most-delivered",
                "most-paid"
        ), "This indicates the next sort type to select when switching to another one\n" +
                "A_Z and Z_A sorts are not supported.");
        new SlotInfo(4, List.of("<white>Click to refresh"), "<aqua>Refresh", ItemType.PAPER).addDefault(config, "gui.main.buttons.refresh");
        new SlotInfo(6, List.of("<white>Click to view your orders"), "<aqua>Your Orders", ItemType.CHEST).addDefault(config, "gui.main.buttons.your-orders");
        new SlotInfo(3, List.of(
                "",
                "<white> • <most-money-per-item>",
                "<white> • <recently-listed>",
                "<white> • <most-delivered>",
                "<white> • <most-paid>"
        ), "<aqua>Sort", ItemType.HOPPER).addDefault(config, "gui.main.buttons.sort");
        new SlotInfo(0, List.of("<white>Click to go to the previous page"), "<aqua>Back", ItemType.ARROW).addDefault(config, "gui.main.buttons.back");
        new SlotInfo(8, List.of("<white>Click to go to the next page"), "<aqua>Next", ItemType.ARROW).addDefault(config, "gui.main.buttons.next");
        new SlotInfo(5, List.of("<white>Click to search"), "<aqua>Search", ItemType.OAK_SIGN).addDefault(config, "gui.main.buttons.search");

        // YOUR ORDERS GUI
        config.addDefault("gui.your-orders.title", "Your Orders");
        config.addDefault("gui.your-orders.order-lore", List.of(
                "",
                "<#786500>$<paid><gray>/<#017800>$<total> <gray>Paid",
                "<#786500><delivered><gray>/<#017800><amount> <gray>Delivered",
                "<green>$<money-per> <white>each",
                "",
                "<order-status>"
        ));
        new SlotInfo(-1, List.of("<white>Click to create a new order"), "<aqua>New Order", ItemType.MAP).addDefault(config, "gui.your-orders.buttons.new-order");

        // CHOOSE ITEM GUI
        config.addDefault("gui.choose-item.title", "Choose Your Item");
        config.addDefault("gui.choose-item.sorts-order", List.of(
                "a-z",
                "z-a"
        ), "Only A_Z and Z_A sorts are supported, don't put other sort types in here");
        new SlotInfo(0, List.of("<white>Click to go to the previous page"), "<aqua>Back", ItemType.ARROW).addDefault(config, "gui.choose-item.buttons.back");
        new SlotInfo(8, List.of("<white>Click to go to the next page"), "<aqua>Next", ItemType.ARROW).addDefault(config, "gui.choose-item.buttons.next");
        new SlotInfo(5, List.of("<white>Click to search"), "<aqua>Search", ItemType.OAK_SIGN).addDefault(config, "gui.choose-item.buttons.search");
        new SlotInfo(3, List.of(
                "",
                "<white> • <a-z>",
                "<white> • <z-a>"
        ), "<aqua>Sort", ItemType.HOPPER).addDefault(config, "gui.choose-item.buttons.sort");

        // SEARCH SIGN GUI
        config.addDefault("gui.search-sign.type", "minecraft:oak_sign");
        config.addDefault("gui.search-sign.search-line", 1, "This indicates what line to take as the search query.\n" +
                "By default, it's 1, so whatever the player puts in the first line of the sign will be used as the search query");
        config.addDefault("gui.search-sign.lines", List.of(
                "",
                "↑↑↑↑↑↑↑↑↑↑↑↑",
                "Search",
                ""
        ));

        // DELIVERY GUI
        config.addDefault("gui.delivery.title", "Delivering...");
        config.addDefault("gui.delivery.rows", 6);

        // NEW ORDER DIALOG
        config.addDefault("gui.new-order.title", "Create A New Order");
        config.addDefault("gui.new-order.item-description", "You're creating an order for this item");
        config.addDefault("gui.new-order.amount-label", "Amount");
        config.addDefault("gui.new-order.money-per-label", "Money Per Item");
        config.addDefault("gui.new-order.change-item-button", "Change Item...");
        config.addDefault("gui.new-order.change-item-tooltip", "Click to change the item");
        config.addDefault("gui.new-order.confirm-button", "<green>Confirm");
        config.addDefault("gui.new-order.confirm-tooltip", "Click to confirm the order");
        config.addDefault("gui.new-order.description-width", 210);
        config.addDefault("gui.new-order.input-width", 200);
        config.addDefault("gui.new-order.button-width", 150);

        // CONFIRM DELIVERY DIALOG
        config.addDefault("gui.confirm-delivery.title", "Confirm your Delivery");
        config.addDefault("gui.confirm-delivery.body", "You are delivering");
        config.addDefault("gui.confirm-delivery.transaction-message", "You will get <green>$<money><white> in return");
        config.addDefault("gui.confirm-delivery.confirm-button", "<green>Confirm");
        config.addDefault("gui.confirm-delivery.confirm-tooltip", "Click to confirm the delivery");
        config.addDefault("gui.confirm-delivery.cancel-button", "<red>Cancel");
        config.addDefault("gui.confirm-delivery.cancel-tooltip", "Click to cancel the delivery");

        // MANAGE ORDER DIALOG
        config.addDefault("gui.manage-order.title", "Manage Order");
        config.addDefault("gui.manage-order.body", "You are managing this order");
        config.addDefault("gui.manage-order.collect-items-button", "Collect Items");
        config.addDefault("gui.manage-order.collect-items-tooltip", "Click to collect items from this order");
        config.addDefault("gui.manage-order.cancel-order-button", "Cancel Order");
        config.addDefault("gui.manage-order.cancel-order-tooltip", "Click to cancel the order");

        // COLLECT ITEMS DIALOG
        config.addDefault("gui.collect-items.title", "Collect Items");
        config.addDefault("gui.collect-items.body", "You are collecting items from this order. You can collect up to <aqua><in-storage> <item>");
        config.addDefault("gui.collect-items.amount-label", "Amount");
        config.addDefault("gui.collect-items.cancel-button", "<red>Cancel");
        config.addDefault("gui.collect-items.cancel-tooltip", "Click to cancel");
        config.addDefault("gui.collect-items.confirm-button", "<green>Confirm");
        config.addDefault("gui.collect-items.confirm-tooltip", "Click to confirm");

        // CANCEL ORDER DIALOG
        config.addDefault("gui.cancel-order.title", "Cancel Order");
        config.addDefault("gui.cancel-order.body", "You are cancelling this order. It will be expired");
        config.addDefault("gui.cancel-order.cancel-button", "<red>Cancel");
        config.addDefault("gui.cancel-order.cancel-tooltip", "Click to cancel the cancellation of this order");
        config.addDefault("gui.cancel-order.confirm-button", "<green>Confirm");
        config.addDefault("gui.cancel-order.confirm-tooltip", "Click to confirm the cancellation of this order");

        config.save();
        config.reload();

        bStats = config.getBoolean("bstats");
        checkForUpdates = config.getBoolean("check-for-updates");
        logTransactions = config.getBoolean("log-transactions");
        expiresAfter = config.getLong("expires-after");
        sortPrefix = config.getString("sort-prefix");
        maxCollect = config.getInteger("max-collect");
        maxCollectPerMinute = config.getInteger(("max-collect-per-minute"));
        sortPlaceholders = new TagResolver[SortTypes.values().length];
        int i = 0;
        for (SortTypes sortType : SortTypes.values()) {
            @Subst("ignored")
            final String identifier = sortType.getIdentifier();
            sortType.setDisplay(config.getString("sort-types." + identifier));
            assert sortType.getDisplay() != null;
            sortPlaceholders[i++] = Placeholder.parsed(identifier, sortType.getDisplay());
        }

        for (OrderStatus status : OrderStatus.values()) {
            status.setText(config.getString("order-status." + status.getIdentifier()));
        }

        final List<String> rawDataComponents = config.getStringList("similarity-check");
        similarityCheck.clear();
        for (final String s : rawDataComponents) {
            final DataComponentType.Valued<?> dataComponentType = ConvertUtils.getDataComponentType(s);
            if (dataComponentType == null) {
                plugin.getLogger().severe("Failed to get data component type with identifier " + s);
                continue;
            }
            similarityCheck.add(dataComponentType);
        }

        orderCreationSuccessful = config.getString("messages.create-order-success");
        invalidInput = config.getString("messages.invalid-input");
        delivered = config.getString("messages.delivery");
        receiveDelivery = config.getString("messages.receive-delivery");
        notEnoughMoney = config.getString("messages.not-enough-money");
        deliverSelf = config.getString("messages.deliver-self");
        exceedMaxCollect = config.getString("messages.exceeded-max-collect");
        collectingTooFast = config.getString("messages.collecting-too-fast");

        nextPageSound = getSound("next-page");
        previousPageSound = getSound("previous-page");
        refreshSound = getSound("refresh");
        sortSound = getSound("sort");
        newOrderSound = getSound("new-order");
        deliverSound = getSound("deliver");

        mainGuiTitle = config.getString("gui.main.title");
        orderLore = config.getStringList("gui.main.order-lore");
        ordersSortsOrder = config.getStringList("gui.main.sorts-order").stream().map(SortTypes::fromIdentifier).toList();
        refreshButton.deserialize(config.getConfigSection("gui.main.buttons.refresh"));
        yoButton.deserialize(config.getConfigSection("gui.main.buttons.your-orders"));
        ordersSortButton.deserialize(config.getConfigSection("gui.main.buttons.sort"));
        ordersBackButton.deserialize(config.getConfigSection("gui.main.buttons.back"));
        ordersNextButton.deserialize(config.getConfigSection("gui.main.buttons.next"));
        ordersSearchButton.deserialize(config.getConfigSection("gui.main.buttons.search"));

        yoGuiTitle = config.getString("gui.your-orders.title");
        yoLore = config.getStringList("gui.your-orders.order-lore");
        newOrderButton.deserialize(config.getConfigSection("gui.your-orders.buttons.new-order"));

        chooseItemTitle = config.getString("gui.choose-item.title");
        chooseSortsOrder = config.getStringList("gui.choose-item.sorts-order").stream().map(SortTypes::fromIdentifier).toList();
        chooseBackButton.deserialize(config.getConfigSection("gui.choose-item.buttons.back"));
        chooseNextButton.deserialize(config.getConfigSection("gui.choose-item.buttons.next"));
        chooseSearchButton.deserialize(config.getConfigSection("gui.choose-item.buttons.search"));
        chooseSortButton.deserialize(config.getConfigSection("gui.choose-item.buttons.sort"));

        searchLine = config.getInteger("gui.search-sign.search-line");
        lines = config.getStringList("gui.search-sign.lines");
        signBlock = NMSUtils.getBlockType(config.getString("gui.search-sign.type"));

        deliverTitle = config.getString("gui.delivery.title");
        deliverRows = config.getInteger("gui.delivery.rows");

        newOrderDialogTitle = config.getString("gui.new-order.title");
        itemDescription = config.getString("gui.new-order.item-description");
        amountLabel = config.getString("gui.new-order.amount-label");
        moneyPerLabel = config.getString("gui.new-order.money-per-label");
        changeItemButton = config.getString("gui.new-order.change-item-button");
        changeItemTooltip = config.getString("gui.new-order.change-item-tooltip");
        confirmButton = config.getString("gui.new-order.confirm-button");
        confirmTooltip = config.getString("gui.new-order.confirm-tooltip");
        descriptionWidth = config.getInteger("gui.new-order.description-width");
        inputWidth = config.getInteger("gui.new-order.input-width");
        buttonWidth = config.getInteger("gui.new-order.button-width");

        confirmDeliveryTitle = config.getString("gui.confirm-delivery.title");
        confirmDeliveryBody = config.getString("gui.confirm-delivery.body");
        confirmDeliveryTransactionMessage = config.getString("gui.confirm-delivery.transaction-message");
        confirmDeliveryConfirmLabel = config.getString("gui.confirm-delivery.confirm-button");
        confirmDeliveryConfirmHover = config.getString("gui.confirm-delivery.confirm-tooltip");
        confirmDeliveryCancelLabel = config.getString("gui.confirm-delivery.cancel-button");
        confirmDeliveryCancelHover = config.getString("gui.confirm-delivery.cancel-tooltip");

        manageOrderTitle = config.getString("gui.manage-order.title");
        manageOrderBody = config.getString("gui.manage-order.body");
        collectItemsLabel = config.getString("gui.manage-order.collect-items-button");
        collectItemsHover = config.getString("gui.manage-order.collect-items-tooltip");
        cancelOrderLabel = config.getString("gui.manage-order.cancel-order-button");
        cancelOrderHover = config.getString("gui.manage-order.cancel-order-tooltip");

        collectItemsTitle = config.getString("gui.collect-items.title");
        collectItemsBody = config.getString("gui.collect-items.body");
        collectItemsAmountLabel = config.getString("gui.collect-items.amount-label");
        collectItemsCancelLabel = config.getString("gui.collect-items.cancel-button");
        collectItemsCancelHover = config.getString("gui.collect-items.cancel-tooltip");
        collectItemsConfirmLabel = config.getString("gui.collect-items.confirm-button");
        collectItemsConfirmHover = config.getString("gui.collect-items.confirm-tooltip");

        cancelOrderTitle = config.getString("gui.cancel-order.title");
        cancelOrderBody = config.getString("gui.cancel-order.body");
        cancelOrderCancelLabel = config.getString("gui.cancel-order.cancel-button");
        cancelOrderCancelHover = config.getString("gui.cancel-order.cancel-tooltip");
        cancelOrderConfirmLabel = config.getString("gui.cancel-order.confirm-button");
        cancelOrderConfirmHover = config.getString("gui.cancel-order.confirm-tooltip");
    }

    private Sound getSound(String name) {
        return Sound.sound(Key.key(config.getString("sounds." + name + ".sound")), Sound.Source.UI, config.getFloat("sounds." + name + ".volume"), config.getFloat("sounds." + name + ".pitch"));
    }

    private void checkFiles() {
        File parent = configFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            configFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create plugin files", e);
        }
    }
}
