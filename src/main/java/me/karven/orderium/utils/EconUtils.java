package me.karven.orderium.utils;

import me.karven.orderium.obj.MoneyTransaction;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import static me.karven.orderium.Orderium.plugin;

public class EconUtils {
    public static void addMoney(OfflinePlayer p, double amount) {
        final MoneyTransaction transaction = logTransactionBefore(p, amount);
        plugin.getEconomy().depositPlayer(p, amount);
        logTransactionAfter(transaction);
    }

    /// Returns `true` if the player has enough money to remove, otherwise `false`
    public static boolean removeMoney(Player p, double amount) {
        if (plugin.getEconomy().getBalance(p) < amount) return false;
        final MoneyTransaction transaction = logTransactionBefore(p, amount);
        plugin.getEconomy().withdrawPlayer(p, amount);
        logTransactionAfter(transaction);
        return true;
    }

    private static MoneyTransaction logTransactionBefore(OfflinePlayer p, double amount) {
        final MoneyTransaction transaction = new MoneyTransaction(p);
        if (!transaction.config.logTransactions) return transaction;
        transaction.before = plugin.getEconomy().getBalance(p);
        transaction.amount = amount;
        return transaction;
    }

    private static void logTransactionAfter(final MoneyTransaction transaction) {
        if (!transaction.config.logTransactions) return;
        transaction.after = plugin.getEconomy().getBalance(transaction.player);
        plugin.getStorage().logTransaction(transaction.player.getUniqueId(), transaction.before, transaction.amount, transaction.after);
    }
}