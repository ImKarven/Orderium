package me.karven.orderium.storage.sql;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.karven.orderium.storage.StorageException;
import me.karven.orderium.storage.StorageSettings;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SqlDatabase implements AutoCloseable {
    private final HikariDataSource dataSource;

    private SqlDatabase(final HikariConfig config) {
        this.dataSource = new HikariDataSource(config);
    }

    public static SqlDatabase sqlite(final File file) {
        final HikariConfig config = new HikariConfig();
        config.setPoolName("Orderium SQLite");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        // SQLite only allows one writer at a time, a single connection avoids "database is locked" errors
        config.setMaximumPoolSize(1);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        return new SqlDatabase(config);
    }

    public static SqlDatabase mysql(final StorageSettings.MySql mysql, final StorageSettings.Pool pool) {
        final HikariConfig config = new HikariConfig();
        config.setPoolName("Orderium MySQL");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database());
        config.setUsername(mysql.username());
        config.setPassword(mysql.password());
        config.setMaximumPoolSize(pool.maximumPoolSize());
        config.setMinimumIdle(pool.minimumIdle());
        config.setMaxLifetime(pool.maxLifetime());
        config.setConnectionTimeout(pool.connectionTimeout());

        // Just a few properties which are recommended by HikariCP for MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("characterEncoding", "utf8");

        for (final String property : mysql.properties()) {
            final int separator = property.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid MySQL property '" + property + "' in storage.yml, use key=value");
            }
            config.addDataSourceProperty(property.substring(0, separator).trim(), property.substring(separator + 1).trim());
        }
        return new SqlDatabase(config);
    }

    public <T> T withConnection(final SqlFunction<T> action) {
        try (final Connection connection = dataSource.getConnection()) {
            return action.apply(connection);
        } catch (SQLException e) {
            throw new StorageException("Database operation failed", e);
        }
    }

    @CanIgnoreReturnValue
    public <T> T inTransaction(final SqlFunction<T> action) {
        return withConnection(connection -> {
            connection.setAutoCommit(false);
            try {
                final T result = action.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    @Override
    public void close() {
        dataSource.close();
    }

    public static boolean tableExists(final Connection connection, final String table) throws SQLException {
        final DatabaseMetaData metaData = connection.getMetaData();
        try (final ResultSet tables = metaData.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
            }
            return false;
        }
    }

    public static boolean columnExists(final Connection connection, final String table, final String column) throws SQLException {
        final DatabaseMetaData metaData = connection.getMetaData();
        try (final ResultSet columns = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            while (columns.next()) {
                if (table.equalsIgnoreCase(columns.getString("TABLE_NAME")) && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
            return false;
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
