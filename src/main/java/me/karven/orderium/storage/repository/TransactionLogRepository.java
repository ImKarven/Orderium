package me.karven.orderium.storage.repository;


import java.util.UUID;

public interface TransactionLogRepository {

    void log(UUID player, long time, double before, double amount, double after);
}
