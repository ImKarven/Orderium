package me.karven.orderium.storage.sql;

import me.karven.orderium.utils.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class SqlSchemaMigrator {
    private static final String SCHEMA_TABLE = "orderium_schema";

    private final SqlDatabase database;

    public SqlSchemaMigrator(final SqlDatabase database) {
        this.database = database;
    }

    // MySQL commits schema changes immediately instead of in the transaction, so its migrations must be safe to run again
    public void migrate(final List<? extends SqlRepository> repositories) {
        database.inTransaction(connection -> {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + SCHEMA_TABLE + " (name VARCHAR(64) NOT NULL PRIMARY KEY, version INT NOT NULL)");
            }

            for (final SqlRepository repository : repositories) {
                final String name = repository.schemaName();
                int version = currentVersion(connection, name);
                for (final SqlMigration migration : repository.migrations()) {
                    if (migration.version() <= version) continue;

                    Log.info("Updating " + name + " storage to version " + migration.version() + ": " + migration.description());
                    migration.action().apply(connection);
                    setVersion(connection, name, version, migration.version());
                    version = migration.version();
                }
            }
            return null;
        });
    }

    private static int currentVersion(final Connection connection, final String name) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement("SELECT version FROM " + SCHEMA_TABLE + " WHERE name = ?")) {
            statement.setString(1, name);
            try (final ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static void setVersion(final Connection connection, final String name, final int from, final int to) throws SQLException {
        final String sql = from == 0
                ? "INSERT INTO " + SCHEMA_TABLE + " (version, name) VALUES (?, ?)"
                : "UPDATE " + SCHEMA_TABLE + " SET version = ? WHERE name = ?";
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, to);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }
}
