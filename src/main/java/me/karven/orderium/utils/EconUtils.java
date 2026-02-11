package me.karven.orderium.utils;

import lombok.Getter;
import lombok.val;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.MoneyTransaction;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

public class EconUtils {
    private static Economy eco;
    @Getter
    private static final MoneyTransaction currentTransaction = new MoneyTransaction();
    private static DBManager db;
    private static ConfigManager cache;

    public static void init(Orderium plugin) {
        eco = plugin.getEcon();
        db = plugin.getDbManager();
        cache = plugin.getConfigs();
    }

    public static void addMoney(Player p, double amount) {
        logTransactionBefore(p, amount);
        eco.depositPlayer(p, amount);
        logTransactionAfter(p);
    }

    public static void removeMoney(Player p, double amount) {
        logTransactionBefore(p, amount);
        eco.withdrawPlayer(p, amount);
        logTransactionAfter(p);
    }

    private static void logTransactionBefore(Player p, double amount) {
        if (!cache.isLogTransactions()) return;
        currentTransaction.player = p.getUniqueId();
        currentTransaction.before = eco.getBalance(p);
        currentTransaction.amount = amount;
    }

    private static void logTransactionAfter(Player p) {
        if (!cache.isLogTransactions()) return;
        currentTransaction.after = eco.getBalance(p);
        db.logTransaction();
    }
}