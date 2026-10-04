package me.karven.orderium.storage.sql;

import me.karven.orderium.order.Order;
import me.karven.orderium.order.OrderDraft;
import me.karven.orderium.order.OrderState;
import me.karven.orderium.order.OrderType;
import me.karven.orderium.order.OrderTypes;
import me.karven.orderium.storage.StorageException;
import me.karven.orderium.storage.repository.OrderRepository;
import me.karven.orderium.utils.Log;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class SqlOrderRepository extends SqlRepository implements OrderRepository {
    protected static final String TABLE = "orderium_orders";

    private static final String STATE_COLUMNS = "money_per, amount, delivered, in_storage, expires_at, version";
    private static final String SELECT_ALL = "SELECT id, type, owner_most, owner_least, data, " + STATE_COLUMNS + " FROM " + TABLE;
    private static final String SELECT_STATE = "SELECT " + STATE_COLUMNS + " FROM " + TABLE + " WHERE id = ?";
    private static final String INSERT = "INSERT INTO " + TABLE + " (type, owner_most, owner_least, data, " + STATE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE " + TABLE + " SET money_per = ?, amount = ?, delivered = ?, in_storage = ?, expires_at = ?, version = ? WHERE id = ? AND version = ?";
    private static final String DELETE = "DELETE FROM " + TABLE + " WHERE id = ? AND version = ?";

    protected SqlOrderRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public final String schemaName() {
        return "orders";
    }

    @Override
    public List<Order> findAll() {
        return database.withConnection(connection -> {
            try (
                    final PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
                    final ResultSet result = statement.executeQuery()
            ) {
                final List<Order> orders = new ArrayList<>();
                while (result.next()) {
                    final int id = result.getInt("id");
                    final String typeKey = result.getString("type");
                    final OrderType<?> type = OrderTypes.get(typeKey);
                    if (type == null) {
                        Log.warn("Skipping order #" + id + " of unknown type '" + typeKey + "'");
                        continue;
                    }

                    final UUID owner = new UUID(result.getLong("owner_most"), result.getLong("owner_least"));
                    try {
                        orders.add(type.decode(id, owner, result.getBytes("data"), readState(result)));
                    } catch (RuntimeException e) {
                        Log.error("Skipping order #" + id + ", its data could not be read", e);
                    }
                }
                return orders;
            }
        });
    }

    @Override
    public @Nullable OrderState findState(final int id) {
        return database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(SELECT_STATE)) {
                statement.setInt(1, id);
                try (final ResultSet result = statement.executeQuery()) {
                    return result.next() ? readState(result) : null;
                }
            }
        });
    }

    @Override
    public <O extends Order> O insert(final OrderDraft<O> draft) {
        final int id = database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                final OrderState state = draft.state();
                statement.setString(1, draft.type().key());
                statement.setLong(2, draft.owner().getMostSignificantBits());
                statement.setLong(3, draft.owner().getLeastSignificantBits());
                statement.setBytes(4, draft.data());
                writeState(statement, 5, state, state.version());
                statement.executeUpdate();

                try (final ResultSet generated = statement.getGeneratedKeys()) {
                    if (!generated.next()) throw new StorageException("The database returned no id for the new order");
                    return generated.getInt(1);
                }
            }
        });
        return draft.toOrder(id);
    }

    @Override
    public @Nullable OrderState update(final int id, final OrderState expected, final OrderState updated) {
        final int newVersion = expected.version() + 1;
        final boolean changed = database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                writeState(statement, 1, updated, newVersion);
                statement.setInt(7, id);
                statement.setInt(8, expected.version());
                return statement.executeUpdate() > 0;
            }
        });
        return changed ? updated.withVersion(newVersion) : null;
    }

    @Override
    public boolean delete(final int id, final OrderState expected) {
        return database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(DELETE)) {
                statement.setInt(1, id);
                statement.setInt(2, expected.version());
                return statement.executeUpdate() > 0;
            }
        });
    }

    private static OrderState readState(final ResultSet result) throws SQLException {
        return new OrderState(
                result.getDouble("money_per"),
                result.getInt("amount"),
                result.getInt("delivered"),
                result.getInt("in_storage"),
                result.getLong("expires_at"),
                result.getInt("version")
        );
    }

    // Writes in the order of STATE_COLUMNS, starting at `index`
    private static void writeState(final PreparedStatement statement, final int index, final OrderState state, final int version) throws SQLException {
        statement.setDouble(index, state.moneyPer());
        statement.setInt(index + 1, state.amount());
        statement.setInt(index + 2, state.delivered());
        statement.setInt(index + 3, state.inStorage());
        statement.setLong(index + 4, state.expiresAt());
        statement.setInt(index + 5, version);
    }
}
