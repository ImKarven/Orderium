package me.karven.orderium.obj;

import me.karven.orderium.config.Config;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class MoneyTransaction {
    public final Config config = Config.config;
    public final OfflinePlayer player;
    public double before;
    public double amount;
    public double after;

    public MoneyTransaction(final @NotNull OfflinePlayer player) {
        this.player = player;
    }
}
