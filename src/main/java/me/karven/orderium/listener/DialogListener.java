package me.karven.orderium.listener;

import me.karven.orderium.obj.PendingDelivery;
import me.karven.orderium.utils.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

public class DialogListener {
    // TODO: probably should replace `Player` with `UUID`.
    private static final ConcurrentHashMap<Player, PendingDelivery> pendingDeliveries = new ConcurrentHashMap<>();

    // Registers the delivery of a newly opened confirm dialog. A previous one that was never answered is handed back.
    public static void addDelivery(Player p, PendingDelivery delivery) {
        final PendingDelivery previous = pendingDeliveries.put(p, delivery);
        if (previous != null) giveBack(p, previous);
    }

    // Returns true if the items may be delivered, and false if they were already delivered or handed back
    public static boolean confirm(Player p, PendingDelivery delivery) {
        pendingDeliveries.remove(p, delivery);
        return delivery.claim();
    }

    public static void cancel(Player p, PendingDelivery delivery) {
        pendingDeliveries.remove(p, delivery);
        giveBack(p, delivery);
    }

    public static void onCancel(Player p) {
        final PendingDelivery delivery = pendingDeliveries.remove(p);
        if (delivery != null) giveBack(p, delivery);
    }

    private static void giveBack(Player p, PendingDelivery delivery) {
        if (delivery.claim()) PlayerUtils.give(p, delivery.items(), false);
    }
}
