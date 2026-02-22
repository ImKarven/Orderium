package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static me.karven.orderium.utils.ConvertUtils.ceil_div;

@SuppressWarnings("UnstableApiUsage")
public class MainGUI {
    private final List<ChestGui> pages = new ArrayList<>();
    private final Collection<Order> orders;
    private final Player player;
    private final int amount;
    private final int sortIdx;
    private final String search;

    private static Orderium plugin;
    private static ConfigManager cache;
    private static MiniMessage mm;

    public static void init(Orderium pl) {
        plugin = pl;
        cache = plugin.getConfigs();
        mm = plugin.mm;
    }

    public static void cancelDelivery(final ChestGui gui, final Player p) {

        final Collection<ItemStack> items = new ArrayList<>();
        for (final ItemStack item : gui.getInventory()) {
            if (item == null) continue;
            items.add(item);
        }
        PlayerUtils.give(p, items);
    }

    public MainGUI(Player p, int sortIdx) {
        this.search = "";
        this.sortIdx = sortIdx;
        this.player = p;
        final SortTypes sortType = cache.getOrdersSortsOrder().get(sortIdx);
        orders = plugin.getDbManager().getSortedOrders(sortType);
        this.amount = ceil_div(orders.size(), 45);
        setupPages();
        open(p);
    }

    public MainGUI(Player p, int sortIdx, String search) {
        this.search = search;
        this.sortIdx = sortIdx;
        this.player = p;
        final SortTypes sortType = cache.getOrdersSortsOrder().get(sortIdx);
        orders = AlgoUtils.searchOrder(search, plugin.getDbManager().getSortedOrders(sortType));

        this.amount = ceil_div(orders.size(), 45);
        setupPages();
        open(p);
    }

    private void setupPages() {
        int curr = 0, cnt = 0;
        ChestGui page = initPage();
        OutlinePane orderPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addButtons(buttonsPane, curr);

        for (final Order order : orders) {
            if (!order.isActive()) continue;
            if (cnt == 45) {
                cnt = 0;
                page.addPane(orderPane);
                page.addPane(buttonsPane);
                pages.add(page);
                page = initPage();
                orderPane = new OutlinePane(0, 0, 9, 5);
                buttonsPane = new StaticPane(0, 5, 9, 1);
                addButtons(buttonsPane, ++curr);
            }
            orderPane.addItem(ConvertUtils.parseOrder(order, cache.getOrderLore(), e -> {
                if (player.getUniqueId().equals(order.owner())) {
                    player.sendRichMessage(cache.getDeliverSelf());
                    return;
                }
                setupDeliverGUI(order).show(e.getWhoClicked());
            }));
            cnt++;
        }
        page.addPane(orderPane);
        page.addPane(buttonsPane);
        pages.add(page);
    }

    private ChestGui initPage() {
        ChestGui page = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        page.setOnGlobalDrag(e -> e.setCancelled(true));
        return page;
    }

    private ChestGui setupDeliverGUI(Order order) {
        final ItemStack comparer = order.item();
        final ChestGui deliverGUI = new ChestGui(cache.getDeliverRows(), ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));

        deliverGUI.setOnClose(e -> {
            if (!(e.getPlayer() instanceof Player p)) return;
            final Inventory inv = e.getInventory();

            int amount = 0;
            final List<ItemStack> items = new ArrayList<>();
            amount += scanInv(inv, items, comparer);

            if (amount == 0) {
                PlayerUtils.give(p, items);
                return;
            }

            DeliveryConfirmDialog.show(p, order, amount, items);
        });
        return deliverGUI;
    }

    /// Returns the amount of similar item found
    private int scanInv(Inventory inv, List<ItemStack> items, ItemStack comparer) {
        int amount = 0;
        for (final ItemStack item : inv) {
            if (item == null || item.isEmpty()) continue;
            items.add(item);
            if (!AlgoUtils.isSimilar(item, comparer)) continue;
            amount += item.getAmount();
        }
        return amount;
    }

    private void addButtons(StaticPane buttonsPane, int curr) {
        if (curr > 0)
            buttonsPane.addItem(ConvertUtils.parseButton(cache.getOrdersBackButton(), e -> {
                PlayerUtils.clickBack(e, pages.get(curr - 1));
            }
        ), cache.getOrdersBackButton().getSlot(), 0);

        if (curr + 1 < amount)
            buttonsPane.addItem(ConvertUtils.parseButton(cache.getOrdersNextButton(), e -> {
                PlayerUtils.clickNext(e, pages.get(curr + 1));
            }
        ), cache.getOrdersNextButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getRefreshButton(), e -> {

            if (search.isEmpty()) new MainGUI(player, sortIdx);
            else new MainGUI(player, sortIdx, search);

            PlayerUtils.playSound(player, cache.getRefreshSound());

        }), cache.getRefreshButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseSortButton(cache.getOrdersSortButton(), cache.getOrdersSortsOrder().get(sortIdx), e -> {

            if (search.isEmpty()) new MainGUI(player, sortIdx + 1 == cache.getOrdersSortsOrder().size() ? 0 : sortIdx + 1);
            else new MainGUI(player, sortIdx + 1 == cache.getOrdersSortsOrder().size() ? 0 : sortIdx + 1, search);

            PlayerUtils.playSound(player, cache.getSortSound());

        }), cache.getOrdersSortButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getOrdersSearchButton(), e -> {
            SignGUI.newSession(
                    player,
                    (s) -> player.getScheduler().run(plugin, t -> new MainGUI(player, sortIdx, s), null),
                    cache.getLines(), cache.getSignBlock(), cache.getSearchLine()
            );
        }), cache.getOrdersSearchButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getYoButton(), e -> {
            YourOrderGUI.open(player);
        }), cache.getYoButton().getSlot(), 0);
    }


    private void open(Player p) {
        PlayerUtils.openGui(p, pages.getFirst());
    }
}
