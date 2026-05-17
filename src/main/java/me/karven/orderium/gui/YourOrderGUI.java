package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static me.karven.orderium.config.ConfigCache.cache;
import static me.karven.orderium.load.Orderium.plugin;

public class YourOrderGUI {
    public static void open(Player player) {
        open(player, false);
    }

    public static void open(Player p, boolean isAsync) {
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID, isAsync);
        final MiniMessage mm = plugin.mm;
        final InventoryGUI gui = new InventoryGUI(3, mm.deserialize(cache.yoGuiTitle));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        final List<String> rawLore = cache.yoLore;
        int slot = 0;
        for (Order order : orders) {
            gui.addItem(ConvertUtils.parseOrder(order, rawLore, event -> {
                PlayerUtils.closeInv(p);
                Dialog dialog = ManageOrderDialog.getDialog(order, p);
                PlayerUtils.openDialog(p, dialog);
            }), slot++);
        }

        if (orders.size() < 27) {
            gui.addItem(ConvertUtils.parseNewButton(cache.newOrderButton, event -> {
                InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
                PlayerUtils.openGUI(p, chooseItemGUI, false);
            }), slot);
        }

        PlayerUtils.openGUI(p, gui, isAsync);
    }
}
