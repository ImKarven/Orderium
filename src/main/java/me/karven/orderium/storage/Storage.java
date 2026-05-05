package me.karven.orderium.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.val;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.Pair;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;
import me.karven.orderium.obj.orderitem.VanillaItem;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.Log;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static me.karven.orderium.load.Orderium.plugin;

public abstract class Storage {
    protected static ConfigCache configs;
    protected static File dataDir;
    protected final String ORDER_TABLE = configs.getTablePref() + "orders";
    protected final String TRANSACTION_TABLE = configs.getTablePref() + "transactions";
    private final String CUSTOM_ITEMS_TABLE = "orderium_custom_items_v2";
    private final String BLACKLIST_TABLE = "orderium_blacklist";

    private final HikariDataSource modifiedItemDataSource;

    public static void init() {
        Storage.configs = plugin.getConfigs();
        Storage.dataDir = plugin.getDataFolder();
    }

    protected Storage() {
        val modifiedItemsConfig = new HikariConfig();
        modifiedItemsConfig.setPoolName("modified items pool");
        modifiedItemsConfig.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "modified_items.db");
        this.modifiedItemDataSource = new HikariDataSource(modifiedItemsConfig);

        val itemsList = loadItems();
        val blacklistAndCustomItems = loadBlacklistAndCustomItems();

        plugin.getDataCache().setItems(itemsList, blacklistAndCustomItems.first, blacklistAndCustomItems.second);
    }

    public void addBlacklist(BlacklistedItem item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val addItem = connection.prepareStatement("INSERT INTO " + BLACKLIST_TABLE + " (item) VALUES (?)")
        ) {
            addItem.setBytes(1, item.getItemAsBytes());
            addItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to add blacklist item", e);
        }
    }

    public void addCustomItem(CustomItem item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val addItem = connection.prepareStatement("INSERT INTO " + CUSTOM_ITEMS_TABLE + " (item, search) VALUES (?, ?)")
        ) {
            addItem.setBytes(1, item.getItemAsBytes());
            addItem.setString(2, String.join(",", item.getSearches()));
            addItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to add custom item", e);
        }
    }

    public void removeBlacklist(BlacklistedItem item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val removeItem = connection.prepareStatement("DELETE FROM " + BLACKLIST_TABLE + " WHERE item = (?)")
        ) {
            removeItem.setBytes(1, item.getItemAsBytes());
            removeItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to remove blacklist item", e);
        }
    }

    public void removeCustomItem(CustomItem item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val removeCustomItem = connection.prepareStatement("DELETE FROM " + CUSTOM_ITEMS_TABLE + " WHERE item = (?)")
        ) {
            removeCustomItem.setBytes(1, item.getItemAsBytes());
            removeCustomItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to remove custom item", e);
        }
    }

    public void updateCustomItemSearch(CustomItem item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val updateSearch = connection.prepareStatement("UPDATE " + CUSTOM_ITEMS_TABLE + " SET search = ? WHERE item = ?")
        ) {
            updateSearch.setString(1, item.getParsedSearches());
            updateSearch.setBytes(2, item.getItemAsBytes());
            updateSearch.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to update custom item search", e);
        }
    }

    private Pair<Collection<BlacklistedItem>, Collection<CustomItem>> loadBlacklistAndCustomItems() {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val createCustomItemsTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + CUSTOM_ITEMS_TABLE + " (item BLOB, search VARCHAR(65535))");
                val createBlacklistTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + BLACKLIST_TABLE + " (item BLOB)")
        ) {
            createCustomItemsTable.executeUpdate();
            createBlacklistTable.executeUpdate();

            val getCustomItems = connection.prepareStatement("SELECT * FROM " + CUSTOM_ITEMS_TABLE);
            val getBlacklist = connection.prepareStatement("SELECT * FROM " + BLACKLIST_TABLE);

            val blacklist = ConvertUtils.convertBlacklistedItems(getBlacklist.executeQuery());
            val customItems = ConvertUtils.convertCustomItems(getCustomItems.executeQuery());

            getCustomItems.close();
            getBlacklist.close();

            return new Pair<>(blacklist, customItems);

        } catch (SQLException e) {
            Log.error("Failed to load modified items", e);
        }
        return new Pair<>(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Use minecraft internals to get the items
     * @return the default items
     */
    private Collection<VanillaItem> loadItems() {
        MinecraftServer server = MinecraftServer.getServer();
        RegistryAccess registryAccess = server.registryAccess();
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, false, registryAccess);
        Registry<CreativeModeTab> tabs = BuiltInRegistries.CREATIVE_MODE_TAB;
        Collection<net.minecraft.world.item.ItemStack> minecraftItems = new HashSet<>();
        Set<VanillaItem> items = new HashSet<>();

        for (CreativeModeTab tab : tabs) {
            tab.buildContents(params);
            minecraftItems.addAll(tab.getSearchTabDisplayItems());
        }

        for (net.minecraft.world.item.ItemStack mcItem : minecraftItems) {
            items.add(new VanillaItem(mcItem.asBukkitCopy(), true));
        }
        return items;
    }

    public abstract CompletableFuture<Collection<Order>> loadOrders();

    public abstract CompletableFuture<Order> createOrder(UUID owner, ItemStack item, int amount, double moneyPer);

    public abstract CompletableFuture<Double> cancelOrder(Order order);

    /**
     * Process a delivery from a player
     * @param deliverer the player that delivers the order
     * @param order the order the player is delivering
     * @param items the inventory the player is requesting to deliver
     * @return the amount of money the player receive for this delivery, or null if an error occurred
     */
    public abstract CompletableFuture<Double> deliverOrder(Player deliverer, Order order, Iterable<ItemStack> items);

    public abstract CompletableFuture<Void> deleteOrder(Order order);

    /**
     * Subtract {@code amount} to inStorage of an order
     * @param order the order
     * @param amount the amount to collect
     * @return {@code true} if there is enough items in storage, and they are subtracted, otherwise {@code false}
     */
    public abstract CompletableFuture<Boolean> collectItems(Order order, int amount);

    public abstract CompletableFuture<Void> updateOrder(Order order, String var, Object value);

    public abstract CompletableFuture<Void> logTransaction(UUID player, double before, double amount, double after);

    public abstract CompletableFuture<Void> createTables();

    public abstract CompletableFuture<Void>  performMigration();
}
