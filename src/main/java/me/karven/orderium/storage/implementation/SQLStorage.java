package me.karven.orderium.storage.implementation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.StorageMethod;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.utils.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.config.Config.config;

public class SQLStorage extends Storage {

    // Universal Statements
    private final String CREATE_TRANSACTION_TABLE = "CREATE TABLE IF NOT EXISTS " + TRANSACTION_TABLE + " (time BIGINT PRIMARY KEY, player_most BIGINT, player_least BIGINT, `before` DOUBLE, amount DOUBLE, `after` DOUBLE)";
    private final String CREATE_ORDER = "INSERT INTO " + ORDER_TABLE + " (owner_most, owner_least, item, money_per, amount, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
    private final String UPDATE_ORDER = "UPDATE " + ORDER_TABLE + " SET amount = ?, money_per = ?, delivered = ?, in_storage = ? WHERE id = ?";
    private final String DELETE_ORDER = "DELETE FROM " + ORDER_TABLE + " WHERE id = ?";
    private final String CANCEL_ORDER = "UPDATE " + ORDER_TABLE + " SET expires_at = ? WHERE id = ?";
    private final String GET_ORDER = "SELECT * FROM " + ORDER_TABLE + " WHERE id = ?";
    private final String LOG_TRANSACTION = "INSERT INTO " + TRANSACTION_TABLE + " (time, player_most, player_least, `before`, amount, `after`) VALUES (?, ?, ?, ?, ?, ?)";

    // Standalone Statements
    private final String CREATE_ORDER_TABLE;

    private final HikariDataSource data;

//    public static SQLStorage mysql() {
//        return new SQLStorage(StorageMethod.MYSQL, "jdbc:mysql://" + configs.remoteAddress + "/" + configs.databaseName, configs.dbUsername, configs.dbPassword);
//    }

    public static SQLStorage h2() {
        return new SQLStorage(StorageMethod.H2, "jdbc:h2:" + dataDir + File.separator + "data-h2", "sa", "");
    }

    public static SQLStorage sqlite() {
        return new SQLStorage(StorageMethod.SQLITE, "jdbc:sqlite:" + dataDir + File.separator + "data.db", null, null);
    }

    private SQLStorage(StorageMethod method, String jdbcUrl, String username, String password) {
        super();
        HikariConfig conf = new HikariConfig();
        conf.setPoolName("orders data pool");
        conf.setJdbcUrl(jdbcUrl);
        if (username != null) conf.setUsername(username);
        if (password != null) conf.setPassword(password);
        data = new HikariDataSource(conf);

        switch (method) {
            case SQLITE -> CREATE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";

            default -> CREATE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY AUTO_INCREMENT, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
        }

        createTables().thenAccept(ignored -> loadOrders().thenAccept(plugin.getDataCache()::setOrders));
    }

    @Override
    public CompletableFuture<Collection<Order>> loadOrders() {
        CompletableFuture<Collection<Order>> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrders = connection.prepareStatement("SELECT * FROM " + ORDER_TABLE)
            ) {
                ResultSet raw = getOrders.executeQuery();
                future.complete(ConvertUtils.convertOrders(raw));
            } catch (SQLException e) {
                Log.error("Failed to load orders", e);
                future.complete(null);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Order> createOrder(UUID owner, ItemStack item, int amount, double moneyPer) {
        CompletableFuture<Order> future = new CompletableFuture<>();
        // TODO: Add order limit check here
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement create = connection.prepareStatement(CREATE_ORDER, Statement.RETURN_GENERATED_KEYS)
            ) {
                long expiresAt = System.currentTimeMillis() + config.expiresAfter;
                create.setLong(1, owner.getMostSignificantBits());
                create.setLong(2, owner.getLeastSignificantBits());
                create.setBytes(3, item.serializeAsBytes());
                create.setDouble(4, moneyPer);
                create.setInt(5, amount);
                create.setLong(6, expiresAt);
                create.executeUpdate();

                ResultSet generated = create.getGeneratedKeys();
                if (!(generated.next())) throw new RuntimeException("Failed to create order. No generated keys found");

                Order order = new Order(
                        generated.getInt(1),
                        owner, item, moneyPer, amount,
                        0, 0, expiresAt
                );
                plugin.getDataCache().addOrder(order);

                future.complete(order);
            } catch (SQLException e) {
                Log.error("Error while creating an order", e);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Double> cancelOrder(Order order) {
        CompletableFuture<Double> future = new CompletableFuture<>();

        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrder = connection.prepareStatement(GET_ORDER);
                    PreparedStatement cancelOrder = connection.prepareStatement(CANCEL_ORDER);
                    PreparedStatement deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                int orderId = order.getId();
                getOrder.setInt(1, orderId);
                ResultSet raw = getOrder.executeQuery();
                if (!raw.next()) {
                    future.complete(-1.0);
                    return;
                }
                int delivered = raw.getInt("delivered");
                int orderAmount = raw.getInt("amount");
                int inStorage = raw.getInt("in_storage");
                double moneyPer = raw.getDouble("money_per");
                long expiresAt = raw.getLong("expires_at");
                if (expiresAt < System.currentTimeMillis()) {
                    future.complete(-1.0);
                    return;
                }
                double payBack = (orderAmount - delivered) * moneyPer;
                if (inStorage == 0) {
                    deleteOrder.setInt(1, orderId);
                    deleteOrder.executeUpdate();
                    plugin.getDataCache().deleteOrder(order, true);
                    future.complete(payBack);
                    return;
                }
                cancelOrder.setLong(1, System.currentTimeMillis() - 1);
                cancelOrder.setInt(2, order.getId());
                cancelOrder.executeUpdate();
                plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, delivered, inStorage);
                future.complete(payBack);
            } catch (SQLException e) {
                Log.error("Failed to cancel order", e);
            }
        });
        return future;
    }

    /**
     * deliver an order from an inventory of items
     * @param deliverer the player that is delivering the order
     * @param order the order the player is delivering
     * @param items the inventory of items
     * @return the amount of money the player receive after delivering
     */
    @Override
    public CompletableFuture<Double> deliverOrder(Player deliverer, Order order, Iterable<ItemStack> items) {
        CompletableFuture<Double> future = new CompletableFuture<>();

        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrder = connection.prepareStatement(GET_ORDER);
                    PreparedStatement updateOrder = connection.prepareStatement(UPDATE_ORDER)
            ) {
                connection.setAutoCommit(false);
                int orderId = order.getId();
                getOrder.setInt(1, orderId);
                ResultSet raw = getOrder.executeQuery();
                if (!raw.next()) {
                    connection.commit();
                    future.complete(null);
                    return;
                }
                int delivered = raw.getInt("delivered");
                int orderAmount = raw.getInt("amount");
                int inStorage = raw.getInt("in_storage");
                double moneyPer = raw.getDouble("money_per");

                int deliverable = orderAmount - delivered;

                for (ItemStack item : items) {
                    if (!AlgoUtils.isSimilar(item, order.getItem())) {
                        if (isShulkerBox(item) && config.shulkerDelivering) {
                            deliverable = scanShulkerBox(item, order.getItem(), deliverable);
                        }
                        PlayerUtils.give(deliverer, item, true);
                        continue;
                    }
                    int itemAmount = item.getAmount();
                    if (deliverable >= itemAmount) {
                        deliverable -= itemAmount;
                        continue;
                    }
                    item.setAmount(itemAmount - deliverable);
                    PlayerUtils.give(deliverer, item, true);
                    deliverable = 0;
                }
                int newDelivered = orderAmount - deliverable;
                updateOrder.setInt(1, orderAmount);
                updateOrder.setDouble(2, moneyPer);
                updateOrder.setInt(3, newDelivered);
                updateOrder.setInt(4, inStorage + newDelivered - delivered);
                updateOrder.setInt(5, orderId);
                updateOrder.executeUpdate();
                plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, newDelivered, inStorage + newDelivered - delivered);
                connection.commit();
                future.complete((newDelivered - delivered) * moneyPer);
            } catch (SQLException e) {
                Log.error("Failed to deliver order", e);
            }
        });
        return future;
    }

    /**
     * scan this shulker box for similar items
     * @param shulkerBox the shulker box to scan
     * @param comparer the item to check for similarity
     * @param deliverable the maximum amount of items can be delivered
     * @return the new deliverable value after scanning
     */
    @SuppressWarnings("UnstableApiUsage")
    private int scanShulkerBox(ItemStack shulkerBox, ItemStack comparer, int deliverable) {
        ItemContainerContents shulkerContent = shulkerBox.getData(DataComponentTypes.CONTAINER);
        List<ItemStack> declinedItems = new ArrayList<>();
        if (shulkerContent == null) return deliverable;
        for (final ItemStack item : shulkerContent.contents()) {
            if (item == null || item.isEmpty()) {
                // Add empty items to keep the order of the items in the shulker
                declinedItems.add(ItemStack.empty());
                continue;
            }
            if (deliverable == 0) {
                declinedItems.add(item);
                continue;
            }
            if (!AlgoUtils.isSimilar(item, comparer)) {
                declinedItems.add(item);
                continue;
            }
            int itemAmount = item.getAmount();
            if (deliverable >= itemAmount) {
                deliverable -= itemAmount;
                declinedItems.add(ItemStack.empty());
                continue;
            }
            item.setAmount(itemAmount - deliverable);
            deliverable = 0;
            declinedItems.add(item);
        }
        ItemContainerContents contentAfterScan = ItemContainerContents.containerContents(declinedItems);
        shulkerBox.setData(DataComponentTypes.CONTAINER, contentAfterScan);
        return deliverable;
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean isShulkerBox(ItemStack item) {
        return item.hasData(DataComponentTypes.CONTAINER);
    }

    @Override
    public CompletableFuture<Boolean> collectItems(Order order, int amount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrder = connection.prepareStatement(GET_ORDER);
                    PreparedStatement updateOrder = connection.prepareStatement(UPDATE_ORDER);
                    PreparedStatement deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                int orderId = order.getId();
                connection.setAutoCommit(false);
                getOrder.setInt(1, orderId);
                ResultSet raw = getOrder.executeQuery();
                if (!raw.next()) {
                    connection.commit();
                    future.complete(false);
                    return;
                }
                int delivered = raw.getInt("delivered");
                int orderAmount = raw.getInt("amount");
                int inStorage = raw.getInt("in_storage");
                double moneyPer = raw.getDouble("money_per");
                if (inStorage < amount) {
                    connection.commit();
                    future.complete(false);
                    return;
                }
                if (inStorage - amount == 0 && (delivered == orderAmount || order.getExpiresAt() < System.currentTimeMillis())) {
                    deleteOrder.setInt(1, orderId);
                    deleteOrder.executeUpdate();
                    plugin.getDataCache().deleteOrder(order, true);
                } else {
                    updateOrder.setInt(1, orderAmount);
                    updateOrder.setDouble(2, moneyPer);
                    updateOrder.setInt(3, delivered);
                    updateOrder.setInt(4, inStorage - amount);
                    updateOrder.setInt(5, orderId);
                    updateOrder.executeUpdate();
                    plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, delivered, inStorage - amount);
                }
                connection.commit();
                future.complete(true);
            } catch (SQLException e) {
                Log.error("Failed to collect items", e);
            }
        });

        return future;
    }

    public CompletableFuture<Void> updateOrder(Order order, String var, Object value) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement updateOrder = connection.prepareStatement("UPDATE " + ORDER_TABLE + " SET " + var + " = ? WHERE id = ?")
            ) {
                int orderId = order.getId();
                updateOrder.setObject(1, value);
                updateOrder.setInt(2, orderId);
                updateOrder.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Log.error("Failed to update order", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteOrder(Order order) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                deleteOrder.setInt(1, order.getId());
                deleteOrder.executeUpdate();
                plugin.getDataCache().deleteOrder(order, true);
                future.complete(null);
            } catch (SQLException e) {
                Log.error("Failed to delete order", e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> logTransaction(UUID player, double before, double amount, double after) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
           try (
                   Connection connection = data.getConnection();
                   PreparedStatement logTransaction = connection.prepareStatement(LOG_TRANSACTION)
           ) {
               logTransaction.setLong(1, System.currentTimeMillis());
               logTransaction.setLong(2, player.getMostSignificantBits());
               logTransaction.setLong(3, player.getLeastSignificantBits());
               logTransaction.setDouble(4, before);
               logTransaction.setDouble(5, amount);
               logTransaction.setDouble(6, after);
              logTransaction.executeUpdate();
              future.complete(null);
           } catch (SQLException e) {
               Log.error("Failed to log transaction", e);
           }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> createTables() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try (
                Connection connection = data.getConnection();
                PreparedStatement createOrderTable = connection.prepareStatement(CREATE_ORDER_TABLE);
                PreparedStatement createTransactionTable = connection.prepareStatement(CREATE_TRANSACTION_TABLE)
        ) {
            createOrderTable.executeUpdate();
            createTransactionTable.executeUpdate();
            future.complete(null);
        } catch (SQLException e) {
            Log.error("Failed to create tables", e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> performMigration() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        return future;
    }


}
