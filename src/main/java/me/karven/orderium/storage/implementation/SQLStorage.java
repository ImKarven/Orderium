package me.karven.orderium.storage.implementation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import me.karven.orderium.api.events.OrderRemoveEvent;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.StorageMethod;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.storage.object.order.OrderRow;
import me.karven.orderium.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.config.Config.config;

public class SQLStorage extends Storage {

    // Universal Statements
    private final String CREATE_TRANSACTION_TABLE = "CREATE TABLE IF NOT EXISTS " + TRANSACTION_TABLE + " (id INTEGER PRIMARY KEY, time BIGINT, player_most BIGINT, player_least BIGINT, `before` DOUBLE, amount DOUBLE, `after` DOUBLE)";
    private final String CREATE_ORDER = "INSERT INTO " + ORDER_TABLE + " (owner_most, owner_least, item, money_per, amount, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
//    private final String UPDATE_ORDER = "UPDATE " + ORDER_TABLE + " SET amount = ?, money_per = ?, delivered = ?, in_storage = ? WHERE id = ?";
    private final String UPDATE_ORDER = "UPDATE " + ORDER_TABLE + " SET amount = ?, money_per = ?, delivered = ?, in_storage = ?, expires_at = ?, state = state + 1 WHERE state = ? AND id = ?";
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
        conf.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;");
        conf.setPoolName("orders data pool");
        conf.setJdbcUrl(jdbcUrl);
        if (username != null) conf.setUsername(username);
        if (password != null) conf.setPassword(password);
        data = new HikariDataSource(conf);

        switch (method) {
            case SQLITE -> CREATE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT, state INT DEFAULT 0)";

            default -> CREATE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY AUTO_INCREMENT, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT, state INT DEFAULT 0)";
        }
        final Consumer<Void> loadOrders = _ -> loadOrders().thenAccept(plugin.getDataCache()::setOrders)
                .exceptionally(exception -> {
                    throw new RuntimeException(exception);
                });
        final Consumer<Void> postTablesCreation = _ -> performMigration().thenAccept(loadOrders)
                .exceptionally(exception -> {
                    throw new RuntimeException(exception);
                });

        createTables().thenAccept(postTablesCreation)
                .exceptionally(exception -> {
                    throw new RuntimeException(exception);
                });
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
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Order> createOrder(final OfflinePlayer owner, ItemStack item, int amount, double moneyPer) {
        CompletableFuture<Order> future = new CompletableFuture<>();
        // TODO: Add order limit check here
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement create = connection.prepareStatement(CREATE_ORDER, Statement.RETURN_GENERATED_KEYS)
            ) {
                long expiresAt = System.currentTimeMillis() + config.expiresAfter;
                final UUID ownerUUID = owner.getUniqueId();
                create.setLong(1, ownerUUID.getMostSignificantBits());
                create.setLong(2, ownerUUID.getLeastSignificantBits());
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
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Double> cancelOrder(Order order) {
        return cancelOrder(order, 1);
    }

    public CompletableFuture<Double> cancelOrder(Order order, final int attempt) {
        CompletableFuture<Double> future = new CompletableFuture<>();

        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrder = connection.prepareStatement(GET_ORDER);
                    PreparedStatement updateOrder = connection.prepareStatement(UPDATE_ORDER)
            ) {
                int orderId = order.getId();
                getOrder.setInt(1, orderId);
                ResultSet raw = getOrder.executeQuery();
                final OrderRow row = OrderRow.fromSQL(raw);
                if (row == null) {
                    future.complete(-1.0);
                    return;
                }
                int delivered = row.delivered();
                int orderAmount = row.amount();
                int inStorage = row.inStorage();
                double moneyPer = row.moneyPer();
                long expiresAt = row.expiresAt();
                final int state = row.state();
                if (expiresAt < System.currentTimeMillis()) {
                    future.complete(-1.0);
                    return;
                }

                if (delivered == orderAmount) {
                    future.complete(-1.0);
                    return;
                }
                double payBack = (orderAmount - delivered) * moneyPer;
                if (inStorage == 0) {
                    if (deleteOrder(order) != null) {
                        plugin.getDataCache().deleteOrder(order);
                        future.complete(payBack);
                    } else future.complete(-1.0);
                    return;
                }
                final OrderRow updatedRow = new OrderRow(
                        row.id(),
                        row.owner(),
                        row.itemBytes(),
                        row.moneyPer(),
                        row.amount(),
                        row.delivered(),
                        row.inStorage(),
                        System.currentTimeMillis() - 1,
                        state
                );
                updatedRow.toSQL(updateOrder);
                final int modifiedRows = updateOrder.executeUpdate();
                if (modifiedRows > 0) {
                    // TODO: ????? what does this do
                    plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, delivered, inStorage);
                    future.complete(payBack);
                    return;
                }

                if (attempt >= 5) {
                    future.complete(-1.0);
                    return;
                }

                cancelOrder(order, attempt + 1).thenAccept(future::complete);
            } catch (SQLException e) {
                Log.error("Failed to cancel order", e);
                future.completeExceptionally(e);
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
    public CompletableFuture<Double> deliverOrder(final Player deliverer, final Order order, final Iterable<ItemStack> items) {
        return deliverOrder(deliverer, order, items, 1);
    }

    public CompletableFuture<Double> deliverOrder(Player deliverer, Order order, Iterable<ItemStack> items, final int attempt) {
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
                final OrderRow row = OrderRow.fromSQL(raw);
                if (row == null) {
                    connection.commit();
                    future.complete(null);
                    return;
                }
                int delivered = row.delivered();
                int orderAmount = row.amount();
                int inStorage = row.inStorage();
                double moneyPer = row.moneyPer();

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

                final OrderRow updatedRow = new OrderRow(
                        row.id(),
                        row.owner(),
                        row.itemBytes(),
                        moneyPer,
                        orderAmount,
                        newDelivered,
                        inStorage + newDelivered - delivered,
                        row.expiresAt(),
                        row.state()
                );

                updatedRow.toSQL(updateOrder);

                final int modifiedRows = updateOrder.executeUpdate();
                if (modifiedRows > 0) {
                    plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, newDelivered, inStorage + newDelivered - delivered);
                    connection.commit();
                    future.complete((newDelivered - delivered) * moneyPer);
                    return;
                }

                if (attempt >= 5) {
                    future.complete(null);
                    return;
                }

                deliverOrder(deliverer, order, items, attempt + 1).thenAccept(future::complete);
            } catch (SQLException e) {
                Log.error("Failed to deliver order", e);
                future.completeExceptionally(e);
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
    public CompletableFuture<Boolean> collectItems(final Order order, final int amount) {
        return collectItems(order, amount, 1);
    }

    public CompletableFuture<Boolean> collectItems(Order order, int amount, final int attempt) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrder = connection.prepareStatement(GET_ORDER);
                    PreparedStatement updateOrder = connection.prepareStatement(UPDATE_ORDER)
            ) {
                int orderId = order.getId();
                connection.setAutoCommit(false);
                getOrder.setInt(1, orderId);
                ResultSet raw = getOrder.executeQuery();
                final OrderRow row = OrderRow.fromSQL(raw);
                if (row == null) {
                    connection.commit();
                    future.complete(false);
                    return;
                }
                int delivered = row.delivered();
                int orderAmount = row.amount();
                int inStorage = row.inStorage();
                double moneyPer = row.moneyPer();
                if (inStorage < amount) {
                    connection.commit();
                    future.complete(false);
                    return;
                }

                if (inStorage - amount == 0 && (delivered == orderAmount || order.getExpiresAt() < System.currentTimeMillis())) {
                    if (deleteOrder(order) == null) {
                        connection.commit();
                        // TODO: proper message instead of assuming invalid value
                        future.complete(false);
                        return;
                    }

                    plugin.getDataCache().deleteOrder(order);
                    connection.commit();
                    future.complete(true);
                    return;
                }

                final OrderRow updatedRow = new OrderRow(
                        orderId,
                        row.owner(),
                        row.itemBytes(),
                        moneyPer,
                        orderAmount,
                        delivered,
                        inStorage - amount,
                        row.expiresAt(),
                        row.state()
                );

                updatedRow.toSQL(updateOrder);

                final int modifiedRows = updateOrder.executeUpdate();

                if (modifiedRows > 0) {
                    plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, delivered, inStorage - amount);
                    connection.commit();
                    future.complete(true);
                    return;
                }

                // TODO: proper message instead of assuming invalid value
                if (attempt >= 5) {
                    future.complete(false);
                    return;
                }

                collectItems(order, amount, attempt + 1).thenAccept(future::complete);
            } catch (SQLException e) {
                Log.error("Failed to collect items", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public CompletableFuture<Boolean> updateOrder(Order order, Order.Field field, Object value) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement getOrder = connection.prepareStatement(GET_ORDER);
                    PreparedStatement updateOrder = connection.prepareStatement("UPDATE " + ORDER_TABLE + " SET " + field.getColumnName() + " = ? WHERE id = ?")
            ) {
                connection.setAutoCommit(false);
                int orderId = order.getId();
                getOrder.setInt(1, orderId);
                ResultSet raw = getOrder.executeQuery();
                if (!raw.next()) {
                    connection.commit();
                    future.complete(false);
                    return;
                }
                int delivered = raw.getInt("delivered");
                int amount = raw.getInt("amount");
                int inStorage = raw.getInt("in_storage");
                double moneyPer = raw.getDouble("money_per");
                long expiresAt = raw.getLong("expires_at");

                switch (field) {
                    case DELIVERED -> delivered = (int) value;
                    case AMOUNT -> amount = (int) value;
                    case IN_STORAGE -> inStorage = (int) value;
                    case MONEY_PER -> moneyPer = (double) value;
                }

                if ((delivered == amount || expiresAt <= System.currentTimeMillis()) && inStorage == 0) {
                    if (deleteOrder(order) != null) {
                        plugin.getDataCache().deleteOrder(order);
                        future.complete(false);
                    } else future.complete(null);
                } else {
                    updateOrder.setObject(1, value);
                    updateOrder.setInt(2, orderId);
                    updateOrder.executeUpdate();
                    plugin.getDataCache().updateOrder(order, moneyPer, amount, delivered, inStorage);
                    future.complete(true);
                }

                connection.commit();
            } catch (SQLException e) {
                Log.error("Failed to update order", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteOrder(Order order) {
        // TODO: I hate this stupid event
        final OrderRemoveEvent.Pre preEvent = new OrderRemoveEvent.Pre(order, !Bukkit.isPrimaryThread());
        if (!preEvent.callEvent()) {
            return null;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {
            try (
                    Connection connection = data.getConnection();
                    PreparedStatement deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                deleteOrder.setInt(1, order.getId());
                deleteOrder.executeUpdate();
                plugin.getDataCache().deleteOrder(order);
                final OrderRemoveEvent.Post postEvent = new OrderRemoveEvent.Post(order, true);
                postEvent.callEvent();
                future.complete(null);
            } catch (SQLException e) {
                Log.error("Failed to delete order", e);
                future.completeExceptionally(e);
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
               future.completeExceptionally(e);
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
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> performMigration() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        DispatchUtil.async(() -> {
            try (
                    final Connection connection = data.getConnection();
                    final PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + ORDER_TABLE + " ADD COLUMN state INTEGER NOT NULL DEFAULT 0")
            ) {
                if (columnExists(connection, ORDER_TABLE, "state")) {
                    future.complete(null);
                    return;
                }

                statement.executeUpdate();

            } catch (SQLException e) {
                Log.error("Failed to migrate database", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
