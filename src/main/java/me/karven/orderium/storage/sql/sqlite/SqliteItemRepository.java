package me.karven.orderium.storage.sql.sqlite;

import me.karven.orderium.storage.sql.SqlDatabase;
import me.karven.orderium.storage.sql.SqlItemRepository;
import me.karven.orderium.storage.sql.SqlMigration;
import me.karven.orderium.utils.Log;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public final class SqliteItemRepository extends SqlItemRepository {
    // Orderium 2.4 and older kept custom and blacklisted items in this separate file
    private final File legacyFile;

    public SqliteItemRepository(final SqlDatabase database, final File legacyFile) {
        super(database);
        this.legacyFile = legacyFile;
    }

    @Override
    public List<SqlMigration> migrations() {
        return List.of(
                new SqlMigration(1, "create the item tables and import items from older versions", this::createTables)
        );
    }

    private void createTables(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + CUSTOM_ITEMS_TABLE + " (item BLOB NOT NULL, search TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + BLACKLIST_TABLE + " (item BLOB NOT NULL)");
        }

        if (legacyFile.isFile()) importLegacyItems(connection);
    }

    private void importLegacyItems(final Connection connection) throws SQLException {
        final Driver driver;
        try {
            // Not through the pool: SQLite can't ATTACH another database inside the migration transaction
            driver = (Driver) Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SQLException("The SQLite driver is not available", e);
        }

        int customItems = 0;
        int blacklistedItems = 0;
        try (final Connection legacy = driver.connect("jdbc:sqlite:" + legacyFile.getAbsolutePath(), new Properties())) {
            if (SqlDatabase.tableExists(legacy, CUSTOM_ITEMS_TABLE)) {
                try (
                        final PreparedStatement read = legacy.prepareStatement("SELECT item, search FROM " + CUSTOM_ITEMS_TABLE);
                        final ResultSet items = read.executeQuery();
                        final PreparedStatement write = connection.prepareStatement("INSERT INTO " + CUSTOM_ITEMS_TABLE + " (item, search) VALUES (?, ?)")
                ) {
                    while (items.next()) {
                        final byte[] item = items.getBytes("item");
                        if (item == null) continue;
                        final String search = items.getString("search");
                        write.setBytes(1, item);
                        write.setString(2, search == null ? "" : search);
                        write.executeUpdate();
                        customItems++;
                    }
                }
            }

            if (SqlDatabase.tableExists(legacy, BLACKLIST_TABLE)) {
                try (
                        final PreparedStatement read = legacy.prepareStatement("SELECT item FROM " + BLACKLIST_TABLE);
                        final ResultSet items = read.executeQuery();
                        final PreparedStatement write = connection.prepareStatement("INSERT INTO " + BLACKLIST_TABLE + " (item) VALUES (?)")
                ) {
                    while (items.next()) {
                        final byte[] item = items.getBytes("item");
                        if (item == null) continue;
                        write.setBytes(1, item);
                        write.executeUpdate();
                        blacklistedItems++;
                    }
                }
            }
        }

        Log.info("Imported " + customItems + " custom items and " + blacklistedItems + " blacklisted items from " + legacyFile.getName() + ". The file is no longer used and can be deleted");
    }
}
