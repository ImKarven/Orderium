package me.karven.orderium.storage.object;

import me.karven.orderium.obj.StorageMethod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record Migration(String name, Consumer<Connection> action) {
    public List<Migration> getMigrations(final StorageMethod method) {
        final List<Migration> result = new ArrayList<>();
        switch (method) {
            case SQLITE -> {
                result.add(new Migration("Add state for modifying orders", connection -> {
                    try (final PreparedStatement statement = connection.prepareStatement("ALTER TABLE orderium_orders ADD COLUMN state INTEGER NOT NULL DEFAULT 0")) {
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
        }

        return result;
    }
}
