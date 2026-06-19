package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static me.karven.orderium.Orderium.plugin;

public class YourOrderGUI {
    public static void open(Player player) {
        open(player, false);
    }

    public static void open(Player p, boolean isAsync) {
        final Config config = Config.config;
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID, isAsync);
        final MiniMessage mm = plugin.mm;
        final InventoryGUI gui = new InventoryGUI(config.yourOrdersGUIConfig.rows, mm.deserialize(config.yourOrdersGUIConfig.title));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        final List<String> rawLore = config.yourOrdersGUIConfig.orderConfig.lore;
        int currentSlotIndex = 0;
        for (Order order : orders) {
            gui.addItem(order.item(rawLore, event -> {
                Dialog dialog = ManageOrderDialog.getDialog(order);
                PlayerUtils.openDialog(p, dialog);
            }), config.yourOrdersGUIConfig.orderConfig.slots.get(currentSlotIndex++));
        }

        if (orders.size() < config.yourOrdersGUIConfig.rows * 9) {
            gui.addItem(
                    config.yourOrdersGUIConfig.newOrderButton.item(event -> {
                        InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
                        PlayerUtils.openGUI(p, chooseItemGUI, false);
                    }), config.yourOrdersGUIConfig.newOrderButton.slot
            );
        }

        PlayerUtils.openGUI(p, gui, isAsync);
    }
}
