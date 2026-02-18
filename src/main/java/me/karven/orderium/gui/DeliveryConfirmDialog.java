package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class DeliveryConfirmDialog {
    private static MiniMessage mm;
    private static ConfigManager cache;

    public static void init(Orderium plugin) {
        mm = plugin.mm;
        cache = plugin.getConfigs();
    }

    public static void show(Player p, Order order, int amount, ChestGui returnGUI, List<ItemStack> returnItems) {
        final Dialog dialog = Dialog.create(builder -> {
            final String amountText = ConvertUtils.formatNumber(amount);
            final int amountWidth = amountText.length() * 10;
            builder.empty()
                    .base(DialogBase.builder(mm.deserialize(cache.getConfirmDeliveryTitle()))
                            .body(List.of(
                                    DialogBody.plainMessage(mm.deserialize(cache.getConfirmDeliveryBody())),
                                    DialogBody.item(ConvertUtils.parseOrder(order, cache.getOrderLore())).description(DialogBody.plainMessage(Component.text(amountText), amountWidth)).build(),
                                    DialogBody.plainMessage(mm.deserialize(cache.getConfirmDeliveryTransactionMessage(), Placeholder.unparsed("money", ConvertUtils.formatNumber(amount * order.moneyPer()))))
                            ))
                            .build())
                    .type(DialogType.confirmation(
                            ActionButton.builder(mm.deserialize(cache.getConfirmDeliveryConfirmLabel()))
                                    .tooltip(mm.deserialize(cache.getConfirmDeliveryConfirmHover()))
                                    .action(DialogAction.customClick((view, player) -> {
                                        final int maxDeliverAmount = order.amount() - order.delivered();

                                        PlayerUtils.give(p, returnItems, true);

                                        if (amount <= maxDeliverAmount) {
                                            order.deliver(p, amount);
                                            return;
                                        }
                                        order.deliver(p, maxDeliverAmount);

                                        final int rem = amount - maxDeliverAmount;
                                        PlayerUtils.give(p, order.item().clone(), rem);
                                    }, ClickCallback.Options.builder().build()))
                                    .build(),
                            ActionButton.builder(mm.deserialize(cache.getConfirmDeliveryCancelLabel()))
                                    .tooltip(mm.deserialize(cache.getConfirmDeliveryCancelHover()))
                                    .action(DialogAction.customClick(
                                            (view, player) -> MainGUI.cancelDelivery(returnGUI, p),
                                            ClickCallback.Options.builder().build()))
                                    .build()
                    ))

            ;
        });

        PlayerUtils.openDialog(p, dialog);
    }
}
