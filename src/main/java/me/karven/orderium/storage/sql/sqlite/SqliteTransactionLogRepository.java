package me.karven.orderium.storage.sql.sqlite;

import me.karven.orderium.storage.sql.SqlDatabase;
import me.karven.orderium.storage.sql.SqlMigration;
import me.karven.orderium.storage.sql.SqlTransactionLogRepository;

import java.sql.Statement;
import java.util.List;

public final class SqliteTransactionLogRepository extends SqlTransactionLogRepository {

    public SqliteTransactionLogRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public List<SqlMigration> migrations() {
        // Same layout as in older versions, so an existing table is simply kept
        return List.of(
                new SqlMigration(1, "create the transaction log table", connection -> {
                    try (final Statement statement = connection.createStatement()) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (id INTEGER PRIMARY KEY, time BIGINT, player_most BIGINT, player_least BIGINT, `before` DOUBLE, amount DOUBLE, `after` DOUBLE)");
                    }
                })
        );
    }
}
