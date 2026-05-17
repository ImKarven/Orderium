package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static me.karven.orderium.config.ConfigCache.cache;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialog {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static Dialog getDialog(Order order, Player player_) {
        final String name = player_.getName();
        final ItemStack item = ConvertUtils.parseOrder(order, cache.yoLore);
        final Dialog collectItemsDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm.deserialize(cache.collectItemsTitle))
                        .body(
                                List.of(
                                        DialogBody.item(item)
                                                .description(DialogBody.plainMessage(ConvertUtils.delOrder(cache.collectItemsBody, order, name)))
                                                .build()
                                )
                        )
                        .inputs(
                                List.of(
                                        DialogInput.text("amount", mm.deserialize(cache.collectItemsAmountLabel))
                                                .initial("1")
                                                .build()
                                )
                        )
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.builder(mm.deserialize(cache.collectItemsConfirmLabel))
                                .tooltip(mm.deserialize(cache.collectItemsConfirmHover))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (!(player instanceof Player p)) return;

                                    final String rawAmount = view.getText("amount");
                                    order.collect(rawAmount);
                                    YourOrderGUI.open(p);
                                }, ClickCallback.Options.builder().build()))
                                .build(),
                        ActionButton.builder(mm.deserialize(cache.collectItemsCancelLabel))
                                .tooltip(mm.deserialize(cache.collectItemsCancelHover))
                                .build()
                ))
        );

        final Dialog cancelOrderDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm.deserialize(cache.cancelOrderTitle))
                        .body(
                                List.of(
                                        DialogBody.item(item)
                                                .description(DialogBody.plainMessage(ConvertUtils.delOrder(cache.cancelOrderBody, order, name)))
                                                .build()
                                )
                        )
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.builder(mm.deserialize(cache.cancelOrderConfirmLabel))
                                .tooltip(mm.deserialize(cache.cancelOrderConfirmHover))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (!(player instanceof Player p)) return;
                                    order.cancel(p);
                                }, ClickCallback.Options.builder().build()))
                                .build(),
                        ActionButton.builder(mm.deserialize(cache.cancelOrderCancelLabel))
                                .tooltip(mm.deserialize(cache.cancelOrderCancelHover))
                                .build()
                ))
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm.deserialize(cache.manageOrderTitle))
                        .body(
                                List.of(
                                        DialogBody.item(item)
                                                .description(DialogBody.plainMessage(ConvertUtils.delOrder(cache.manageOrderBody, order, name)))
                                                .build()
                                )
                        )
                        .build())
                .type(DialogType.multiAction(
                        List.of(
                                ActionButton.builder(mm.deserialize(cache.collectItemsLabel))
                                        .tooltip(mm.deserialize(cache.collectItemsHover))
                                        .action(DialogAction.customClick((view, player) -> {
                                            if (!(player instanceof Player p)) return;
                                            PlayerUtils.openDialog(p, collectItemsDialog);
                                        }, ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(mm.deserialize(cache.cancelOrderLabel))
                                        .tooltip(mm.deserialize(cache.cancelOrderHover))
                                        .action(DialogAction.customClick((view, player) -> {
                                            if (!(player instanceof Player p)) return;
                                            PlayerUtils.openDialog(p, cancelOrderDialog);
                                        }, ClickCallback.Options.builder().build()))
                                        .build()
                        )
                )
                                .build()
                )
        );
    }
}
