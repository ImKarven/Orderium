package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialog {
    private static ConfigManager cache;
    private static MiniMessage mm;

    public static void init(Orderium plugin) {
        cache = plugin.getConfigs();
        mm = plugin.mm;
    }

    public static void show(Order order, Player player_) {
        final String name = player_.getName();
        final ItemStack item = ConvertUtils.parseOrder(order, cache.getYoLore());
        final Dialog collectItemsDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm.deserialize(cache.getCollectItemsTitle()))
                        .body(
                                List.of(
                                        DialogBody.item(item)
                                                .description(DialogBody.plainMessage(ConvertUtils.delOrder(cache.getCollectItemsBody(), order, name)))
                                                .build()
                                )
                        )
                        .inputs(
                                List.of(
                                        DialogInput.text("amount", mm.deserialize(cache.getCollectItemsAmountLabel()))
                                                .initial("1")
                                                .build()
                                )
                        )
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.builder(mm.deserialize(cache.getCollectItemsConfirmLabel()))
                                .tooltip(mm.deserialize(cache.getCollectItemsConfirmHover()))
                                .action(DialogAction.customClick((view, player) -> {
                                    if (!(player instanceof Player p)) return;

                                    final String rawAmount = view.getText("amount");
                                    if (rawAmount == null) return;
                                    final double dAmount = ConvertUtils.formatNumber(rawAmount);
                                    final int amount = (int) dAmount;
                                    if (dAmount == -1 || dAmount != amount || order.inStorage() < amount) {
                                        p.sendRichMessage(cache.getInvalidInput());
                                        return;
                                    }
                                    order.setInStorage(order.inStorage() - amount);
                                    final ItemStack copy = order.item().clone();
                                    final int maxSize = copy.getMaxStackSize();
                                    copy.setAmount(maxSize);
                                    final int maxStacks = amount / maxSize;
                                    final int rem = amount % maxSize;
                                    final Collection<ItemStack> toGive = new ArrayList<>();
                                    for (int i = 0; i < maxStacks; i++) {
                                        toGive.add(copy.clone());
                                    }
                                    if (rem > 0) {
                                        copy.setAmount(rem);
                                        toGive.add(copy);
                                    }
                                    p.give(toGive, true);
                                    YourOrderGUI.open(p);
                                }, ClickCallback.Options.builder().build()))
                                .build(),
                        ActionButton.builder(mm.deserialize(cache.getCollectItemsCancelLabel()))
                                .tooltip(mm.deserialize(cache.getCollectItemsCancelHover()))
                                .build()
                ))
        );

        final Dialog cancelOrderDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm.deserialize(cache.getCancelOrderTitle()))
                        .body(
                                List.of(
                                        DialogBody.item(item)
                                                .description(DialogBody.plainMessage(ConvertUtils.delOrder(cache.getCancelOrderBody(), order, name)))
                                                .build()
                                )
                        )
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.builder(mm.deserialize(cache.getCancelOrderConfirmLabel()))
                                .tooltip(mm.deserialize(cache.getCancelOrderConfirmHover()))
                                .action(DialogAction.customClick((v, player) -> {
                                    if (!(player instanceof Player p)) return;
                                    order.cancel();
                                    YourOrderGUI.open(p);
                                }, ClickCallback.Options.builder().build()))
                                .build(),
                        ActionButton.builder(mm.deserialize(cache.getCancelOrderCancelLabel()))
                                .tooltip(mm.deserialize(cache.getCancelOrderCancelHover()))
                                .build()
                ))
        );

        final Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mm.deserialize(cache.getManageOrderTitle()))
                        .body(
                                List.of(
                                        DialogBody.item(item)
                                                .description(DialogBody.plainMessage(ConvertUtils.delOrder(cache.getManageOrderBody(), order, name)))
                                                .build()
                                )
                        )
                        .build())
                .type(DialogType.multiAction(
                        List.of(
                                ActionButton.builder(mm.deserialize(cache.getCollectItemsLabel()))
                                        .tooltip(mm.deserialize(cache.getCollectItemsHover()))
                                        .action(DialogAction.customClick((v, p) -> {
                                            p.showDialog(collectItemsDialog);
                                        }, ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(mm.deserialize(cache.getCancelOrderLabel()))
                                        .tooltip(mm.deserialize(cache.getCancelOrderHover()))
                                        .action(DialogAction.customClick((v, p) -> {
                                            p.showDialog(cancelOrderDialog);
                                        }, ClickCallback.Options.builder().build()))
                                        .build()
                        )
                )
                                .build()
                )
        );
        player_.showDialog(dialog);
    }
}
