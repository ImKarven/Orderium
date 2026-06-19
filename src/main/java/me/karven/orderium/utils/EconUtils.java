package me.karven.orderium.utils;

import me.karven.orderium.config.Config;
import me.karven.orderium.obj.MoneyTransaction;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import static me.karven.orderium.Orderium.plugin;

public class EconUtils {
    private static final MoneyTransaction currentTransaction = new MoneyTransaction();
    private static Config config;

    public static void addMoney(OfflinePlayer p, double amount) {
        logTransactionBefore(p, amount);
        plugin.getEconomy().depositPlayer(p, amount);
        logTransactionAfter(p);
    }

    /// Returns `true` if the player has enough money to remove, otherwise `false`
    public static boolean removeMoney(Player p, double amount) {
        if (plugin.getEconomy().getBalance(p) < amount) return false;
        logTransactionBefore(p, amount);
        plugin.getEconomy().withdrawPlayer(p, amount);
        logTransactionAfter(p);
        return true;
    }

    private static void logTransactionBefore(OfflinePlayer p, double amount) {
        config = Config.config;
        if (!config.logTransactions) return;
        currentTransaction.player = p.getUniqueId();
        currentTransaction.before = plugin.getEconomy().getBalance(p);
        currentTransaction.amount = amount;
    }

    private static void logTransactionAfter(OfflinePlayer p) {
        if (!config.logTransactions) return;
        currentTransaction.after = plugin.getEconomy().getBalance(p);
        plugin.getStorage().logTransaction(currentTransaction.player, currentTransaction.before, currentTransaction.amount, currentTransaction.after);
    }
}