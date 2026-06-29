package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import me.karven.orderium.config.Config;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.SearchableItem;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static me.karven.orderium.config.Config.config;
import static me.karven.orderium.utils.ConvertUtils.formatNumber;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialog {
    public static Dialog getDialog(OrderItem orderItem) {
        final ItemStack item = orderItem instanceof SearchableItem searchableItem ? searchableItem.getParsedItemStack() : orderItem.getItemStack();

        final DialogActionCallback cancelAction = (view, player) -> {
            if (!(player instanceof Player p)) return;
            InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
            PlayerUtils.openGUI(p, chooseItemGUI, false);
        };

        return config.newOrderDialogConfig.dialog(
                item,
                (view, player) -> {
                    if (!(player instanceof Player p)) {
                        return;
                    }
                    final String stringMoneyPer = view.getText("money_per");
                    final String stringAmount = view.getText("amount");

                    // Cache the config object to prevent tearing
                    final Config config = Config.config;
                    if (stringAmount == null || stringMoneyPer == null) {
                        p.sendRichMessage(config.invalidInput);
                        PlayerUtils.closeInv(p);
                        return;
                    }
                    final double dAmount = formatNumber(stringAmount);
                    final int amount = (int) dAmount;
                    final double moneyPer = formatNumber(stringMoneyPer);
                    if (dAmount == -1 || moneyPer == -1 || moneyPer < config.minPrice || dAmount != amount) {
                        p.sendRichMessage(config.invalidInput);
                        PlayerUtils.closeInv(p);
                        return;
                    }

                    final Runnable confirmAction = () -> {
                        PlayerUtils.closeInv(p);
                        // Create new order
                        final Order.Response response = Order.create(p, item, moneyPer, amount);

                        final Config nestedConfig = Config.config;
                        switch (response) {
                            case INVALID -> p.sendRichMessage(nestedConfig.invalidInput);
                            case FAIL -> p.sendRichMessage(nestedConfig.notEnoughMoney);
                            case SUCCESS -> {
                                p.sendRichMessage(nestedConfig.orderCreationSuccessful);
                                PlayerUtils.playSound(p, nestedConfig.newOrderSound);
                            }
                        }
                    };

                    if (config.newOrderDialogConfig.confirmTooltip.isEmpty()) {
                        confirmAction.run();
                        return;
                    }

                    final Dialog confirmationDialog = config.newOrderDialogConfig.confirmDialog(
                            item,
                            (nestedView, nestedAudience) -> confirmAction.run(),
                            cancelAction,
                            Placeholder.unparsed("cost", formatNumber(moneyPer * amount))
                    );

                    p.showDialog(confirmationDialog);
                },
                cancelAction
        );
    }
}
