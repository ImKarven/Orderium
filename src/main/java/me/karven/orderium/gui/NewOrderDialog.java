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
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialog {

    private static Orderium plugin;
    private static MiniMessage mm;
    private static ConfigManager cache;
    public static void init(Orderium plugin) {
        NewOrderDialog.plugin = plugin;
        mm = plugin.mm;
        cache = plugin.getConfigs();
    }

    public static void start(Player p) {
        ChooseItemGUI.choose(p, 0, 0);
    }

    public static void newSession(Player p, ItemStack displayItem) {

        try {
            p.showDialog(createDialog(
                    mm.deserialize(cache.getNewOrderDialogTitle()),
                    mm.deserialize(cache.getItemDescription()),
                    displayItem,
                    mm.deserialize(cache.getAmountLabel()),
                    mm.deserialize(cache.getMoneyPerLabel()),
                    mm.deserialize(cache.getChangeItemButton()),
                    mm.deserialize(cache.getChangeItemTooltip()),
                    mm.deserialize(cache.getConfirmButton()),
                    mm.deserialize(cache.getConfirmTooltip())
            ));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send dialog to player " + p.getName());

        }
    }

    private static Dialog createDialog(Component title,  Component bodyText, ItemStack display, Component amountLabel, Component moneyPerLabel, Component changeItemLabel, Component changeItemHover, Component confirmLabel, Component confirmHover) {
        return Dialog.create(builder -> {
            builder.empty()
                    .base(
                            DialogBase.builder(title)
                                    .canCloseWithEscape(true)
                                    .body(List.of(
                                            DialogBody.item(display)
                                                    .description(DialogBody.plainMessage(bodyText, cache.getDescriptionWidth()))
                                                    .showDecorations(false)
                                                    .build()
                                            )
                                    )
                                    .inputs(List.of(
                                            DialogInput.text("amount", amountLabel)
                                                    .width(cache.getInputWidth())
                                                    .initial("1")
                                                    .build(),
                                            DialogInput.text("money_per", moneyPerLabel)
                                                    .width(cache.getInputWidth())
                                                    .initial("1")
                                                    .build()
                                            )
                                    )
                                    .build()

                    )
                    .type(DialogType.confirmation(
                            ActionButton.builder(confirmLabel)
                                    .tooltip(confirmHover)
                                    .width(cache.getButtonWidth())
                                    .action(DialogAction.customClick((view, player) -> {
                                        if (!(player instanceof Player p)) {
                                            return;
                                        }
                                        // Create new order
                                        final String rawAmount = view.getText("amount");
                                        final String rawMoneyPer = view.getText("money_per");
                                        p.closeInventory();
                                        if (rawAmount == null || rawMoneyPer == null) {
                                            p.sendRichMessage(cache.getInvalidInput());
                                            return;
                                        }
                                        final double dAmount = ConvertUtils.formatNumber(rawAmount);
                                        final int amount = (int) dAmount;
                                        final double moneyPer = ConvertUtils.formatNumber(rawMoneyPer);
                                        if (dAmount == -1 || moneyPer == -1 || dAmount != amount) {
                                            p.sendRichMessage(cache.getInvalidInput());
                                            return;
                                        }
                                        if (!EconUtils.removeMoney(p, moneyPer * amount)) {
                                            p.sendRichMessage(cache.getNotEnoughMoney());
                                            return;
                                        }
                                        plugin.getDbManager().createOrder(p.getUniqueId(), display, moneyPer, amount);
                                        p.sendRichMessage(cache.getOrderCreationSuccessful());
                                    },  ClickCallback.Options.builder().uses(1).build()))
                                    .build(),
                            ActionButton.builder(changeItemLabel)
                                    .tooltip(changeItemHover)
                                    .width(cache.getButtonWidth())
                                    .build()
                    ));
        });
    }
}
