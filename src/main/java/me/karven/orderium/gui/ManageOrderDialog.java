package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static me.karven.orderium.utils.ConvertUtils.formatNumber;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialog {
    public static Dialog getDialog(Order order) {
        final Config config = Config.config;
        final ItemStack item = order.itemStack(config.yourOrdersGUIConfig.orderConfig.lore);
        final Dialog collectItemsDialog = config.manageOrderDialogConfig.collectItems.dialog(
                item,
                order.placeholders(),
                (view, audience) -> {
                    if (!(audience instanceof Player player)) return;

                    final String rawAmount = view.getText("amount");
                    order.collect(rawAmount);
                    player.closeInventory();
                },
                null
        );

        final Dialog cancelOrderDialog = config.manageOrderDialogConfig.cancelOrder.dialog(
                item,
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    order.cancel(p);
                },
                (view, audience) -> {
                    if (!(audience instanceof Player player)) return;
                    YourOrderGUI.open(player);
                },
                Placeholder.unparsed("earn", formatNumber(order.getMoneyPer() * (order.getAmount() - order.getDelivered())))
        );

        return config.manageOrderDialogConfig.manageOrder.dialog(
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    PlayerUtils.openDialog(p, collectItemsDialog);
                },
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    PlayerUtils.openDialog(p, cancelOrderDialog);
                }
        );
    }
}
