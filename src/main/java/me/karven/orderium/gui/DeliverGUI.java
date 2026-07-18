package me.karven.orderium.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.karven.orderium.Orderium.plugin;

public class DeliverGUI {
    public static final MiniMessage mm = MiniMessage.miniMessage();
    private final InventoryGUI deliverGUI;

    public DeliverGUI(final @NotNull Order order) {
        final ItemStack comparer = order.getItem();
        final Config currentConfig = Config.config;
        this.deliverGUI = new InventoryGUI(currentConfig.deliverGUIConfig.rows, mm.deserialize(currentConfig.deliverGUIConfig.title));

        deliverGUI.setOnClose(e -> {
            if (!(e.getPlayer() instanceof Player deliveringPlayer)) return;
            final Inventory inv = e.getInventory();

            int amount = 0;
            amount += scanInv(inv, comparer);

            final List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : inv) {
                if (item == null || item.isEmpty()) continue;
                items.add(item);
            }

            if (amount == 0) {
                PlayerUtils.give(deliveringPlayer, items, false);
                deliveringPlayer.getScheduler().run(plugin, _ -> deliveringPlayer.updateInventory(), null);
                return;
            }

            final Dialog dialog = DeliveryConfirmDialog.getDialog(deliveringPlayer, order, amount, order.moneyPer * amount, items);

            PlayerUtils.openDialog(deliveringPlayer, dialog);
        });
    }

    public @NotNull InventoryGUI getGUI() {
        return deliverGUI;
    }


    /**
     * Scan an inventory for similar items
     * @param inv the inventory to scan
     * @param comparer the item to compare for similarity
     * @return the amount of similar items
     */
    @SuppressWarnings("UnstableApiUsage")
    private static int scanInv(Iterable<ItemStack> inv, ItemStack comparer) {
        int amount = 0;
        for (final ItemStack item : inv) {
            if (item == null || item.isEmpty()) continue;
            if (AlgoUtils.isSimilar(item, comparer)) {
                amount += item.getAmount();
                continue;
            }
            if (!Config.config.shulkerDelivering) continue;
            ItemContainerContents shulkerContent = item.getData(DataComponentTypes.CONTAINER);
            if (shulkerContent == null) continue;

            amount += scanInv(shulkerContent.contents(), comparer);
        }
        return amount;
    }
}
