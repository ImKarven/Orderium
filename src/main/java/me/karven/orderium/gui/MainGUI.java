package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static me.karven.orderium.utils.ConvertUtils.ceil_div;

public class MainGUI {
    private final List<ChestGui> pages = new ArrayList<>();
    private final Set<Order> orders;
    private final Orderium plugin;
    private final ConfigManager cache;
    private final MiniMessage mm;
    private final Player player;
    private final int amount;
    private final int sortIdx;

    public MainGUI(Orderium plugin, Player p, int sortIdx) {
        this.sortIdx = sortIdx;
        this.plugin = plugin;
        this.cache = plugin.getConfigs();
        this.mm = plugin.mm;
        this.player = p;
        final SortTypes sortType = cache.getOrdersSortsOrder().get(sortIdx);
        orders = plugin.getDbManager().getSortedOrders(sortType);
        if (orders.isEmpty()) { // No active order but still create a page with buttons
            this.amount = -1;
            final ChestGui gui = new ChestGui(6, cache.getMainGuiTitle());
            final StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
            addButtons(buttonsPane, 0);
            gui.addPane(buttonsPane);
            pages.add(gui);
        } else {
            this.amount = ceil_div(orders.size(), 45);
            setupPages();
        }
        open(p);
    }

    private void setupPages() {
        int curr = 0, cnt = 0;
        ChestGui page = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));
        OutlinePane orderPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addButtons(buttonsPane, curr);
        page.addPane(orderPane);
        page.addPane(buttonsPane);

        for (final Order order : orders) {
            if (cnt == 45) {
                cnt = 0;
                pages.add(page);
                page = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));
                orderPane = new OutlinePane(0, 0, 9, 5);
                buttonsPane = new StaticPane(0, 5, 9, 1);
                addButtons(buttonsPane, ++curr);
            }
            orderPane.addItem(ConvertUtils.parseOrder(order, cache.getOrderLore(), e -> {
                e.setCancelled(true);
                // TODO: handle order process
                final ItemStack comparer = order.item();
                final ChestGui deliverGUI = new ChestGui(cache.getDeliverRows(), ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));
                deliverGUI.setOnClose(e2 -> {
                    if (!(e2.getPlayer() instanceof Player p)) return;
                    final List<ItemStack> returns = new ArrayList<>();
                    int amount = 0;
                    for (final ItemStack item : e2.getInventory()) {
                        if (item.isSimilar(comparer)) {
                            amount += item.getAmount();
                            continue;
                        }

                        returns.add(item);
                    }


                });
            }));
            cnt++;
        }
        pages.add(page);
    }

    private void addButtons(StaticPane buttonsPane, int curr) {
        if (curr > 0) buttonsPane.addItem(ConvertUtils.parseButton(cache.getOrdersBackButton(), e -> {
            e.setCancelled(true);
            pages.get(curr - 1).show(player);
        }), cache.getOrdersBackButton().getSlot(), 0);

        if (curr + 1 < amount) buttonsPane.addItem(ConvertUtils.parseButton(cache.getOrdersNextButton(), e -> {
            e.setCancelled(true);
            pages.get(curr + 1).show(player);
        }), cache.getOrdersNextButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getRefreshButton(), e -> {
            e.setCancelled(true);
            new MainGUI(plugin, player, sortIdx);
        }), cache.getRefreshButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseSortButton(cache.getOrdersSortButton(), cache.getOrdersSortsOrder().get(sortIdx), e -> {
            e.setCancelled(true);
            new MainGUI(plugin, player, sortIdx + 1 == cache.getOrdersSortsOrder().size() ? 0 : sortIdx + 1);
        }), cache.getOrdersSortButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getOrdersSearchButton(), e -> {
            e.setCancelled(true);
            // TODO: Handle search
        }), cache.getOrdersSearchButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getYoButton(), e -> {
            e.setCancelled(true);
            new YourOrderGUI(plugin, player);
        }), cache.getYoButton().getSlot(), 0);
    }


    private void open(Player p) {
        pages.getFirst().show(p);
    }
}
