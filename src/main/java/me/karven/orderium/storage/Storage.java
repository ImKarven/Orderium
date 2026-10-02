package me.karven.orderium.storage;

import me.karven.orderium.storage.repository.ItemRepository;
import me.karven.orderium.storage.repository.OrderRepository;
import me.karven.orderium.storage.repository.TransactionLogRepository;
import me.karven.orderium.storage.sql.*;
import me.karven.orderium.storage.sql.mysql.MySqlItemRepository;
import me.karven.orderium.storage.sql.mysql.MySqlOrderRepository;
import me.karven.orderium.storage.sql.mysql.MySqlTransactionLogRepository;
import me.karven.orderium.storage.sql.sqlite.SqliteItemRepository;
import me.karven.orderium.storage.sql.sqlite.SqliteOrderRepository;
import me.karven.orderium.storage.sql.sqlite.SqliteTransactionLogRepository;
import me.karven.orderium.utils.Log;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class Storage {
    private final StorageType type;
    private final OrderRepository orders;
    private final ItemRepository items;
    private final TransactionLogRepository transactionLog;
    private final AutoCloseable database;
    private final ExecutorService executor;

    private Storage(
            final StorageType type,
            final OrderRepository orders,
            final ItemRepository items,
            final TransactionLogRepository transactionLog,
            final AutoCloseable database,
            final int threads
    ) {
        this.type = type;
        this.orders = orders;
        this.items = items;
        this.transactionLog = transactionLog;
        this.database = database;

        final AtomicInteger threadCount = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(threads, runnable -> {
            final Thread thread = new Thread(runnable, "Orderium Storage #" + threadCount.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public static Storage open(final StorageSettings settings, final File dataFolder) {
        return switch (settings.type()) {
            case SQLITE -> {
                final SqlDatabase database = SqlDatabase.sqlite(new File(dataFolder, "data.db"));
                yield openSql(settings.type(), database, 1,
                        new SqliteOrderRepository(database),
                        new SqliteItemRepository(database, new File(dataFolder, "modified_items.db")),
                        new SqliteTransactionLogRepository(database)
                );
            }
            case MYSQL -> {
                final SqlDatabase database = SqlDatabase.mysql(settings.mysql(), settings.pool());
                yield openSql(settings.type(), database, Math.max(1, settings.pool().maximumPoolSize()),
                        new MySqlOrderRepository(database),
                        new MySqlItemRepository(database),
                        new MySqlTransactionLogRepository(database)
                );
            }
        };
    }

    private static Storage openSql(
            final StorageType type,
            final SqlDatabase database,
            final int threads,
            final SqlOrderRepository orders,
            final SqlItemRepository items,
            final SqlTransactionLogRepository transactionLog
    ) {
        try {
            new SqlSchemaMigrator(database).migrate(List.of(orders, items, transactionLog));
        } catch (RuntimeException e) {
            database.close();
            throw e;
        }
        return new Storage(type, orders, items, transactionLog, database, threads);
    }

    public StorageType type() {
        return type;
    }

    public OrderRepository orders() {
        return orders;
    }

    public ItemRepository items() {
        return items;
    }

    public TransactionLogRepository transactionLog() {
        return transactionLog;
    }

    public <T> CompletableFuture<T> supplyAsync(final String action, final Supplier<T> task) {
        final CompletableFuture<T> future;
        try {
            future = CompletableFuture.supplyAsync(task, executor);
        } catch (RejectedExecutionException e) {
            Log.error("Failed to " + action + ", the storage is closed", e);
            return CompletableFuture.failedFuture(e);
        }
        return future.whenComplete((_, exception) -> {
            if (exception != null) Log.error("Failed to " + action, exception instanceof CompletionException && exception.getCause() != null ? exception.getCause() : exception);
        });
    }

    public CompletableFuture<Void> runAsync(final String action, final Runnable task) {
        return supplyAsync(action, () -> {
            task.run();
            return null;
        });
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                Log.warn("Storage tasks did not finish in time, some changes may be lost");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            database.close();
        } catch (Exception e) {
            Log.error("Failed to close the database", e);
        }
    }
}
