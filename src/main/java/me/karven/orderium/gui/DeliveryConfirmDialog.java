package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.listener.DialogListener;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class DeliveryConfirmDialog {
    public static Dialog getDialog(Player p, Order order, int amount, double reward, Collection<ItemStack> items) {
        final Config config = Config.config;
        final Dialog dialog = config.confirmDeliveryDialogConfig.dialog(
                order.mainGUIItemStack(),
                ConvertUtils.formatNumber(amount),
                ConvertUtils.formatNumber(reward),
                (_, _) -> {
                    DialogListener.removeItems(p);
                    order.deliver(p, items, false);
                },
                (_, audience) -> {
                    if (!(audience instanceof final Player player)) return;
                    DialogListener.onCancel(player);
                }
        );
        DialogListener.addItems(p, items);

        return dialog;
    }
}
