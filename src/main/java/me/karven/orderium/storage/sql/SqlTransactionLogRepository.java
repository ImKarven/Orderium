package me.karven.orderium.storage.sql;

import me.karven.orderium.storage.repository.TransactionLogRepository;

import java.sql.PreparedStatement;
import java.util.UUID;

public abstract class SqlTransactionLogRepository extends SqlRepository implements TransactionLogRepository {
    protected static final String TABLE = "orderium_transactions_v2";

    protected SqlTransactionLogRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public final String schemaName() {
        return "transactions";
    }

    @Override
    public void log(final UUID player, final long time, final double before, final double amount, final double after) {
        database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TABLE + " (time, player_most, player_least, `before`, amount, `after`) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setLong(1, time);
                statement.setLong(2, player.getMostSignificantBits());
                statement.setLong(3, player.getLeastSignificantBits());
                statement.setDouble(4, before);
                statement.setDouble(5, amount);
                statement.setDouble(6, after);
                return statement.executeUpdate();
            }
        });
    }
}
