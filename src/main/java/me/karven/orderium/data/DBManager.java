package me.karven.orderium.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.val;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.MoneyTransaction;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.Pair;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DBManager {
    private final Orderium plugin;
    private final ConfigManager configs;
    private final HikariConfig dbConfig = new HikariConfig();
    private final HikariDataSource dataSource;
    private final String dbFilePath;
    private static final String PREFIX = "orderium_";
    private static final String ORDER_TABLE = PREFIX + "orders";
    private static final String TRANSACTION_TABLE = PREFIX + "transactions";
    private static final String BLACKLIST_TABLE = PREFIX + "blacklist";
    /// pre-1.1.0: PREFIX + "custom_items"
    /// > 1.1.0: PREFIX + "custom_items_v2"
    private static final String CUSTOM_ITEMS_TABLE = PREFIX + "custom_items_v2";

    private final MoneyTransaction moneyTransaction;

    private final HikariConfig itemConfig = new HikariConfig();
    private final HikariDataSource itemDataSource;

    private final HikariConfig modifiedItemsConfig = new HikariConfig();
    private final HikariDataSource modifiedItemDataSource;

    private final boolean isExecuting = false;
    @Getter
    private final List<Order> orders = new ArrayList<>();
    private final Set<Order> mostMoneyPerItem = new TreeSet<>(
            Comparator.comparingDouble(Order::moneyPer).reversed().thenComparing(Order::id)
    );
    private final Set<Order> recentlyListed = new TreeSet<>(
            Comparator.comparingLong(Order::expiresAt).reversed().thenComparing(Order::id)
    );
    private final Set<Order> mostDelivered = new TreeSet<>(
            Comparator.comparingInt(Order::delivered).reversed().thenComparing(Order::id)
    );
    private final Set<Order> mostPaid = new TreeSet<>(
            Comparator.comparingDouble(Order::paid).reversed().thenComparing(Order::id)
    );

    @Getter
    private final Set<Pair<ItemStack, String>> customItems = new HashSet<>();
    @Getter
    private final Set<ItemStack> blacklistedItems = new HashSet<>();

    public DBManager(Orderium plugin) {
        this.plugin = plugin;
        this.configs = plugin.getConfigs();
        moneyTransaction = EconUtils.getCurrentTransaction();
        dbFilePath = plugin.getDataFolder() + File.separator + "data.db";
        dbConfig.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
        dataSource = new HikariDataSource(dbConfig);

        itemConfig.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "items.db");
        itemDataSource = new HikariDataSource(itemConfig);

        modifiedItemsConfig.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "modified_items.db");
        modifiedItemDataSource = new HikariDataSource(modifiedItemsConfig);

        execSync("CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)");
        reloadOrders();

        execSync("CREATE TABLE IF NOT EXISTS " + TRANSACTION_TABLE + " (time BIGINT PRIMARY KEY, player_most BIGINT, player_least BIGINT, before DOUBLE, amount DOUBLE, after DOUBLE)");

        execSync(modifiedItemDataSource, "CREATE TABLE IF NOT EXISTS " + CUSTOM_ITEMS_TABLE + " (item BLOB, search VARCHAR(65535))");
        reloadCustomItems();

        execSync(modifiedItemDataSource, "CREATE TABLE iF NOT EXISTS " + BLACKLIST_TABLE + " (item BLOB)");
        reloadBlacklist();
    }

    public CompletableFuture<List<ItemStack>> getItems() {
        final String TABLE_NAME = "items_" + plugin.VERSION;
        final List<ItemStack> items = new ArrayList<>();
        final CompletableFuture<List<ItemStack>> res = new CompletableFuture<>();
        query(itemDataSource, raw -> {
            try (raw) {
                while (raw.next()) {
                    items.add(ItemStack.deserializeBytes(raw.getBytes(1)));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
            }
            res.complete(items);
        }, "SELECT * FROM " + TABLE_NAME);
        return res;
    }

    public void addBlacklist(ItemStack item) {
        exec(modifiedItemDataSource, "INSERT INTO " + BLACKLIST_TABLE + " (item) VALUES (?)", (Object) item.serializeAsBytes());
        blacklistedItems.add(item);
    }

    public void removeBlacklist(ItemStack item) {
        exec(modifiedItemDataSource, "DELETE FROM " + BLACKLIST_TABLE + " WHERE item = (?)", (Object) item.serializeAsBytes());
        blacklistedItems.remove(item);
    }

    public void addCustomItem(ItemStack item) {
        exec(modifiedItemDataSource, "INSERT INTO " + CUSTOM_ITEMS_TABLE + " (item) VALUES (?, ?)", item.serializeAsBytes(), "");
        customItems.add(new Pair<>(item, ""));
    }

    public void removeCustomItem(ItemStack item) {
        exec(modifiedItemDataSource, "DELETE FROM " + CUSTOM_ITEMS_TABLE + " WHERE item = (?)", (Object) item.serializeAsBytes());
        customItems.removeIf(e -> e.first().equals(item));
    }

    public void updateCustomItemSearch(Pair<ItemStack, String> item) {
        exec(modifiedItemDataSource, "UPDATE " + CUSTOM_ITEMS_TABLE + " SET search = ? WHERE item = ?", item.second, item.first.serializeAsBytes());

    }

    private void reloadOrders() {
        query(rawOrders -> {
            orders.clear();
            orders.addAll(ConvertUtils.convertOrders(rawOrders));
            mostMoneyPerItem.addAll(orders);
            recentlyListed.addAll(orders);
            mostDelivered.addAll(orders);
            mostPaid.addAll(orders);
        }, "SELECT * FROM " + ORDER_TABLE);
    }

    private void reloadCustomItems() {
        query(modifiedItemDataSource, rawItems -> {
            customItems.clear();
            customItems.addAll(ConvertUtils.convertSearchableItems(rawItems));
        }, "SELECT * FROM " + CUSTOM_ITEMS_TABLE);
    }

    private void reloadBlacklist() {
        query(modifiedItemDataSource,rawItems -> {
            blacklistedItems.clear();
            blacklistedItems.addAll(ConvertUtils.convertItems(rawItems));
        },  "SELECT * FROM " + BLACKLIST_TABLE);
    }
    
    public void createOrder(UUID owner, ItemStack item, double moneyPer, int amount) {
        byte[] itemData = item.serializeAsBytes();
        long expiresAt = System.currentTimeMillis() + configs.getExpiresAfter();

        final Consumer<PreparedStatement> cb = stmt -> {
            try (final ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next()) return;
                final int id = rs.getInt(1);
                final Order order = new Order(
                        id,
                        owner,
                        item,
                        moneyPer,
                        amount,
                        0,
                        0,
                        expiresAt
                );
                orders.add(order);
                mostMoneyPerItem.add(order);
                recentlyListed.add(order);
                mostDelivered.add(order);
                mostPaid.add(order);
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
            }
        };

        exec("INSERT INTO " + ORDER_TABLE + " (owner_most, owner_least, item, money_per, amount, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
                owner.getMostSignificantBits(), owner.getLeastSignificantBits(), itemData, moneyPer, amount, expiresAt).thenAccept(cb);
    }

    public CompletableFuture<List<Integer>> dataVersions() {
        val ret = new CompletableFuture<List<Integer>>();
        final List<Integer> ver = new ArrayList<>();
        query(itemDataSource, res -> {
            try (res) {
                while (res.next()) {
                    final String sName = res.getString("name");
                    if (!sName.startsWith("items_")) continue;
                    try {
                        ver.add(Integer.parseInt(sName.replace("items_", "")));
                    } catch (Exception ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
            }
            ret.complete(ver);
        }, "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
        return ret;
    }

    public void collectOrder(int orderId, int amount) {
        int idx = getIdx(orderId);
        if (idx == -1) return;
        final Order order = orders.get(idx);
        collectOrder(order, amount);
    }
    public void collectOrder(Order order, int amount) {
        final int newVal = order.inStorage() - amount;
        order.setInStorage(newVal);
        exec("UPDATE " + ORDER_TABLE + " SET in_storage = ? WHERE id = ?", newVal, order.id());

        if (order.inStorage() == 0 && order.delivered() == order.amount()) deleteOrder(order);
    }

    public void deliverOrder(int orderId, int amount) {
        int idx = getIdx(orderId);
        if (idx == -1) return;
        final Order order = orders.get(idx);
        deliverOrder(order, amount);
    }

    public void deliverOrder(Order order, int amount) {
        mostDelivered.remove(order);
        mostPaid.remove(order);
        final int newVal = order.delivered() + amount;
        order.setDelivered(newVal);
        order.setInStorage(order.inStorage() + amount);
        exec("UPDATE " + ORDER_TABLE + " SET delivered = ? WHERE id = ?", newVal, order.id());
        mostDelivered.add(order);
        mostPaid.add(order);
    }

    public void deleteOrder(int orderId) {
        int idx = getIdx(orderId);
        if (idx == -1) return;
        final Order order = orders.get(idx);
        deleteOrder(order);
    }

    public void deleteOrder(Order order) {
        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
        orders.remove(order);
        exec("DELETE FROM " + ORDER_TABLE + " WHERE id = ?", order.id());
    }

    private int getIdx(int orderId) {
        // Use binary-search algorithm to find the index since the IDs are sorted
        int l = 0, r = Math.min(orderId, orders.size()) - 1;
        int ans = -1;
        while (l <= r) {
            final int m = (l + r) / 2;
            if (orders.get(m).id() >= orderId) {
                r = m - 1;
                ans = m;
            } else l = m + 1;
        }
        return ans;
    }

    public List<Order> getActiveOrders() {
        return orders.stream().filter(Order::isActive).toList();
    }

    public Set<Order> getSortedOrders(SortTypes sortType) {
        switch (sortType) {
            case MOST_MONEY_PER_ITEM -> { return mostMoneyPerItem; }
            case RECENTLY_LISTED -> { return recentlyListed; }
            case MOST_DELIVERED -> { return mostDelivered; }
            case MOST_PAID -> { return mostPaid; }
        }
        return new HashSet<>(orders);
    }

    public List<Order> getOrders(UUID ownerId) {
        val toDel = new ArrayList<Order>();
        val res = orders.stream().filter(order -> {
            if (!order.owner().equals(ownerId)) return false;
            if (order.shouldBeDeleted()) {
                toDel.add(order);
                return false;
            }
            return true;
        }).toList();
        toDel.forEach(this::deleteOrder);
        return res;
    }

    public void logTransaction() {
        final UUID uuid = moneyTransaction.player;
        exec("INSERT INTO " + TRANSACTION_TABLE + " (time, player_most, player_least, before, amount, after) VALUES (?, ?, ?, ?, ?, ?)",
                System.currentTimeMillis(),
                uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits(),
                moneyTransaction.before,
                moneyTransaction.amount,
                moneyTransaction.after
                );
    }

    private CompletableFuture<PreparedStatement> exec(String stmt, Object... params) {
        return exec(dataSource, stmt, params);
    }

    private void execSync(String stmt, Object... params) {
        execSync(dataSource, stmt, params);
    }

    private CompletableFuture<PreparedStatement> exec(HikariDataSource dataSource, String stmt, Object... params) {
        final CompletableFuture<PreparedStatement> completableFuture = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            try (
                    final Connection connection = dataSource.getConnection();
                    final PreparedStatement preparedStatement = connection.prepareStatement(stmt);
            ) {
                processStatement(preparedStatement, params);
                preparedStatement.executeUpdate();
                completableFuture.complete(preparedStatement);
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
            }
        });
        return completableFuture;
    }


    private void execSync(HikariDataSource dataSource, String stmt, Object... params) {
        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(stmt)
        ) {
            processStatement(preparedStatement, params);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }
    }

    private void query(Consumer<ResultSet> cb, String stmt, Object... params) {
        query(dataSource, cb, stmt, params);
    }

    private void query(HikariDataSource dataSource, Consumer<ResultSet> cb, String stmt, Object... params) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            try (
                    final Connection connection = dataSource.getConnection();
                    final PreparedStatement preparedStatement = connection.prepareStatement(stmt)
            ) {
                processStatement(preparedStatement, params);
                cb.accept(preparedStatement.executeQuery());
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
            }
        });
    }

    private void processStatement(PreparedStatement statement, Object... vals) throws SQLException {
        for (int i = 0; i < vals.length; i++) {
            statement.setObject(i + 1, vals[i]);
        }
    }
}
