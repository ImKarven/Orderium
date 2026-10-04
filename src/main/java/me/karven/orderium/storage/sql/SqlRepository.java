package me.karven.orderium.storage.sql;


import java.util.List;

public abstract class SqlRepository {
    protected final SqlDatabase database;

    protected SqlRepository(final SqlDatabase database) {
        this.database = database;
    }

    // The schema version is tracked under this name, must never change once released
    public abstract String schemaName();

    public abstract List<SqlMigration> migrations();
}
