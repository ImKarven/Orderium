package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.NMSUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChooseItemGUI {
    private static final List<ChestGui> AZ = new ArrayList<>();
    private static final List<ChestGui> ZA = new ArrayList<>();
    private static Orderium plugin;
    private static MiniMessage mm;
    private static ConfigManager cache;
    private static int pagesAmount;

    public static void init(Orderium plugin) {
        ChooseItemGUI.plugin = plugin;
        mm = plugin.mm;
        cache = plugin.getConfigs();
        final int itemsAmount = NMSUtils.getItems(SortTypes.A_Z).size();

        pagesAmount = ConvertUtils.ceil_div(itemsAmount, 45);

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

    private static void addButtons(StaticPane buttons, SortTypes sortType, final int idx, final int pagesAmount) {
        final List<SortTypes> sortOrder = cache.getChooseSortsOrder();
        final int sortIdx = sortOrder.indexOf(sortType);
        if (idx > 0) buttons.addItem(ConvertUtils.parseButton(cache.getChooseBackButton(), e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            PlayerUtils.openGui(p, getPages(sortType).get(idx - 1));
        }), cache.getChooseBackButton().getSlot(), 0);

        if (idx + 1 < pagesAmount) buttons.addItem(ConvertUtils.parseButton(cache.getChooseNextButton(), e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            PlayerUtils.openGui(p, getPages(sortType).get(idx + 1));
        }), cache.getChooseNextButton().getSlot(), 0);

        buttons.addItem(ConvertUtils.parseSortButton(cache.getChooseSortButton(), sortType, e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            final int nextIdx = sortIdx == sortOrder.size() - 1 ? 0 : sortIdx + 1;
            choose(p, nextIdx, idx);
        }), cache.getChooseSortButton().getSlot(), 0);

        buttons.addItem(ConvertUtils.parseButton(cache.getChooseSearchButton(), e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            SignGUI.newSession(
                    p,
                    (s) -> p.getScheduler().run(plugin, t -> ChooseItemGUI.choose(p, sortIdx, s), null),
                    cache.getLines(),
                    cache.getSignBlock(),
                    cache.getSearchLine()
            );

        }), cache.getChooseSearchButton().getSlot(), 0);
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType) {
        createPages(pages, sortType, NMSUtils.getItems(sortType));
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType, String search) {
        if (search.isEmpty()) {
            createPages(pages, sortType);
            return;
        }

        final List<ItemStack> items = AlgoUtils.searchItem(search, NMSUtils.getItems(sortType));
        createPages(pages, sortType, items);
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType, Collection<ItemStack> items) {
        final int pagesAmount = ConvertUtils.ceil_div(items.size(), 45);

        OutlinePane itemsPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addButtons(buttonsPane, sortType, 0, pagesAmount);
        ChestGui currPage = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));
        currPage.setOnGlobalClick(e -> e.setCancelled(true));
        currPage.setOnGlobalDrag(e -> e.setCancelled(true));
        int idx = 0, cnt = 0;
        for (final ItemStack item : items) {
            if (cnt == 45) {
                cnt = 0;
                idx++;
                currPage.addPane(itemsPane);
                currPage.addPane(buttonsPane);
                pages.add(currPage);

                itemsPane = new OutlinePane(0, 0, 9, 5);
                buttonsPane = new StaticPane(0, 5, 9, 1);
                addButtons(buttonsPane, sortType, idx, pagesAmount);
                currPage = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));
                currPage.setOnGlobalClick(e -> e.setCancelled(true));
                currPage.setOnGlobalDrag(e -> e.setCancelled(true));
            }
            itemsPane.addItem(new GuiItem(item.clone(), e -> {
                e.setCancelled(true);
                if (!(e.getWhoClicked() instanceof Player p)) return;
                NewOrderDialog.newSession(p, item.clone());
            }));

            cnt++;
        }

        currPage.addPane(itemsPane);
        currPage.addPane(buttonsPane);
        pages.add(currPage);
    }
}
