package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static me.karven.orderium.load.Orderium.plugin;

public class YourOrderGUI {
    public static void open(Player p) {
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID);
        final ConfigCache cache = plugin.getConfigs();
        final MiniMessage mm = plugin.mm;
        final ChestGui gui = new ChestGui(3, ComponentHolder.of(mm.deserialize(cache.getYoGuiTitle())));
        gui.setOnGlobalClick(e -> e.setCancelled(true));
        gui.setOnGlobalDrag(e -> e.setCancelled(true));
        final OutlinePane ordersPane = new OutlinePane(9, 3);
        final List<String> rawLore = cache.getYoLore();
        for (Order order : orders) {
            ordersPane.addItem(ConvertUtils.parseOrder(order, rawLore, e -> {
                PlayerUtils.closeInv(p);
                ManageOrderDialog.show(order, p);
            }));
        }

        if (orders.size() < 27) {
            ordersPane.addItem(ConvertUtils.parseButton(cache.getNewOrderButton(), e -> {
                NewOrderDialog.start(p);
            }));
        }

        gui.addPane(Slot.fromXY(0, 0), ordersPane);
        PlayerUtils.openGui(p, gui);
    }
}
