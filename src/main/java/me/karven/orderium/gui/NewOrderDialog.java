package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.SearchableItem;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static me.karven.orderium.config.ConfigCache.cache;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialog {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static Dialog getDialog(OrderItem orderItem) {
        return createDialog(
                mm.deserialize(cache.newOrderDialogTitle),
                mm.deserialize(cache.itemDescription),
                orderItem,
                mm.deserialize(cache.amountLabel),
                mm.deserialize(cache.moneyPerLabel),
                mm.deserialize(cache.changeItemButton),
                mm.deserialize(cache.changeItemTooltip),
                mm.deserialize(cache.confirmButton),
                mm.deserialize(cache.confirmTooltip)
        );
    }

    private static Dialog createDialog(Component title,  Component bodyText, OrderItem orderItem, Component amountLabel, Component moneyPerLabel, Component changeItemLabel, Component changeItemHover, Component confirmLabel, Component confirmHover) {
        return Dialog.create(builder ->
            builder.empty()
                    .base(
                            DialogBase.builder(title)
                                    .canCloseWithEscape(true)
                                    .body(List.of(
                                            DialogBody.item(orderItem.getItemStack())
                                                    .description(DialogBody.plainMessage(bodyText, cache.descriptionWidth))
                                                    .showDecorations(false)
                                                    .build()
                                            )
                                    )
                                    .inputs(List.of(
                                            DialogInput.text("amount", amountLabel)
                                                    .width(cache.inputWidth)
                                                    .initial("1")
                                                    .build(),
                                            DialogInput.text("money_per", moneyPerLabel)
                                                    .width(cache.inputWidth)
                                                    .initial("1")
                                                    .build()
                                            )
                                    )
                                    .build()

                    )
                    .type(DialogType.confirmation(
                            ActionButton.builder(confirmLabel)
                                    .tooltip(confirmHover)
                                    .width(cache.buttonWidth)
                                    .action(DialogAction.customClick((view, player) -> {
                                        if (!(player instanceof Player p)) {
                                            return;
                                        }

                                        PlayerUtils.closeInv(p);

                                        // Create new order
                                        ItemStack item = orderItem instanceof SearchableItem searchableItem ? searchableItem.getParsedItemStack() : orderItem.getItemStack();
                                        final Order.Response response = Order.create(p, item, view.getText("money_per"), view.getText("amount"));

                                        switch (response) {
                                            case INVALID -> p.sendRichMessage(cache.invalidInput);
                                            case FAIL -> p.sendRichMessage(cache.notEnoughMoney);
                                            case SUCCESS -> {
                                                p.sendRichMessage(cache.orderCreationSuccessful);
                                                PlayerUtils.playSound(p, cache.newOrderSound);
                                            }
                                        }

                                    },  ClickCallback.Options.builder().uses(1).build()))
                                    .build(),
                            ActionButton.builder(changeItemLabel)
                                    .tooltip(changeItemHover)
                                    .width(cache.buttonWidth)
                                    .action(DialogAction.customClick((view, player) -> {
                                        if (!(player instanceof Player p)) return;
                                        InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
                                        PlayerUtils.openGUI(p, chooseItemGUI, false);
                                    }, ClickCallback.Options.builder().uses(1).build()))
                                    .build()
                    ))
        );
    }
}
