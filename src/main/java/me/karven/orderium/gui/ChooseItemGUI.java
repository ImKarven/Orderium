package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
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

import static me.karven.orderium.config.ConfigCache.cache;
import static me.karven.orderium.load.Orderium.plugin;

public class ChooseItemGUI {
    private static final List<InventoryGUI> AZ = new ArrayList<>();
    private static final List<InventoryGUI> ZA = new ArrayList<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void init() {
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

    public static InventoryGUI getGUI(int sortIdx, int pageIdx) {
        return getPages(cache.chooseSortsOrder.get(sortIdx)).get(pageIdx);
    }

    public static InventoryGUI getGUI(int sortIdx, String search) {
        final List<InventoryGUI> pages = new ArrayList<>();
        createPages(pages, cache.chooseSortsOrder.get(sortIdx), search);
        return pages.getFirst();
    }

    private static void addButtons(InventoryGUI gui, List<InventoryGUI> pages, SortTypes sortType, final int idx, final int pagesAmount) {
        final List<SortTypes> sortOrder = cache.chooseSortsOrder;
        final int sortIdx = sortOrder.indexOf(sortType);

        if (idx > 0) gui.addItem(ConvertUtils.parseNewButton(
                cache.chooseBackButton,
                e -> PlayerUtils.clickBack(e, pages.get(idx - 1))
        ), cache.chooseBackButton.getSlot() + 45);

        if (idx + 1 < pagesAmount) gui.addItem(ConvertUtils.parseNewButton(
                cache.chooseNextButton,
                e -> PlayerUtils.clickNext(e, pages.get(idx + 1))
        ), cache.chooseNextButton.getSlot() + 45);

        gui.addItem(ConvertUtils.parseSortButton(cache.chooseSortButton, sortType, e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            final int nextIdx = sortIdx == sortOrder.size() - 1 ? 0 : sortIdx + 1;
            PlayerUtils.openGUI(p, getGUI(nextIdx, idx), false);
            PlayerUtils.playSound(p, cache.sortSound);
        }), cache.chooseSortButton.getSlot() + 45);

        gui.addItem(ConvertUtils.parseNewButton(cache.chooseSearchButton, e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            SignGUI.newSession(
                    p,
                    (s) -> PlayerUtils.openGUI(p, ChooseItemGUI.getGUI(sortIdx, s), true),
                    cache.lines,
                    cache.signBlock,
                    cache.searchLine
            );

        }), cache.chooseSearchButton.getSlot() + 45);
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
                            !cache.enchantItem ||
                            !(orderItem instanceof EnchantableItem enchantableItem)
                    ) {
                        Dialog dialog = NewOrderDialog.getDialog(orderItem);
                        PlayerUtils.openDialog(p, dialog);
                        return;
                    }
                    EnchantGUI enchantGUI = new EnchantGUI(enchantableItem, (enchantedItem) -> {
                        Dialog dialog = NewOrderDialog.getDialog(enchantedItem);
                        PlayerUtils.openDialog(p, dialog);
                    });
                    InventoryGUI inventoryEnchantGUI = enchantGUI.getGUI();
                    if (inventoryEnchantGUI != null) PlayerUtils.openGUI(p, inventoryEnchantGUI, false);
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
        InventoryGUI gui = new InventoryGUI(6, mm.deserialize(cache.chooseItemTitle));
        addButtons(gui, pages, sortType, idx, pagesAmount);
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        return gui;
    }
}
