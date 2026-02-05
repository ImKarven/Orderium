package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.RequiredArgsConstructor;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class DeliveryConfirmDialog {
    private final Player p;
    private final Order order;
    private final int amount;
    private static Orderium plugin;
    private static MiniMessage mm;
    private static ConfigManager cache;

    public static void init(Orderium plugin) {
        DeliveryConfirmDialog.plugin = plugin;
        mm = plugin.mm;
        cache = plugin.getConfigs();
    }

    public DeliveryConfirmDialog(Player p, Order order, int amount, ChestGui returnGUI, List<ItemStack> returnItems) {
        this.p = p;
        this.order = order;
        this.amount = amount;
        final Economy eco = plugin.getEcon();

        final Dialog dialog = Dialog.create(builder -> {
            builder.empty()
                    .base(DialogBase.builder(mm.deserialize(cache.getConfirmDeliveryTitle()))
                            .body(List.of(
                                    DialogBody.plainMessage(mm.deserialize(cache.getConfirmDeliveryBody())),
                                    DialogBody.item(order.item()).description(DialogBody.plainMessage(Component.text(ConvertUtils.formatNumber(order.amount())))).build(),
                                    DialogBody.plainMessage(mm.deserialize(cache.getConfirmDeliveryTransactionMessage(), Placeholder.unparsed("money", ConvertUtils.formatNumber(amount * order.moneyPer()))))
                            ))
                            .build())
                    .type(DialogType.confirmation(
                            ActionButton.builder(mm.deserialize(cache.getConfirmDeliveryConfirmLabel()))
                                    .tooltip(mm.deserialize(cache.getConfirmDeliveryConfirmHover()))
                                    .action(DialogAction.customClick((view, player) -> {
                                        final int maxDeliverAmount = order.amount() - order.delivered();

                                        p.give(returnItems, true);

                                        if (amount <= maxDeliverAmount) {
                                            EconUtils.addMoney(p, order.deliver(amount));
                                            return;
                                        }
                                        EconUtils.addMoney(p, order.deliver(maxDeliverAmount));

                                        int rem = amount - maxDeliverAmount;
                                        final ItemStack item = order.item();
                                        final int maxStackSize = order.item().getMaxStackSize();
                                        if (rem >= maxStackSize) {
                                            final ItemStack copy = item.clone();
                                            copy.setAmount(maxStackSize);
                                            final int fullStackAmount = rem / maxStackSize;
                                            final List<ItemStack> items = new ArrayList<>();
                                            for (int i = 0; i < fullStackAmount; i++) {
                                                items.add(copy.clone());
                                            }
                                            p.give(items, true);
                                            rem %= maxStackSize;
                                        }

                                        if (rem > 0) {
                                            final ItemStack copy = item.clone();
                                            copy.setAmount(rem);
                                            p.give(List.of(copy), true);
                                        }

                                    }, ClickCallback.Options.builder().build()))
                                    .build(),
                            ActionButton.builder(mm.deserialize(cache.getConfirmDeliveryCancelLabel()))
                                    .tooltip(mm.deserialize(cache.getConfirmDeliveryCancelHover()))
                                    .action(DialogAction.customClick(
                                            (view, player) -> {
                                                returnGUI.setOnClose((ignored) -> {});
                                                returnGUI.show(p);
                                            },
                                            ClickCallback.Options.builder().build()))
                                    .build()
                    ))

            ;
        });

        p.showDialog(dialog);
    }
}
