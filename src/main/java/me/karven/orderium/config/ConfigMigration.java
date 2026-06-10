package me.karven.orderium.config;

import com.google.common.io.Files;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.SlotInfo;
import me.karven.orderium.obj.OrderStatus;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.Log;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static me.karven.orderium.Orderium.plugin;

public class ConfigMigration {

    /**
     * Perform config migration
     *
     * @param config the config
     * @return {@code true} if migration was performed. {@code false} if nothing changed
     */
    public static boolean perform(final @NotNull Config config) throws IOException {
        config.configFile.loadContent();
        final int configVersion = config.configFile.getInteger("config-version");
        if (configVersion == Config.CURRENT_CONFIG_VERSION) return false;
        if (configVersion > Config.CURRENT_CONFIG_VERSION) {
            throw new RuntimeException("Downgrading config is not supported. Please use the latest version");
        }

        setDefaultV4(config.configFile);

        // Backup old config file
        final File backupConfig = new File(plugin.getDataFolder(), "config.yml.old");
        try {
            Files.copy(new File(plugin.getDataFolder(), "config.yml"), backupConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Convert GUI config sections to their respective files
        config.mainGUIConfig.migrateV5(config.configFile);
        config.yourOrdersGUIConfig.migrateV5(config.configFile);
        config.chooseItemGUIConfig.migrateV5(config.configFile);
        config.signGUIConfig.migrateV5(config.configFile);
        config.enchantGUIConfig.migrateV5(config.configFile);
        config.deliverGUIConfig.migrateV5(config.configFile);
        config.newOrderDialogConfig.migrateV5(config.configFile);
        config.confirmDeliveryDialogConfig.migrateV5(config.configFile);
        config.manageOrderDialogConfig.migrateV5(config.configFile);

        // Remove the gui section entirely after migration
        config.configFile.set("gui", null);

        // Merge `sort-types` and `sort-prefix` to `sorts-display`
        final String prefix = config.configFile.getString("sort-prefix");
        assert prefix != null;
        for (final SortType sort : SortType.values()) {
            final String display = config.configFile.getString("sort-types." + sort.getIdentifier());
            assert display != null;
            sort.setDisplayActive(prefix + display);
            sort.setDisplayInactive(display);
            config.configFile.set("sorts-display.active." + sort.getIdentifier(), sort.getDisplayActive());
            config.configFile.set("sorts-display.inactive." + sort.getIdentifier(), sort.getDisplayInactive());
        }

        config.configFile.set("sort-prefix", null);
        config.configFile.set("sort-types", null);
        config.configFile.set("enchantments", null);

        config.configFile.set("config-version", 5);

        try {
            config.configFile.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    // Set default values for config with version 4 or below
    @SuppressWarnings("UnstableApiUsage")
    public static void setDefaultV4(final @NotNull ConfigFile config) {
        config.set("config-version", 4);

        config.addDefault("bstats", true, "Whether to let bStats collect data anonymously or not");
        config.addDefault("check-for-updates", true, "Whether to check for updates or not");
        config.addDefault("log-transactions", true, "Whether to log money changes of players or not");
        config.addDefault("expires-after", 7L * 24L * 60L * 60L * 1000L, "After this amount of millisecond(s), the order will be expired");
        config.addDefault("minimum-price", 0.1, "The minimum amount of money per item the player can create order with");
        config.addDefault("sort-prefix", "<aqua>", "This will be put at the beginning of the sort type that is being selected");
        config.addDefault("max-collect", 1000, "Maximum amount of items to collect, this shouldn't be confused with max-collect-per-minute");
        config.addDefault("max-collect-per-minute", 1000,
                """
                        The maximum amount of items a player can collect every minute
                        Setting this too high might allow players to lag the server with large orders
                        The 1-minute timer is global, not per-player.
                        """
        );
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
                        "minecraft:ominous_bottle_amplifier",
                        "minecraft:damage",
                        "minecraft:custom_name",
                        "minecraft:item_model",
                        "minecraft:bundle_contents",
                        "minecraft:damage_type",
                        "minecraft:consumable"
                ),
                """
                This defines how should two items to be similar.
                If all the following data component types are equal on both items beside their item types, they are similar.
                This similarity check happens when a player deliver an order, it accepts items in the delivery inventory that are similar to the one in the order
                See a list of data components here, note that only use ones that exist on your server version: https://minecraft.wiki/w/Data_component_format#List_of_components
                """
        );

        config.addDefault("enchantments", false,
                """
                Whether to enable enchanting items or not.
                Currently you cannot edit what enchantments can be applied, the default will be used.
                """
        );

        config.addDefault("shulker-delivering", true, "Whether to allow players to deliver orders with items in shulker boxes");

        config.addDefault("order-command-aliases", List.of("order", "orders"),
                """
                        Execute any of these commands (aliases) open the main GUI
                        You need to restart the server to take effects
                        """
        );

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
        config.addDefault("sort-types." + SortType.MOST_MONEY_PER_ITEM.getIdentifier(), "Most Money Per Item");
        config.addDefault("sort-types." + SortType.RECENTLY_LISTED.getIdentifier(), "Recently Listed");
        config.addDefault("sort-types." + SortType.MOST_DELIVERED.getIdentifier(), "Most Delivered");
        config.addDefault("sort-types." + SortType.MOST_PAID.getIdentifier(), "Most Paid");
        config.addDefault("sort-types." + SortType.A_Z.getIdentifier(), "A - Z");
        config.addDefault("sort-types." + SortType.Z_A.getIdentifier(), "Z - A");

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

        // ENCHANT GUI
        config.addDefault("gui.enchant-item.title", "Enchant Your Item");
        config.addDefault("gui.enchant-item.name-prefix.active", "<aqua>", "This prefix is applied when an enchantment has level 1 or above");
        config.addDefault("gui.enchant-item.name-prefix.inactive", "<gray>", "This prefix is applied when the enchantment is not applied");
        config.addDefault("gui.enchant-item.lore", List.of("", "<gray>Right-click to decrease level", "<gray>Left-click to increase level"));
        new SlotInfo(8, List.of("<white>Click to confirm your enchantments"), "<green>Confirm", ItemType.GREEN_WOOL).addDefault(config, "gui.enchant-item.confirm-button");
        config.addComment("gui.enchant-item", "This GUI is only shown if the setting \"enchantments\" is true");

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

        try {
            config.save();
        } catch (Exception e) {
            Log.error("Failed to migrate config version 4", e);
        }
    }

    public static void migrateV5(final @NotNull Config config) {
        final ConfigFile oldConfigFile = config.configFile;
        config.mainGUIConfig.title = oldConfigFile.getString("gui.main.title");
        final List<String> sortsOrderString = oldConfigFile.getStringList("gui.main.sorts-order");
        config.mainGUIConfig.sortsOrderConfig.reload(sortsOrderString);
    }
}
