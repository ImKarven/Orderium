package me.karven.orderium.storage.sql.mysql;

import me.karven.orderium.storage.sql.SqlDatabase;
import me.karven.orderium.storage.sql.SqlMigration;
import me.karven.orderium.storage.sql.SqlTransactionLogRepository;

import java.sql.Statement;
import java.util.List;

public final class MySqlTransactionLogRepository extends SqlTransactionLogRepository {

    public MySqlTransactionLogRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public List<SqlMigration> migrations() {
        return List.of(
                new SqlMigration(1, "create the transaction log table", connection -> {
                    try (final Statement statement = connection.createStatement()) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, time BIGINT NOT NULL, player_most BIGINT NOT NULL, player_least BIGINT NOT NULL, `before` DOUBLE NOT NULL, amount DOUBLE NOT NULL, `after` DOUBLE NOT NULL) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
                    }
                })
        );
    }
}
