package me.karven.orderium.storage.object.order;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public record OrderRow(int id, @NotNull UUID owner, byte @NotNull [] itemBytes, double moneyPer, int amount, int delivered, int inStorage, long expiresAt, int state) {

    public static @Nullable OrderRow fromSQL(final @NotNull ResultSet row) throws SQLException {
        if (!row.next()) return null;
        return new OrderRow(
                row.getInt("id"),
                new UUID(row.getLong("owner_most"), row.getLong("owner_least")),
                row.getBytes("item"),
                row.getDouble("money_per"),
                row.getInt("amount"),
                row.getInt("delivered"),
                row.getInt("in_storage"),
                row.getLong("expires_at"),
                row.getInt("state")
        );
    }

    public void toSQL(final @NotNull PreparedStatement statement) throws SQLException {
        statement.setInt(1, amount());
        statement.setDouble(2, moneyPer());
        statement.setInt(3, delivered());
        statement.setInt(4, inStorage());
        statement.setLong(5, expiresAt());
        statement.setInt(6, id());
    }
}
