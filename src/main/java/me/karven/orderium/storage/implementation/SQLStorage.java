package me.karven.orderium.storage.implementation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import lombok.val;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.StorageMethod;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.utils.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.karven.orderium.load.Orderium.plugin;

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

    public static SQLStorage mysql() {
        return new SQLStorage(StorageMethod.MYSQL, "jdbc:mysql://" + configs.getRemoteAddress() + "/" + configs.getDatabaseName(), configs.getDbUsername(), configs.getDbPassword());
    }

    public static SQLStorage h2() {
        return new SQLStorage(StorageMethod.H2, "jdbc:h2:" + dataDir + File.separator + "data-h2", "sa", "");
    }

    public static SQLStorage sqlite() {
        return new SQLStorage(StorageMethod.SQLITE, "jdbc:sqlite:" + dataDir + File.separator + "data.db", null, null);
    }

    private SQLStorage(StorageMethod method, String jdbcUrl, String username, String password) {
        super();
        val conf = new HikariConfig();
        conf.setPoolName("orders data pool");
        conf.setJdbcUrl(jdbcUrl);
        if (username != null) conf.setUsername(username);
        if (password != null) conf.setPassword(password);
        data = new HikariDataSource(conf);

        switch (method) {
            case SQLITE -> {
                CREATE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
            }

            default -> {
                CREATE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " (id INTEGER PRIMARY KEY AUTO_INCREMENT, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
            }
        }

        createTables();
        plugin.getDataCache().setOrders(loadOrders());
    }

    @Override
    public Collection<Order> loadOrders() {
        try (
                val connection = data.getConnection();
                val getOrders = connection.prepareStatement("SELECT * FROM " + ORDER_TABLE)
        ) {
            val raw = getOrders.executeQuery();
            return ConvertUtils.convertOrders(raw);
        } catch (SQLException e) {
            Log.error("Failed to load orders", e);
        }
        return new ArrayList<>();
    }

    @Override
    public CompletableFuture<Void> createOrder(UUID owner, ItemStack item, int amount, double moneyPer) {
        val future = new CompletableFuture<Void>();

        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val create = connection.prepareStatement(CREATE_ORDER, Statement.RETURN_GENERATED_KEYS)
            ) {
                val expiresAt = System.currentTimeMillis() + configs.getExpiresAfter();
                create.setLong(1, owner.getMostSignificantBits());
                create.setLong(2, owner.getLeastSignificantBits());
                create.setBytes(3, item.serializeAsBytes());
                create.setDouble(4, moneyPer);
                create.setInt(5, amount);
                create.setLong(6, expiresAt);
                create.executeUpdate();

                val generated = create.getGeneratedKeys();
                if (!(generated.next())) throw new RuntimeException("Failed to create order. No generated keys found");

                plugin.getDataCache().addOrder(new Order(
                        generated.getInt(1),
                        owner, item, moneyPer, amount,
                        0, 0, expiresAt
                ));

                future.complete(null);
            } catch (SQLException e) {
                Log.error("Error while creating an order", e);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Double> cancelOrder(Order order) {
        val future = new CompletableFuture<Double>();

        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val getOrder = connection.prepareStatement(GET_ORDER);
                    val cancelOrder = connection.prepareStatement(CANCEL_ORDER);
                    val deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                val orderId = order.getId();
                getOrder.setInt(1, orderId);
                val raw = getOrder.executeQuery();
                if (!raw.next()) {
                    future.complete(-1.0);
                    return;
                }
                val delivered = raw.getInt("delivered");
                val orderAmount = raw.getInt("amount");
                val inStorage = raw.getInt("in_storage");
                val moneyPer = raw.getDouble("money_per");
                val expiresAt = raw.getLong("expires_at");
                if (expiresAt < System.currentTimeMillis()) {
                    future.complete(-1.0);
                    return;
                }
                double payBack = (orderAmount - delivered) * moneyPer;
                if (inStorage == 0) {
                    deleteOrder.setInt(1, orderId);
                    deleteOrder.executeUpdate();
                    plugin.getDataCache().deleteOrder(order);
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

    @Override
    public CompletableFuture<Integer> deliverOrder(Order order, int amount) {
        val future = new CompletableFuture<Integer>();

        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val getOrder = connection.prepareStatement(GET_ORDER);
                    val updateOrder = connection.prepareStatement(UPDATE_ORDER)
            ) {
                connection.setAutoCommit(false);
                val orderId = order.getId();
                getOrder.setInt(1, orderId);
                val raw = getOrder.executeQuery();
                if (!raw.next()) {
                    connection.commit();
                    future.complete(-1);
                    return;
                }
                val delivered = raw.getInt("delivered");
                val orderAmount = raw.getInt("amount");
                val inStorage = raw.getInt("in_storage");
                val moneyPer = raw.getDouble("money_per");
                val newVal = delivered + amount;
                if (newVal <= orderAmount) {
                    updateOrder.setInt(1, orderAmount);
                    updateOrder.setDouble(2, moneyPer);
                    updateOrder.setInt(3, newVal);
                    updateOrder.setInt(4, inStorage + amount);
                    updateOrder.setInt(5, orderId);
                    updateOrder.executeUpdate();

                    plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, newVal, inStorage + amount);
                    connection.commit();
                    future.complete(0);
                    return;
                }
                updateOrder.setInt(1, orderAmount);
                updateOrder.setDouble(2, moneyPer);
                updateOrder.setInt(3, orderAmount);
                updateOrder.setInt(4, inStorage + orderAmount - delivered);
                updateOrder.setInt(5, orderId);
                updateOrder.executeUpdate();
                plugin.getDataCache().updateOrder(order, moneyPer, orderAmount, orderAmount, inStorage + orderAmount - delivered);
                connection.commit();
                future.complete(newVal - orderAmount);
            } catch (SQLException e) {
                Log.error("Failed to deliver order", e);
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
        val future = new CompletableFuture<Double>();

        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val getOrder = connection.prepareStatement(GET_ORDER);
                    val updateOrder = connection.prepareStatement(UPDATE_ORDER)
            ) {
                connection.setAutoCommit(false);
                val orderId = order.getId();
                getOrder.setInt(1, orderId);
                val raw = getOrder.executeQuery();
                if (!raw.next()) {
                    connection.commit();
                    future.complete(null);
                    return;
                }
                val delivered = raw.getInt("delivered");
                val orderAmount = raw.getInt("amount");
                val inStorage = raw.getInt("in_storage");
                val moneyPer = raw.getDouble("money_per");

                var deliverable = orderAmount - delivered;

                for (ItemStack item : items) {
                    if (!AlgoUtils.isSimilar(item, order.getItem())) {
                        if (isShulkerBox(item) && plugin.getConfigs().isShulkerDelivering()) {
                            deliverable = scanShulkerBox(item, order.getItem(), deliverable);
                        }
                        PlayerUtils.give(deliverer, item, true);
                        continue;
                    }
                    val itemAmount = item.getAmount();
                    if (deliverable >= itemAmount) {
                        deliverable -= itemAmount;
                        continue;
                    }
                    item.setAmount(itemAmount - deliverable);
                    PlayerUtils.give(deliverer, item, true);
                    deliverable = 0;
                }
                val newDelivered = orderAmount - deliverable;
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
        for (ItemStack item : shulkerContent.contents()) {
            if (item.isEmpty()) continue;
            if (deliverable == 0) {
                declinedItems.add(item);
                continue;
            }
            if (!AlgoUtils.isSimilar(item, comparer)) {
                declinedItems.add(item);
                continue;
            }
            val itemAmount = item.getAmount();
            if (deliverable >= itemAmount) {
                deliverable -= itemAmount;
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

    private boolean isShulkerBox(ItemStack item) {
        Material type = item.getType();
        return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX ||
                type == Material.BLACK_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX ||
                type == Material.YELLOW_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX ||
                type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX ||
                type == Material.MAGENTA_SHULKER_BOX || type == Material.PINK_SHULKER_BOX;
    }


    @Override
    public CompletableFuture<Boolean> collectItems(Order order, int amount) {
        val future = new CompletableFuture<Boolean>();

        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val getOrder = connection.prepareStatement(GET_ORDER);
                    val updateOrder = connection.prepareStatement(UPDATE_ORDER);
                    val deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                val orderId = order.getId();
                connection.setAutoCommit(false);
                getOrder.setInt(1, orderId);
                val raw = getOrder.executeQuery();
                if (!raw.next()) {
                    connection.commit();
                    future.complete(false);
                    return;
                }
                val delivered = raw.getInt("delivered");
                val orderAmount = raw.getInt("amount");
                val inStorage = raw.getInt("in_storage");
                val moneyPer = raw.getDouble("money_per");
                if (inStorage < amount) {
                    connection.commit();
                    future.complete(false);
                    return;
                }
                if (inStorage - amount == 0 && (delivered == orderAmount || order.getExpiresAt() < System.currentTimeMillis())) {
                    deleteOrder.setInt(1, orderId);
                    deleteOrder.executeUpdate();
                    plugin.getDataCache().deleteOrder(order);
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
        val future = new CompletableFuture<Void>();
        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val updateOrder = connection.prepareStatement("UPDATE " + ORDER_TABLE + " SET " + var + " = ? WHERE id = ?")
            ) {
                val orderId = order.getId();
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
        val future = new CompletableFuture<Void>();
        DispatchUtil.async(() -> {
            try (
                    val connection = data.getConnection();
                    val deleteOrder = connection.prepareStatement(DELETE_ORDER)
            ) {
                deleteOrder.setInt(1, order.getId());
                deleteOrder.executeUpdate();
                plugin.getDataCache().deleteOrder(order);
                future.complete(null);
            } catch (SQLException e) {
                Log.error("Failed to delete order", e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> logTransaction(UUID player, double before, double amount, double after) {
        val future = new CompletableFuture<Void>();
        DispatchUtil.async(() -> {
           try (
                   val connection = data.getConnection();
                   val logTransaction = connection.prepareStatement(LOG_TRANSACTION)
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
    public void createTables() {
        try (
                val connection = data.getConnection();
                val createOrderTable = connection.prepareStatement(CREATE_ORDER_TABLE);
                val createTransactionTable = connection.prepareStatement(CREATE_TRANSACTION_TABLE)
        ) {
            createOrderTable.executeUpdate();
            createTransactionTable.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to create tables", e);
        }
    }


}
