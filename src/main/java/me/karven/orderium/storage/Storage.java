package me.karven.orderium.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.val;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.Pair;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.Log;
import org.bukkit.Bukkit;
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

    public void addBlacklist(ItemStack item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val addItem = connection.prepareStatement("INSERT INTO " + BLACKLIST_TABLE + " (item) VALUES (?)")
        ) {
            addItem.setBytes(1, item.serializeAsBytes());
            addItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to add blacklist item", e);
        }
    }

    public void addCustomItem(ItemStack item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val addItem = connection.prepareStatement("INSERT INTO " + CUSTOM_ITEMS_TABLE + " (item, search) VALUES (?, ?)")
        ) {
            addItem.setBytes(1, item.serializeAsBytes());
            addItem.setString(2, "");
            addItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to add custom item", e);
        }
    }

    public void removeBlacklist(ItemStack item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val removeItem = connection.prepareStatement("DELETE FROM " + BLACKLIST_TABLE + " WHERE item = (?)")
        ) {
            removeItem.setBytes(1, item.serializeAsBytes());
            removeItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to remove blacklist item", e);
        }
    }

    public void removeCustomItem(ItemStack item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val removeCustomItem = connection.prepareStatement("DELETE FROM " + CUSTOM_ITEMS_TABLE + " WHERE item = (?)")
        ) {
            removeCustomItem.setBytes(1, item.serializeAsBytes());
            removeCustomItem.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to remove custom item", e);
        }
    }

    public void updateCustomItemSearch(Pair<ItemStack, String> item) {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val updateSearch = connection.prepareStatement("UPDATE " + CUSTOM_ITEMS_TABLE + " SET search = ? WHERE item = ?")
        ) {
            updateSearch.setString(1, item.second);
            updateSearch.setBytes(2, item.first.serializeAsBytes());
            updateSearch.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to update custom item search", e);
        }
    }

    private Pair<Collection<ItemStack>, Collection<Pair<ItemStack, String>>> loadBlacklistAndCustomItems() {
        try (
                val connection = modifiedItemDataSource.getConnection();
                val createCustomItemsTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + CUSTOM_ITEMS_TABLE + " (item BLOB, search VARCHAR(65535))");
                val createBlacklistTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + BLACKLIST_TABLE + " (item BLOB)");
                val getCustomItems = connection.prepareStatement("SELECT * FROM " + CUSTOM_ITEMS_TABLE);
                val getBlacklist = connection.prepareStatement("SELECT * FROM " + BLACKLIST_TABLE)
        ) {
            createCustomItemsTable.executeUpdate();
            createBlacklistTable.executeUpdate();

            val blacklist = ConvertUtils.convertItems(getBlacklist.executeQuery());
            val customItems = ConvertUtils.convertSearchableItems(getCustomItems.executeQuery());

            return new Pair<>(blacklist, customItems);

        } catch (SQLException e) {
            Log.error("Failed to load modified items", e);
        }
        return new Pair<>(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * @return the default items
     */
    private Collection<ItemStack> loadItems() {

        val itemConfig = new HikariConfig();
        itemConfig.setPoolName("items pool");
        itemConfig.setJdbcUrl("jdbc:sqlite:" + dataDir + File.separator + "items.db");
        val itemDataSource = new HikariDataSource(itemConfig);

        final List<Integer> dataVersions = new ArrayList<>();

        try (
                val connection = itemDataSource.getConnection();
                val getDataVersions = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
        ) {
            val raw = getDataVersions.executeQuery();

            while (raw.next()) {
                final String sName = raw.getString("name");
                if (!sName.startsWith("items_")) continue;
                try {
                    dataVersions.add(Integer.parseInt(sName.replace("items_", "")));
                } catch (Exception ignored) {}
            }
        } catch (SQLException e) {
            Log.error("Failed to load data versions", e);
        }
        @SuppressWarnings("deprecation")
        val dataVer = Bukkit.getUnsafe().getDataVersion();
        val RELEASE_1_21_7_DATA_VERSION = 4438;
        if (dataVer < RELEASE_1_21_7_DATA_VERSION) {
            throw new RuntimeException("Server running version older than 1.21.7, which is not supported.");
        }
        int maxVer = -1;
        for (int ver : dataVersions) {
            if (ver > maxVer && ver <= dataVer) maxVer = ver;
            if (dataVer == ver) {
                plugin.VERSION = ver;
                break;
            }
        }
        if (plugin.VERSION == -1) {
            Log.warn("No data version in the item database matches your server data version! Using the latest compatible one...");
            plugin.VERSION = maxVer;
        }

        val ITEMS_TABLE_NAME = "items_" + plugin.VERSION;
        val items = new HashSet<ItemStack>();

        try (
                val connection = itemDataSource.getConnection();
                val getItems = connection.prepareStatement("SELECT * FROM " + ITEMS_TABLE_NAME)
                ) {
            val raw = getItems.executeQuery();

            while (raw.next()) {
                items.add(ItemStack.deserializeBytes(raw.getBytes(1)));
            }

        } catch (SQLException e) {
            Log.error("Failed to load items", e);
        }
        itemDataSource.close();
        return items;
    }

    public abstract Collection<Order> loadOrders();

    public abstract CompletableFuture<Void> createOrder(UUID owner, ItemStack item, int amount, double moneyPer);

    public abstract CompletableFuture<Double> cancelOrder(Order order);

    /**
     * Add {@code amount} to delivered and in_storage value
     * @param order the order
     * @param amount the amount to deliver
     * @return number of exceeded or 0 if the delivery doesn't exceed the amount
     */
    public abstract CompletableFuture<Integer> deliverOrder(Order order, int amount);

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

    public abstract void createTables();
}
