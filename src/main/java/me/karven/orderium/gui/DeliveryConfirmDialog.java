package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.listener.DialogListener;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

import static me.karven.orderium.config.Config.config;

public class DeliveryConfirmDialog {
    public static Dialog getDialog(Player p, Order order, int amount, Collection<ItemStack> items) {
        final String amountText = ConvertUtils.formatNumber(amount);
        final Dialog dialog = config.confirmDeliveryDialogConfig.dialog(
                ConvertUtils.parseOrder(order, config.mainGUIConfig.orderConfig.lore),
                amountText,
                (view, audience) -> {
                    DialogListener.removeItems(p);
                    order.deliver(p, items, false);
                },
                (view, audience) -> {
                    if (!(audience instanceof final Player player)) return;
                    DialogListener.onCancel(player);
                }
        );
        DialogListener.addItems(p, items);

        return dialog;
    }
}
