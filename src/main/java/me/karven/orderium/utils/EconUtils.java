package me.karven.orderium.utils;

import lombok.Getter;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.MoneyTransaction;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
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

    public static void addMoney(OfflinePlayer p, double amount) {
        logTransactionBefore(p, amount);
        eco.depositPlayer(p, amount);
        logTransactionAfter(p);
    }

    /// Returns `true` if the player has enough money to remove, otherwise `false`
    public static boolean removeMoney(Player p, double amount) {
        if (eco.getBalance(p) < amount) return false;
        logTransactionBefore(p, amount);
        eco.withdrawPlayer(p, amount);
        logTransactionAfter(p);
        return true;
    }

    private static void logTransactionBefore(OfflinePlayer p, double amount) {
        if (!cache.isLogTransactions()) return;
        currentTransaction.player = p.getUniqueId();
        currentTransaction.before = eco.getBalance(p);
        currentTransaction.amount = amount;
    }

    private static void logTransactionAfter(OfflinePlayer p) {
        if (!cache.isLogTransactions()) return;
        currentTransaction.after = eco.getBalance(p);
        db.logTransaction();
    }
}