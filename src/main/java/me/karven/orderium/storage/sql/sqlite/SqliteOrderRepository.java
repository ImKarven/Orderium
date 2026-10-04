package me.karven.orderium.storage.sql.sqlite;

import me.karven.orderium.order.ItemOrder;
import me.karven.orderium.storage.sql.SqlDatabase;
import me.karven.orderium.storage.sql.SqlMigration;
import me.karven.orderium.storage.sql.SqlOrderRepository;
import me.karven.orderium.utils.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class SqliteOrderRepository extends SqlOrderRepository {
    private static final String LEGACY_TABLE = "orderium_orders_legacy";

    public SqliteOrderRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public List<SqlMigration> migrations() {
        return List.of(
                new SqlMigration(1, "create the orders table and import orders from older versions", SqliteOrderRepository::createTable)
        );
    }

    private static void createTable(final Connection connection) throws SQLException {
        // Orderium 2.4 and older stored only item orders, in a table with the same name
        final boolean legacy = SqlDatabase.tableExists(connection, TABLE)
                && SqlDatabase.columnExists(connection, TABLE, "owner_most")
                && !SqlDatabase.columnExists(connection, TABLE, "type");

        try (final Statement statement = connection.createStatement()) {
            if (legacy) statement.executeUpdate("ALTER TABLE " + TABLE + " RENAME TO " + LEGACY_TABLE);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        type VARCHAR(64) NOT NULL,
                        owner_most BIGINT NOT NULL,
                        owner_least BIGINT NOT NULL,
                        data BLOB NOT NULL,
                        money_per DOUBLE NOT NULL,
                        amount INTEGER NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0,
                        in_storage INTEGER NOT NULL DEFAULT 0,
                        expires_at BIGINT NOT NULL,
                        version INTEGER NOT NULL DEFAULT 0
                    )""".formatted(TABLE));
        }

        if (!legacy) return;

        final String importOrders = """
                INSERT INTO %s (id, type, owner_most, owner_least, data, money_per, amount, delivered, in_storage, expires_at, version)
                SELECT id, ?, owner_most, owner_least, item, money_per, amount, COALESCE(delivered, 0), COALESCE(in_storage, 0), expires_at, 0
                FROM %s""".formatted(TABLE, LEGACY_TABLE);
        try (final PreparedStatement statement = connection.prepareStatement(importOrders)) {
            statement.setString(1, ItemOrder.TYPE.key());
            final int imported = statement.executeUpdate();
            Log.info("Imported " + imported + " orders from the previous storage format. The old table is kept as " + LEGACY_TABLE + " in case it's needed");
        }
    }
}
