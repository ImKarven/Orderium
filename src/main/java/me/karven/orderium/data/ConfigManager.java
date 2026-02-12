package me.karven.orderium.data;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.datacomponent.DataComponentType;
import lombok.Getter;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.OrderStatus;
import me.karven.orderium.obj.SlotInfo;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.NMSUtils;
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

@Getter
@SuppressWarnings("UnstableApiUsage")
public class ConfigManager {

    private final File configFile;
    private ConfigFile config;
    private final Orderium plugin;

    private boolean bStats = true;

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

    private boolean logTransactions = true;
    private long expiresAfter = -1;
    private String sortPrefix;
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
                plugin.getLogger().severe(e.toString());
                return false;
            }
            return true;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            try {
                loadCfg();
                ChooseItemGUI.init(plugin);
            } catch (Exception e) {
                plugin.getLogger().severe(e.toString());
            }
        });
        return true;
    }

    public void loadCfg() throws Exception {
        checkFiles();
        try {
            this.config = ConfigFile.loadConfig(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe(e.toString());
            return;
        }
        // CONFIG
        config.addDefault("bstats", true);
        config.addDefault("log-transactions", true);
        config.addDefault("expires-after", 7L * 24L * 60L * 60L * 1000L);
        config.addDefault("sort-prefix", "<aqua>");
        config.addDefault("similarity-check", List.of(
                "minecraft:enchantments",
                "minecraft:bundle_contents",
                "minecraft:container",
                "minecraft:fireworks",
                "minecraft:instrument",
                "minecraft:potion_contents",
                "minecraft:stored_enchantments",
                "minecraft:max_stack_size"
        ));

        // MESSAGES
        config.addDefault("messages.create-order-success", "<gray>Your order has been created");
        config.addDefault("messages.invalid-input", "<red>Invalid number or format");
        config.addDefault("messages.delivery", "<gray>You earned <green>$<money><gray> from delivering an order");
        config.addDefault("messages.receive-delivery", "<aqua><deliverer> <gray>delivered you <aqua><amount> <item>");
        config.addDefault("messages.not-enough-money", "<red>You do not have enough money");

        // SORT TYPES
        config.addDefault("sort-types." + SortTypes.MOST_MONEY_PER_ITEM.getIdentifier(), "Most Money Per Item");
        config.addDefault("sort-types." + SortTypes.RECENTLY_LISTED.getIdentifier(), "Recently Listed");
        config.addDefault("sort-types." + SortTypes.MOST_DELIVERED.getIdentifier(), "Most Delivered");
        config.addDefault("sort-types." + SortTypes.MOST_PAID.getIdentifier(), "Most Paid");
        config.addDefault("sort-types." + SortTypes.A_Z.getIdentifier(), "A - Z");
        config.addDefault("sort-types." + SortTypes.Z_A.getIdentifier(), "Z - A");

        // ORDER STATUS
        config.addDefault("order-status." + OrderStatus.AVAILABLE.getIdentifier(), "<gray>Expires after <day>d <hour>h <minute>m <second>s");
        config.addDefault("order-status." + OrderStatus.EXPIRED.getIdentifier(), "<red>Order Expired");
        config.addDefault("order-status." + OrderStatus.COMPLETED.getIdentifier(), "<green>Order Completed");

        // MAIN GUI
        config.addDefault("gui.main.title", "Orders");
        config.addDefault("gui.main.order-lore", List.of(
                "",
                "<#786500><paid><gray>/<#017800><total> <gray>Paid",
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
        ));
        new SlotInfo(4, List.of("<white>Click to refresh"), "<aqua>Refresh", ItemType.PAPER).addDefault(config, "gui.main.buttons.refresh");
        new SlotInfo(6, List.of("<white>Click to view your orders"), "<aqua>Your Orders", ItemType.CHEST).addDefault(config, "gui.main.buttons.your-orders");
        new SlotInfo(2, List.of(
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
                "<#786500><paid><gray>/<#017800><total> <gray>Paid",
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
        ));
        new SlotInfo(0, List.of("<white>Click to go to the previous page"), "<aqua>Back", ItemType.ARROW).addDefault(config, "gui.choose-item.buttons.back");
        new SlotInfo(8, List.of("<white>Click to go to the next page"), "<aqua>Next", ItemType.ARROW).addDefault(config, "gui.choose-item.buttons.next");
        new SlotInfo(5, List.of("<white>Click to search"), "<aqua>Search", ItemType.OAK_SIGN).addDefault(config, "gui.choose-item.buttons.search");
        new SlotInfo(2, List.of(
                "",
                "<white> • <a-z>",
                "<white> • <z-a>"
        ), "<aqua>Sort", ItemType.HOPPER).addDefault(config, "gui.choose-item.buttons.sort");

        // SEARCH SIGN GUI
        config.addDefault("gui.search-sign.type", "minecraft:oak_sign");
        config.addDefault("gui.search-sign.search-line", 1);
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
        logTransactions = config.getBoolean("log-transactions");
        expiresAfter = config.getLong("expires-after");
        sortPrefix = config.getString("sort-prefix");
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
    private void checkFiles() {
        File parent = configFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            configFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe(e.toString());
        }
    }
}
