package me.karven.orderium.storage.sql.mysql;

import me.karven.orderium.storage.sql.SqlDatabase;
import me.karven.orderium.storage.sql.SqlItemRepository;
import me.karven.orderium.storage.sql.SqlMigration;

import java.sql.Statement;
import java.util.List;

public final class MySqlItemRepository extends SqlItemRepository {

    public MySqlItemRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public List<SqlMigration> migrations() {
        return List.of(
                new SqlMigration(1, "create the item tables", connection -> {
                    try (final Statement statement = connection.createStatement()) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + CUSTOM_ITEMS_TABLE + " (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, item MEDIUMBLOB NOT NULL, search TEXT NOT NULL) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + BLACKLIST_TABLE + " (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, item MEDIUMBLOB NOT NULL) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
                    }
                })
        );
    }
}
