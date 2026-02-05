package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SortTypes;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.NMSUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChooseItemGUI {
//    private static final List<ChestGui> pages = new ArrayList<>();
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
        final int itemsAmount = NMSUtils.getItemsList().size();

        // This wouldn't happen (hopefully) so we don't need to care
//        if (itemsAmount == 0) {
//            pagesAmount = -1;
//            final StaticPane buttons = new StaticPane(0, 5, 9, 1);
//            addButtons(buttons, 0, );
//            final ChestGui page = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));
//            page.addPane(buttons);
//            pages.add(page);
//            AZ.add(page);
//            ZA.add(page);
//            return;
//        }
        pagesAmount = ConvertUtils.ceil_div(itemsAmount, 45);

        createPages(AZ, SortTypes.A_Z);
        createPages(ZA, SortTypes.Z_A);
    }




    public static void choose(Player p, int sortIdx, int pageIdx) {
        switch (cache.getChooseSortsOrder().get(sortIdx)) {
            case A_Z -> AZ.get(pageIdx).show(p);
            case Z_A -> ZA.get(pageIdx).show(p);
        }
    }

    private static void addButtons(StaticPane buttons, SortTypes sortType, final int idx, final List<ChestGui> pages) {

        if (idx > 0) buttons.addItem(ConvertUtils.parseButton(cache.getChooseBackButton(), e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            pages.get(idx - 1).show(p);
        }), cache.getChooseBackButton().getSlot(), 0);

        if (idx + 1 < pagesAmount) buttons.addItem(ConvertUtils.parseButton(cache.getChooseNextButton(), e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            pages.get(idx + 1).show(p);
        }), cache.getChooseNextButton().getSlot(), 0);

        buttons.addItem(ConvertUtils.parseSortButton(cache.getChooseSortButton(), sortType, e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            final List<SortTypes> sortOrder = cache.getChooseSortsOrder();
            int nextIdx = sortOrder.indexOf(sortType);
            if (nextIdx == sortOrder.size() - 1) nextIdx = 0;
            else nextIdx++;

            choose(p, nextIdx, idx);
        }), cache.getChooseSearchButton().getSlot(), 0);

        buttons.addItem(ConvertUtils.parseButton(cache.getChooseSearchButton(), e -> {
            e.setCancelled(true);
            // TODO: Add search

        }), cache.getChooseSearchButton().getSlot(), 0);
    }

    private static void createPages(List<ChestGui> pages, SortTypes sortType) {
        OutlinePane itemsPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addButtons(buttonsPane, sortType, 0, pages);
        ChestGui currPage = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));
        int idx = 0, cnt = 0;
        for (final ItemStack item : NMSUtils.getItems(sortType)) {
            if (cnt == 45) {
                cnt = 0;
                currPage.addPane(itemsPane);
                currPage.addPane(buttonsPane);
                pages.add(currPage);

                itemsPane = new OutlinePane(0, 0, 9, 5);
                buttonsPane = new StaticPane(0, 5, 9, 1);
                addButtons(buttonsPane, sortType, idx + 1, pages);
                currPage = new ChestGui(6, ComponentHolder.of(mm.deserialize(cache.getChooseItemTitle())));

                idx++;
            }
            itemsPane.addItem(new GuiItem(item, e -> {
                e.setCancelled(true);
                if (!(e.getWhoClicked() instanceof Player p)) return;
                NewOrderDialog.newSession(p, item);
            }));

            cnt++;
        }

        currPage.addPane(itemsPane);
        currPage.addPane(buttonsPane);
        pages.add(currPage);
    }
}
