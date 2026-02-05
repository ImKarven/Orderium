package me.karven.orderium.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.MoneyTransaction;
import me.karven.orderium.obj.Order;
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
import java.util.stream.Collectors;

public class DBManager {
    private final Orderium plugin;
    private final ConfigManager configs;
    private final HikariConfig dbConfig = new HikariConfig();
    private final HikariDataSource dataSource;
    private final String dbFilePath;
    private static final String PREFIX = "orderium_";
    private static final String ORDER_TABLE = PREFIX + "orders";
    private static final String TRANSACTION_TABLE = PREFIX + "transactions";
    private final MoneyTransaction moneyTransaction;
    private final boolean isExecuting = false;
    @Getter
    private List<Order> orders = new ArrayList<>();
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

    public DBManager(Orderium plugin) {
        this.plugin = plugin;
        this.configs = plugin.getConfigs();
        moneyTransaction = EconUtils.getCurrentTransaction();
        dbFilePath = plugin.getDataFolder() + File.separator + "data.db";
        dbConfig.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
        dataSource = new HikariDataSource(dbConfig);


        exec("CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)");
        exec("CREATE TABLE IF NOT EXISTS " + TRANSACTION_TABLE + " (time BIGINT PRIMARY KEY, player_most BIGINT, player_least BIGINT, before DOUBLE, amount DOUBLE, after DOUBLE)");

        reloadOrders();
    }

    private void reloadOrders() {
        query("SELECT * FROM " + ORDER_TABLE).thenAccept(rawOrders -> {
            try (rawOrders) {
                orders = ConvertUtils.convertOrders(rawOrders);
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
                return;
            }
            mostMoneyPerItem.addAll(orders);
            recentlyListed.addAll(orders);
            mostDelivered.addAll(orders);
            mostPaid.addAll(orders);
        });
    }
    
    public void createOrder(UUID owner, ItemStack item, double moneyPer, int amount) {
        byte[] itemData = item.serializeAsBytes();
        long expiresAt = System.currentTimeMillis() + configs.getExpiresAfter();

        exec("INSERT INTO " + ORDER_TABLE + " (owner_most, owner_least, item, money_per, amount, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
                owner.getMostSignificantBits(), owner.getLeastSignificantBits(), itemData, moneyPer, amount, expiresAt).thenAccept(stmt -> {
                    try (final ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
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
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().severe(e.toString());
                    }
        });


    }

    public void collectOrder(int orderId, int amount) {
        int idx = getIdx(orderId);
        if (idx == -1) return;
        final Order order = orders.get(idx);
        final int newVal = order.inStorage() - amount;
        order.setInStorage(newVal);
        exec("UPDATE " + ORDER_TABLE + " SET in_storage = ? WHERE id = ?", newVal, orderId);

        if (order.inStorage() == 0 && order.delivered() == order.amount()) deleteOrder(orderId);
    }

    @Deprecated
    public void deliverOrder(int orderId, int amount) {
        int idx = getIdx(orderId);
        if (idx == -1) return;
        final Order order = orders.get(idx);
        mostDelivered.remove(order);
        mostPaid.remove(order);
        final int newVal = Math.min(order.delivered() + amount, order.amount());
        order.setDelivered(newVal);
        order.setInStorage(order.inStorage() + amount);
        exec("UPDATE " + ORDER_TABLE + " SET delivered = ? WHERE id = ?", newVal, orderId);
        mostDelivered.add(order);
        mostPaid.add(order);
    }

    public double deliverOrder(Order order, int amount) {
        mostDelivered.remove(order);
        mostPaid.remove(order);
        final int newVal = order.delivered() + amount;
        order.setDelivered(newVal);
        order.setInStorage(order.inStorage() + amount);
        exec("UPDATE " + ORDER_TABLE + " SET delivered = ? WHERE id = ?", newVal, order.id());
        mostDelivered.add(order);
        mostPaid.add(order);
        return order.moneyPer() * amount;
    }

    public void deleteOrder(int orderId) {
        int idx = getIdx(orderId);
        if (idx == -1) return;
        final Order order = orders.get(idx);
        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
        orders.remove(idx);
        exec("DELETE FROM " + ORDER_TABLE + " WHERE id = ?", orderId);
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
        return orders.stream().filter(order -> order.owner().equals(ownerId)).toList();
    }

    public void logTransaction() {
        final UUID uuid = moneyTransaction.player;
        exec("INSERT INTO " + TRANSACTION_TABLE + " (time, player_most, player_least, before, amount, after)",
                System.currentTimeMillis(),
                uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits(),
                moneyTransaction.before,
                moneyTransaction.amount,
                moneyTransaction.after
                );
    }

    private CompletableFuture<PreparedStatement> exec(String stmt, Object... params) {
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

    private CompletableFuture<ResultSet> query(String stmt, Object... params) {
        final CompletableFuture<ResultSet> completableFuture = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            try (
                    final Connection connection = dataSource.getConnection();
                    final PreparedStatement preparedStatement = connection.prepareStatement(stmt)
            ) {
                processStatement(preparedStatement, params);
                completableFuture.complete(preparedStatement.executeQuery());
            } catch (SQLException e) {
                plugin.getLogger().severe(e.toString());
            }
        });
        return completableFuture;
    }

    private void processStatement(PreparedStatement statement, Object... vals) throws SQLException {
        for (int i = 0; i < vals.length; i++) {
            statement.setObject(i + 1, vals[i]);
        }
    }
}
