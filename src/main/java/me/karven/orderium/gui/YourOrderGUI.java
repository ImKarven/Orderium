package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class YourOrderGUI {
    private static Orderium plugin;

    public static void init(Orderium plugin) {
        YourOrderGUI.plugin = plugin;
    }
    public static void open(Player p) {
        final DBManager db = plugin.getDbManager();
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = db.getOrders(pUUID);
        final ConfigManager cache = plugin.getConfigs();
        final MiniMessage mm = plugin.mm;
        final ChestGui gui = new ChestGui(3, ComponentHolder.of(mm.deserialize(cache.getYoGuiTitle())));
        gui.setOnGlobalClick(e -> e.setCancelled(true));
        gui.setOnGlobalDrag(e -> e.setCancelled(true));
        final OutlinePane ordersPane = new OutlinePane(0, 0, 9, 3);
        final List<String> rawLore = cache.getYoLore();
        for (Order order : orders) {
            ordersPane.addItem(ConvertUtils.parseOrder(order, rawLore, e -> {
                e.setCancelled(true);
                p.closeInventory();
                ManageOrderDialog.show(order, p);
            }));
        }

        if (orders.size() < 27) {
            ordersPane.addItem(ConvertUtils.parseButton(cache.getNewOrderButton(), e -> {
                e.setCancelled(true);
                NewOrderDialog.start(p);
            }));
        }

        gui.addPane(ordersPane);
        gui.show(p);
    }
}
