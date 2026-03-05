package me.karven.orderium.listener;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import lombok.val;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;

@SuppressWarnings("UnstableApiUsage")
public class DialogListener implements Listener {

    private static final HashMap<Player, Collection<ItemStack>> pendingItems = new HashMap<>();

    @EventHandler
    public void onCancelDelivery(PlayerCustomClickEvent e) {
        if (!checkClick(e, "orderium:confirm_delivery/cancel")) return;

        if (!(e.getCommonConnection() instanceof PlayerGameConnection con)) return;
        val p = con.getPlayer();
        onCancel(p);
    }

    public static HashMap<Player, Collection<ItemStack>> pendingItems() {
        return pendingItems;
    }

    public static void addItems(Player p, Collection<ItemStack> items) {
        pendingItems.put(p, items);
    }

    public static void removeItems(Player p) { pendingItems.remove(p); }

    private boolean checkClick(PlayerCustomClickEvent e, @KeyPattern String key) {
        return e.getIdentifier().equals(Key.key(key));
    }

    public static void onCancel(Player p) {
        val items = pendingItems.get(p);
        if (items == null) return;
        PlayerUtils.give(p, items, false);
        pendingItems.remove(p);
    }
}
