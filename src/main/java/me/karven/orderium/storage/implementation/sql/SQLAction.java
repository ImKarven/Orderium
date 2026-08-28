package me.karven.orderium.storage.implementation.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.karven.orderium.storage.object.RetryOperationException;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.Log;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public record SQLAction<T>(
        @NotNull HikariDataSource dataSource,
        @NotNull Function<Connection, T> action
) {

    public CompletableFuture<T> execute() {
        final CompletableFuture<T> future = new CompletableFuture<>();
        final AtomicInteger retryAttempt = new AtomicInteger(1);

        // Use array as a hack to reference the task before initialization
        final Runnable[] taskSingleton = new Runnable[1];
        taskSingleton[0] = () -> {
            try (final Connection connection = dataSource.getConnection()) {
                try {
                    connection.setAutoCommit(false);
                    final T result = action.apply(connection);
                    future.complete(result);
                    connection.commit();
                } catch (final RetryOperationException retryOperationException) {
                    if (retryAttempt.get() >= 5) {
                        future.completeExceptionally(retryOperationException);
                        return;
                    }
                    retryAttempt.incrementAndGet();

                    taskSingleton[0].run();
                } catch (final RuntimeException exception) {
                    connection.rollback();
                    Log.error("Failed to execute SQL operation", exception);
                    future.completeExceptionally(exception);
                }
            } catch (final SQLException exception) {
                Log.error("Failed to execute SQL operation", exception);
                future.completeExceptionally(exception);
            }
        };

        DispatchUtil.async(taskSingleton[0]);

        return future;
    }
}
