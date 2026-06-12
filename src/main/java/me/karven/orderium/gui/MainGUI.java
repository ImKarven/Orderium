package me.karven.orderium.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.AlgoUtils;
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

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.config.Config.config;
import static me.karven.orderium.utils.ConvertUtils.ceil_div;

public class MainGUI {
    private final List<InventoryGUI> pages = new ArrayList<>();
    private final Collection<Order> orders;
    private final Player player;
    private final int amount;
    private final int sortIdx;
    private final String search;

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public MainGUI(Player p, int sortIdx) {
        this.search = "";
        this.sortIdx = sortIdx;
        this.player = p;
        final SortType sortType = config.mainGUIConfig.sortsOrderConfig.index(sortIdx);

        Collection<Order> allOrders = plugin.getDataCache().getSortedOrders(sortType);
        orders = allOrders.stream().filter(Order::isActive).toList();

        this.amount = ceil_div(orders.size(), 45);
        setupPages();
    }

    public MainGUI(Player p, int sortIdx, String search) {
        this.search = search;
        this.sortIdx = sortIdx;
        this.player = p;
        final SortType sortType = config.mainGUIConfig.sortsOrderConfig.index(sortIdx);

        Collection<Order> allOrders = AlgoUtils.searchOrder(search, plugin.getDataCache().getSortedOrders(sortType));
        orders = allOrders.stream().filter(Order::isActive).toList();

        this.amount = ceil_div(orders.size(), 45);
        setupPages();
    }

    private void setupPages() {
        int curr = 0;

        InventoryGUI page = initPage();
        addButtons(page, curr);

        int currentSlotIndex = 0;

        for (final Order order : orders) {
            if (currentSlotIndex == config.mainGUIConfig.orderConfig.slots.size()) {
                currentSlotIndex = 0;
                pages.add(page);
                page = initPage();
                addButtons(page, ++curr);
            }
            page.addItem(order.item(config.mainGUIConfig.orderConfig.lore, e -> {
                HumanEntity who = e.getWhoClicked();
                if (e.getClick() == ClickType.RIGHT && who.hasPermission("orderium.admin.edit-orders")) {
                    Dialog dialog = AdminToolGUI.createEditOrder(order);
                    who.showDialog(dialog);
                    return;
                }
                if (player.getUniqueId().equals(order.getOwnerUniqueId())) {
                    player.sendRichMessage(config.deliverSelf);
                    return;
                }
                InventoryGUI deliverGUI = setupDeliverGUI(order);
                deliverGUI.open(who);
            }), config.mainGUIConfig.orderConfig.slots.get(currentSlotIndex));
            currentSlotIndex++;
        }
        pages.add(page);
    }

    private InventoryGUI initPage() {
        InventoryGUI page = new InventoryGUI(config.mainGUIConfig.rows, mm.deserialize(config.mainGUIConfig.title));
        page.setOnClick(event -> event.setCancelled(true), InteractLocation.GLOBAL);
        page.setOnDrag(event -> event.setCancelled(true), InteractLocation.GLOBAL);
        return page;
    }

    private InventoryGUI setupDeliverGUI(Order order) {
        final ItemStack comparer = order.getItem();
        final InventoryGUI deliverGUI = new InventoryGUI(config.deliverGUIConfig.rows, mm.deserialize(config.deliverGUIConfig.title));

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
                p.getScheduler().run(plugin, task -> p.updateInventory(), null);
                return;
            }

            Dialog dialog = DeliveryConfirmDialog.getDialog(p, order, amount, items);

            PlayerUtils.openDialog(p, dialog);
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
            if (!config.shulkerDelivering || !isShulkerBox(item)) continue;
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

    private void addButtons(InventoryGUI gui, int curr) {
        if (curr > 0)
            gui.addItem(
                    config.mainGUIConfig.backButton.item(e -> PlayerUtils.clickBack(e, pages.get(curr - 1))),
                    config.mainGUIConfig.backButton.slot
            );

        if (curr + 1 < amount)
            gui.addItem(
                    config.mainGUIConfig.nextButton.item(e -> PlayerUtils.clickNext(e, pages.get(curr + 1))),
                    config.mainGUIConfig.nextButton.slot
            );

        gui.addItem(
                config.mainGUIConfig.refreshButton.item(event -> {
                    MainGUI mainGUI = search.isEmpty() ? new MainGUI(player, sortIdx) : new MainGUI(player, sortIdx, search);
                    PlayerUtils.openGUI(player, mainGUI.getGUI(), false);
                    PlayerUtils.playSound(player, config.refreshSound);

                }),
                config.mainGUIConfig.refreshButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.sortButton.item(event -> {
                    MainGUI mainGUI = search.isEmpty() ?
                            new MainGUI(player, sortIdx + 1 == config.mainGUIConfig.sortsOrderConfig.orderArray.size() ? 0 : sortIdx + 1) :
                            new MainGUI(player, sortIdx + 1 == config.mainGUIConfig.sortsOrderConfig.orderArray.size() ? 0 : sortIdx + 1, search);

                    PlayerUtils.openGUI(player, mainGUI.getGUI(), false);

                    PlayerUtils.playSound(player, config.sortSound);

                }, config.mainGUIConfig.sortsOrderConfig.index(sortIdx)),
                config.mainGUIConfig.sortButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.searchButton.item(event -> SignGUI.newSession(
                        player,
                        (s) -> DispatchUtil.entity(player, () -> {
                            MainGUI mainGUI = new MainGUI(player, sortIdx, s);
                            PlayerUtils.openGUI(player, mainGUI.getGUI(), false);
                        }),
                        config.signGUIConfig.signLines, config.signGUIConfig.signType(), config.signGUIConfig.queryLine
                )),
                config.mainGUIConfig.searchButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.yourOrdersButton.item(event -> YourOrderGUI.open(player)),
                config.mainGUIConfig.yourOrdersButton.slot
        );
    }

    public InventoryGUI getGUI() {
        return pages.getFirst();
    }
}
