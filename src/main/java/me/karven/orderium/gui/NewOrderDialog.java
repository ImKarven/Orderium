package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.SearchableItem;
import me.karven.orderium.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static me.karven.orderium.config.Config.config;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialog {
    public static Dialog getDialog(OrderItem orderItem) {
        final ItemStack item = orderItem instanceof SearchableItem searchableItem ? searchableItem.getParsedItemStack() : orderItem.getItemStack();

        return config.newOrderDialogConfig.dialog(
                item,
                (view, player) -> {
                    if (!(player instanceof Player p)) {
                        return;
                    }

                    PlayerUtils.closeInv(p);

                    // Create new order
                    final Order.Response response = Order.create(p, item, view.getText("money_per"), view.getText("amount"));

                    switch (response) {
                        case INVALID -> p.sendRichMessage(config.invalidInput);
                        case FAIL -> p.sendRichMessage(config.notEnoughMoney);
                        case SUCCESS -> {
                            p.sendRichMessage(config.orderCreationSuccessful);
                            PlayerUtils.playSound(p, config.newOrderSound);
                        }
                    }

                },
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
                    PlayerUtils.openGUI(p, chooseItemGUI, false);
                }
        );
    }
}
