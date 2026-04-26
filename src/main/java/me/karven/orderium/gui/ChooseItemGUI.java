package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.EnchantableItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.SearchableItem;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PDCUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static me.karven.orderium.load.Orderium.plugin;

public class ChooseItemGUI {
    private static final List<ChestGui> AZ = new ArrayList<>();
    private static final List<ChestGui> ZA = new ArrayList<>();
    private static MiniMessage mm;
    private static ConfigCache cache;

    public static void init() {
        mm = plugin.mm;
        cache = plugin.getConfigs();

        AZ.clear();
        ZA.clear();

        createPages(AZ, SortTypes.A_Z);
        createPages(ZA, SortTypes.Z_A);
    }

    private static List<ChestGui> getPages(SortTypes sortType) {
        switch (sortType) {
            case A_Z -> { return AZ; }
            case Z_A -> { return ZA; }
        }
        return AZ;
    }

    public static void choose(Player p, int sortIdx, int pageIdx) {
        PlayerUtils.openGui(p, getPages(cache.getChooseSortsOrder().get(sortIdx)).get(pageIdx));
    }

    public static void choose(Player p, int sortIdx, String search) {
        final List<ChestGui> pages = new ArrayList<>();
        createPages(pages, cache.getChooseSortsOrder().get(sortIdx), search);
        PlayerUtils.openGui(p, pages.getFirst());
    }

    private static void addButtons(StaticPane buttons, List<ChestGui> pages, SortTypes sortType, final int idx, final int pagesAmount) {
        final List<SortTypes> sortOrder = cache.getChooseSortsOrder();
        final int sortIdx = sortOrder.indexOf(sortType);
        if (idx > 0) buttons.addItem(ConvertUtils.parseButton(
                cache.getChooseBackButton(),
                e -> PlayerUtils.clickBack(e, pages.get(idx - 1))
        ), cache.getChooseBackButton().getSlot(), 0);

        if (idx + 1 < pagesAmount) buttons.addItem(ConvertUtils.parseButton(
                cache.getChooseNextButton(),
                e -> PlayerUtils.clickNext(e, pages.get(idx + 1))
        ), cache.getChooseNextButton().getSlot(), 0);

        buttons.addItem(ConvertUtils.parseSortButton(cache.getChooseSortButton(), sortType, e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            final int nextIdx = sortIdx == sortOrder.size() - 1 ? 0 : sortIdx + 1;
            choose(p, nextIdx, idx);
            PlayerUtils.playSound(p, cache.getSortSound());
        }), cache.getChooseSortButton().getSlot(), 0);

        buttons.addItem(ConvertUtils.parseButton(cache.getChooseSearchButton(), e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            SignGUI.newSession(
                    p,
                    (s) -> p.getScheduler().run(plugin, _ -> ChooseItemGUI.choose(p, sortIdx, s), null),
                    cache.getLines(),
                    cache.getSignBlock(),
                    cache.getSearchLine()
            );

        }), cache.getChooseSearchButton().getSlot(), 0);
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType) {
        createPages(pages, sortType, plugin.getDataCache().getItems(sortType));
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType, String search) {
        if (search.isEmpty()) {
            createPages(pages, sortType);
            return;
        }

        final List<OrderItem> items = AlgoUtils.searchItem(search, plugin.getDataCache().getItems(sortType));
        createPages(pages, sortType, items);
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType, Collection<OrderItem> items) {
        final int pagesAmount = ConvertUtils.ceil_div(items.size(), 45);

        OutlinePane itemsPane = new OutlinePane(9, 5);
        StaticPane buttonsPane = new StaticPane(9, 1);
        addButtons(buttonsPane, pages, sortType, 0, pagesAmount);
        ChestGui currPage = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));
        currPage.setOnGlobalClick(e -> e.setCancelled(true));
        currPage.setOnGlobalDrag(e -> e.setCancelled(true));
        int idx = 0, cnt = 0;
        for (OrderItem orderItem : items) {
            if (cnt == 45) {
                cnt = 0;
                idx++;
                currPage.addPane(Slot.fromXY(0, 0), itemsPane);
                currPage.addPane(Slot.fromXY(0, 5), buttonsPane);
                pages.add(currPage);

                itemsPane = new OutlinePane(9, 5);
                buttonsPane = new StaticPane(9, 1);
                addButtons(buttonsPane, pages, sortType, idx, pagesAmount);
                currPage = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));
                currPage.setOnGlobalClick(e -> e.setCancelled(true));
                currPage.setOnGlobalDrag(e -> e.setCancelled(true));
            }
            ItemStack item = orderItem instanceof SearchableItem searchableItem ? searchableItem.getParsedItemStack() : orderItem.getItemStack();
            final GuiItem guiItem = new GuiItem(orderItem.getItemStack());
            guiItem.setAction(e -> {
                if (!(e.getWhoClicked() instanceof Player p)) return;
                if (e.getClick() != ClickType.RIGHT || !p.hasPermission("orderium.admin.blacklist")) {

                    if (
                            !cache.isEnchantItem() ||
                            !(orderItem instanceof EnchantableItem enchantableItem)
                    ) {
                        NewOrderDialog.newSession(p, orderItem);
                        return;
                    }
                    new EnchantGUI(p, enchantableItem, (enchantedItem) -> NewOrderDialog.newSession(p, enchantedItem));
                    return;
                }
                final ItemStack i = guiItem.getItem();
                if (PDCUtils.isBlacklist(i.getItemMeta())) return;

                plugin.getStorage().addBlacklist(new BlacklistedItem(orderItem.getItemStack().serializeAsBytes()));
                p.sendRichMessage("<green>Item added to blacklist. Reload to take effects");
                i.editMeta(PDCUtils::setBlacklist);
            });
            itemsPane.addItem(guiItem);

            cnt++;
        }

        currPage.addPane(Slot.fromXY(0, 0), itemsPane);
        currPage.addPane(Slot.fromXY(0, 5), buttonsPane);
        pages.add(currPage);
    }
}
