package me.karven.orderium.storage.sql;


import java.sql.Connection;
import java.sql.SQLException;

public record SqlMigration(int version, String description, Action action) {

    @FunctionalInterface
    public interface Action {
        void apply(Connection connection) throws SQLException;
    }
}
