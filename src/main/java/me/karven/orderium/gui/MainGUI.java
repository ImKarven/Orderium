package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static me.karven.orderium.load.Orderium.plugin;
import static me.karven.orderium.utils.ConvertUtils.ceil_div;

public class MainGUI {
    private final List<ChestGui> pages = new ArrayList<>();
    private final Collection<Order> orders;
    private final Player player;
    private final int amount;
    private final int sortIdx;
    private final String search;

    private static ConfigCache cache;
    private static MiniMessage mm;

    public static void init() {
        cache = plugin.getConfigs();
        mm = plugin.mm;
    }

    public MainGUI(Player p, int sortIdx) {
        this.search = "";
        this.sortIdx = sortIdx;
        this.player = p;
        final SortTypes sortType = cache.getOrdersSortsOrder().get(sortIdx);

        Collection<Order> allOrders = plugin.getDataCache().getSortedOrders(sortType);
        orders = allOrders.stream().filter(Order::isActive).toList();

        this.amount = ceil_div(orders.size(), 45);
        setupPages();
        open(p);
    }

    public MainGUI(Player p, int sortIdx, String search) {
        this.search = search;
        this.sortIdx = sortIdx;
        this.player = p;
        final SortTypes sortType = cache.getOrdersSortsOrder().get(sortIdx);

        Collection<Order> allOrders = AlgoUtils.searchOrder(search, plugin.getDataCache().getSortedOrders(sortType));
        orders = allOrders.stream().filter(Order::isActive).toList();

        this.amount = ceil_div(orders.size(), 45);
        setupPages();
        open(p);
    }

    private void setupPages() {
        int curr = 0, cnt = 0;
        ChestGui page = initPage();
        OutlinePane orderPane = new OutlinePane(9, 5);
        StaticPane buttonsPane = new StaticPane(9, 1);
        addButtons(buttonsPane, curr);

        for (final Order order : orders) {
            if (cnt == 45) {
                cnt = 0;
                page.addPane(Slot.fromXY(0, 0), orderPane);
                page.addPane(Slot.fromXY(0, 5), buttonsPane);
                pages.add(page);
                page = initPage();
                orderPane = new OutlinePane(9, 5);
                buttonsPane = new StaticPane(9, 1);
                addButtons(buttonsPane, ++curr);
            }
            orderPane.addItem(ConvertUtils.parseOrder(order, cache.getOrderLore(), e -> {
                HumanEntity who = e.getWhoClicked();
                if (e.getClick() == ClickType.RIGHT && who.hasPermission("orderium.admin.edit-orders")) {
                    Dialog dialog = AdminToolGUI.createEditOrder(order);
                    who.showDialog(dialog);
                }
                if (player.getUniqueId().equals(order.getOwnerUniqueId())) {
                    player.sendRichMessage(cache.getDeliverSelf());
                    return;
                }
                ChestGui deliverGUI = setupDeliverGUI(order);
                deliverGUI.show(who);
            }));
            cnt++;
        }
        page.addPane(Slot.fromXY(0, 0), orderPane);
        page.addPane(Slot.fromXY(0, 5), buttonsPane);
        pages.add(page);
    }

    private ChestGui initPage() {
        ChestGui page = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        page.setOnGlobalDrag(e -> e.setCancelled(true));
        return page;
    }

    private ChestGui setupDeliverGUI(Order order) {
        final ItemStack comparer = order.getItem();
        final ChestGui deliverGUI = new ChestGui(cache.getDeliverRows(), ComponentHolder.of(mm.deserialize(cache.getMainGuiTitle())));

        deliverGUI.setOnClose(e -> {
            if (!(e.getPlayer() instanceof Player p)) return;
            final Inventory inv = e.getInventory();

            int amount = 0;
            amount += scanInv(inv, comparer);

            final List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : inv) {
                if (item == null || item.isEmpty()) continue;
                items.add(item);
            }

            if (amount == 0) {
                PlayerUtils.give(p, items, false);
                p.getScheduler().run(plugin, _ -> p.updateInventory(), null);
                return;
            }

            DeliveryConfirmDialog.show(p, order, amount, items);
        });
        return deliverGUI;
    }

    /**
     * Scan an inventory for similar items
     * @param inv the inventory to scan
     * @param comparer the item to compare for similarity
     * @return the amount of similar items
     */
    @SuppressWarnings("UnstableApiUsage")
    private int scanInv(Iterable<ItemStack> inv, ItemStack comparer) {
        int amount = 0;
        for (final ItemStack item : inv) {
            if (item == null || item.isEmpty()) continue;
            if (AlgoUtils.isSimilar(item, comparer)) {
                amount += item.getAmount();
                continue;
            }
            if (!cache.isShulkerDelivering() || !isShulkerBox(item)) continue;
            ItemContainerContents shulkerContent = item.getData(DataComponentTypes.CONTAINER);
            if (shulkerContent == null) continue;

            amount += scanInv(shulkerContent.contents(), comparer);
        }
        return amount;
    }

    private boolean isShulkerBox(ItemStack item) {
        Material type = item.getType();
        return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX ||
                type == Material.BLACK_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX ||
                type == Material.YELLOW_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX ||
                type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX ||
                type == Material.MAGENTA_SHULKER_BOX || type == Material.PINK_SHULKER_BOX;
    }

    private void addButtons(StaticPane buttonsPane, int curr) {
        if (curr > 0)
            buttonsPane.addItem(ConvertUtils.parseButton(
                    cache.getOrdersBackButton(),
                    e -> PlayerUtils.clickBack(e, pages.get(curr - 1))
        ), cache.getOrdersBackButton().getSlot(), 0);

        if (curr + 1 < amount)
            buttonsPane.addItem(ConvertUtils.parseButton(
                    cache.getOrdersNextButton(),
                    e -> PlayerUtils.clickNext(e, pages.get(curr + 1))
        ), cache.getOrdersNextButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(cache.getRefreshButton(), _ -> {

            if (search.isEmpty()) new MainGUI(player, sortIdx);
            else new MainGUI(player, sortIdx, search);

            PlayerUtils.playSound(player, cache.getRefreshSound());

        }), cache.getRefreshButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseSortButton(cache.getOrdersSortButton(), cache.getOrdersSortsOrder().get(sortIdx), _ -> {

            if (search.isEmpty()) new MainGUI(player, sortIdx + 1 == cache.getOrdersSortsOrder().size() ? 0 : sortIdx + 1);
            else new MainGUI(player, sortIdx + 1 == cache.getOrdersSortsOrder().size() ? 0 : sortIdx + 1, search);

            PlayerUtils.playSound(player, cache.getSortSound());

        }), cache.getOrdersSortButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(
                cache.getOrdersSearchButton(),
                _ -> SignGUI.newSession(
                    player,
                    (s) -> DispatchUtil.entity(player, () -> new MainGUI(player, sortIdx, s)),
                    cache.getLines(), cache.getSignBlock(), cache.getSearchLine()
                )
        ), cache.getOrdersSearchButton().getSlot(), 0);

        buttonsPane.addItem(ConvertUtils.parseButton(
                cache.getYoButton(),
                _ -> YourOrderGUI.open(player)
        ), cache.getYoButton().getSlot(), 0);
    }


    private void open(Player p) {
        PlayerUtils.openGui(p, pages.getFirst());
    }
}
