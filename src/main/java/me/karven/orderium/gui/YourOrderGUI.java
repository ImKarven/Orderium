package me.karven.orderium.gui;

import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static me.karven.orderium.load.Orderium.plugin;

public class YourOrderGUI {
    public static void open(Player player) {
        open(player, false);
    }

    public static void open(Player p, boolean isAsync) {
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID, isAsync);
        final ConfigCache cache = plugin.getConfigs();
        final MiniMessage mm = plugin.mm;
        final InventoryGUI gui = new InventoryGUI(3, mm.deserialize(cache.getYoGuiTitle()));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        final List<String> rawLore = cache.getYoLore();
        int slot = 0;
        for (Order order : orders) {
            gui.addItem(ConvertUtils.parseOrder(order, rawLore, _ -> {
                PlayerUtils.closeInv(p);
                ManageOrderDialog.show(order, p);
            }), slot++);
        }

        if (orders.size() < 27) {
            gui.addItem(ConvertUtils.parseNewButton(cache.getNewOrderButton(), _ -> NewOrderDialog.start(p)), slot);
        }

        PlayerUtils.openGUI(p, gui, isAsync);
    }
}
