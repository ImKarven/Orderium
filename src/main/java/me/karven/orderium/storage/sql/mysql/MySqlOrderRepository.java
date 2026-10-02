package me.karven.orderium.storage.sql.mysql;

import me.karven.orderium.storage.sql.SqlDatabase;
import me.karven.orderium.storage.sql.SqlMigration;
import me.karven.orderium.storage.sql.SqlOrderRepository;

import java.sql.Statement;
import java.util.List;

public final class MySqlOrderRepository extends SqlOrderRepository {

    public MySqlOrderRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public List<SqlMigration> migrations() {
        return List.of(
                new SqlMigration(1, "create the orders table", connection -> {
                    try (final Statement statement = connection.createStatement()) {
                        statement.executeUpdate("""
                                CREATE TABLE IF NOT EXISTS %s (
                                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                    type VARCHAR(64) NOT NULL,
                                    owner_most BIGINT NOT NULL,
                                    owner_least BIGINT NOT NULL,
                                    data MEDIUMBLOB NOT NULL,
                                    money_per DOUBLE NOT NULL,
                                    amount INT NOT NULL,
                                    delivered INT NOT NULL DEFAULT 0,
                                    in_storage INT NOT NULL DEFAULT 0,
                                    expires_at BIGINT NOT NULL,
                                    version INT NOT NULL DEFAULT 0
                                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4""".formatted(TABLE));
                    }
                })
        );
    }
}
