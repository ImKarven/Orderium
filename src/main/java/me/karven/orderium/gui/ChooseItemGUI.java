package me.karven.orderium.gui;

import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.EnchantableItem;
import me.karven.orderium.obj.orderitem.OrderItem;
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
    private static final List<InventoryGUI> AZ = new ArrayList<>();
    private static final List<InventoryGUI> ZA = new ArrayList<>();
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

    private static List<InventoryGUI> getPages(SortTypes sortType) {
        switch (sortType) {
            case A_Z -> { return AZ; }
            case Z_A -> { return ZA; }
        }
        return AZ;
    }

    public static InventoryGUI choose(int sortIdx, int pageIdx) {
        return getPages(cache.getChooseSortsOrder().get(sortIdx)).get(pageIdx);
    }

    public static InventoryGUI choose(int sortIdx, String search) {
        final List<InventoryGUI> pages = new ArrayList<>();
        createPages(pages, cache.getChooseSortsOrder().get(sortIdx), search);
        return pages.getFirst();
    }

    private static void addButtons(InventoryGUI gui, List<InventoryGUI> pages, SortTypes sortType, final int idx, final int pagesAmount) {
        final List<SortTypes> sortOrder = cache.getChooseSortsOrder();
        final int sortIdx = sortOrder.indexOf(sortType);

        if (idx > 0) gui.addItem(ConvertUtils.parseNewButton(
                cache.getChooseBackButton(),
                e -> PlayerUtils.clickBack(e, pages.get(idx - 1))
        ), cache.getChooseBackButton().getSlot() + 45);

        if (idx + 1 < pagesAmount) gui.addItem(ConvertUtils.parseNewButton(
                cache.getChooseNextButton(),
                e -> PlayerUtils.clickNext(e, pages.get(idx + 1))
        ), cache.getChooseNextButton().getSlot() + 45);

        gui.addItem(ConvertUtils.parseSortButton(cache.getChooseSortButton(), sortType, e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            final int nextIdx = sortIdx == sortOrder.size() - 1 ? 0 : sortIdx + 1;
            PlayerUtils.openGUI(p, choose(nextIdx, idx), false);
            PlayerUtils.playSound(p, cache.getSortSound());
        }), cache.getChooseSortButton().getSlot() + 45);

        gui.addItem(ConvertUtils.parseNewButton(cache.getChooseSearchButton(), e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            SignGUI.newSession(
                    p,
                    (s) -> PlayerUtils.openGUI(p, ChooseItemGUI.choose(sortIdx, s), true),
                    cache.getLines(),
                    cache.getSignBlock(),
                    cache.getSearchLine()
            );

        }), cache.getChooseSearchButton().getSlot() + 45);
    }

    private static void createPages(List<InventoryGUI> pages, SortTypes sortType) {
        createPages(pages, sortType, plugin.getDataCache().getItems(sortType));
    }

    private static void createPages(List<InventoryGUI> pages, SortTypes sortType, String search) {
        if (search.isEmpty()) {
            createPages(pages, sortType);
            return;
        }

        final List<OrderItem> items = AlgoUtils.searchItem(search, plugin.getDataCache().getItems(sortType));
        createPages(pages, sortType, items);
    }

    private static void createPages(List<InventoryGUI> pages, SortTypes sortType, Collection<OrderItem> items) {
        final int pagesAmount = ConvertUtils.ceil_div(items.size(), 45);

        InventoryGUI gui = initPage(pages, sortType, 0, pagesAmount);
        int idx = 0, cnt = 0;
        for (OrderItem orderItem : items) {
            if (cnt == 45) {
                cnt = 0;
                idx++;
                pages.add(gui);
                gui = initPage(pages, sortType, idx, pagesAmount);
            }
            final InventoryItem guiItem = new InventoryItem(orderItem.getItemStack());
            guiItem.setOnClick(e -> {
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
                final ItemStack guiItemStack = guiItem.getItem();
                if (PDCUtils.isBlacklist(guiItemStack.getItemMeta())) return;
                ItemStack orderItemStack = orderItem.getItemStack();
                plugin.getStorage().addBlacklist(new BlacklistedItem(orderItemStack.serializeAsBytes(), orderItemStack));
                p.sendRichMessage("<green>Item added to blacklist. Reload to take effects");
                guiItemStack.editMeta(PDCUtils::setBlacklist);
            });
            gui.addItem(guiItem, cnt);

            cnt++;
        }
        pages.add(gui);
    }

    private static InventoryGUI initPage(List<InventoryGUI> pages, SortTypes sortType, int idx, int pagesAmount) {
        InventoryGUI gui = new InventoryGUI(6, mm.deserialize(cache.getChooseItemTitle()));
        addButtons(gui, pages, sortType, idx, pagesAmount);
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        return gui;
    }
}
