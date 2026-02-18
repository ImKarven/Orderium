package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecorationAndState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdminToolGUI {

    private static final List<ChestGui> blacklist = new ArrayList<>();
    private static final List<ChestGui> customItems = new ArrayList<>();
    private static DBManager db;

    // Using shared page amount but it's fine I guess
    private static int pageAmount;

    private static final ItemStack next = ItemStack.of(Material.ARROW);
    private static final ItemStack previous = ItemStack.of(Material.ARROW);

    private static final ItemStack readmeBlacklist = ItemStack.of(Material.KNOWLEDGE_BOOK);
    private static final ItemStack readmeCustomItems = ItemStack.of(Material.KNOWLEDGE_BOOK);

    public static void init(Orderium plugin) {
        db = plugin.getDbManager();

        next.editMeta(meta -> {
            meta.displayName(Component.text("Next"));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Click to go to the next page")
            ));
        });

        previous.editMeta(meta -> {
            meta.displayName(Component.text("Back"));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Click to go to the previous page")
            ));
        });

        readmeBlacklist.editMeta(meta -> {
            meta.displayName(Component.text("Blacklist Items", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("View the wiki for usage", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ));
        });

        readmeCustomItems.editMeta(meta -> {
            meta.displayName(Component.text("Custom Items", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("View the wiki for usage", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ));
        });

        createBlacklist();
        createCustomItems();
    }

    public static void createBlacklist() {
        blacklist.clear();

        final Set<ItemStack> items = db.getBlacklistedItems();
        pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        ChestGui page = new ChestGui(6, "Blacklisted Items");
        OutlinePane itemsPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addBlacklistButtons(0, buttonsPane);

        page.setOnGlobalDrag(e -> e.setCancelled(true));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        int cnt = 0, i = 0;

        for (ItemStack item : items) {
            if (cnt == 45) {
                i++;
                page.addPane(itemsPane);
                page.addPane(buttonsPane);
                blacklist.add(page);

                page = new ChestGui(6, "Blacklisted Items");
                itemsPane = new OutlinePane(0, 0, 9, 5);
                buttonsPane = new StaticPane(0, 5, 9, 1);
                addBlacklistButtons(i, buttonsPane);

                page.setOnGlobalDrag(e -> e.setCancelled(true));
                page.setOnGlobalClick(e -> e.setCancelled(true));
            }
            final int currentPage = i;
            itemsPane.addItem(new GuiItem(ConvertUtils.addLore(item.clone(), List.of(
                    "",
                    "<white>Click to <red>remove<white> from blacklist"
            )), e -> {
                db.removeBlacklist(item);
                createBlacklist();
                blacklist.get(Math.min(currentPage, blacklist.size() - 1)).show(e.getWhoClicked());
            }));

            cnt++;
        }

        page.addPane(itemsPane);
        page.addPane(buttonsPane);
        blacklist.add(page);
    }

    public static void createCustomItems() {
        customItems.clear();

        final Set<ItemStack> items = db.getCustomItems();
        pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        ChestGui page = new ChestGui(6, "Custom Items");
        OutlinePane itemsPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addCustomItemsButtons(0, buttonsPane);

        page.setOnGlobalDrag(e -> e.setCancelled(true));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        int cnt = 0, i = 0;

        page.setOnBottomClick(e -> {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            db.addCustomItem(clicked);
            createCustomItems();
            customItems.get(Math.min(0, customItems.size() - 1)).show(e.getWhoClicked());
        });

        for (ItemStack item : items) {
            if (cnt == 45) {
                i++;
                page.addPane(itemsPane);
                page.addPane(buttonsPane);
                customItems.add(page);

                page = new ChestGui(6, "Custom Items");
                itemsPane = new OutlinePane(0, 0, 9, 5);
                buttonsPane = new StaticPane(0, 5, 9, 1);
                addCustomItemsButtons(i, buttonsPane);

                page.setOnGlobalDrag(e -> e.setCancelled(true));
                page.setOnGlobalClick(e -> e.setCancelled(true));

                final int currentPage = i;
                page.setOnBottomClick(e -> {
                    ItemStack clicked = e.getCurrentItem();
                    if (clicked == null) return;

                    db.addCustomItem(clicked);
                    createCustomItems();
                    customItems.get(Math.min(currentPage, customItems.size() - 1)).show(e.getWhoClicked());
                });
            }
            final int currentPage = i;
            itemsPane.addItem(new GuiItem(ConvertUtils.addLore(item.clone(), List.of(
                    "",
                    "<white>Click to <red>remove<white> from custom items list"
            )), e -> {
                db.removeCustomItem(item);
                createCustomItems();
                customItems.get(Math.min(currentPage, customItems.size() - 1)).show(e.getWhoClicked());
            }));

            cnt++;
        }

        page.addPane(itemsPane);
        page.addPane(buttonsPane);
        customItems.add(page);
    }

    public static void openBlacklist(Player p) {
        blacklist.getFirst().show(p);
    }

    public static void openCustomItems(Player p) {
        customItems.getFirst().show(p);
    }

    private static void addBlacklistButtons(int i, final StaticPane pane) {
        if (i > 0) pane.addItem(new GuiItem(previous, e -> blacklist.get(Math.min(i - 1, blacklist.size() - 1)).show(e.getWhoClicked())), 0, 0);
        if (i < pageAmount - 1) pane.addItem(new GuiItem(next, e -> blacklist.get(Math.min(i + 1, blacklist.size() - 1)).show(e.getWhoClicked())), 8, 0);

        pane.addItem(new GuiItem(readmeBlacklist), 4, 0);
    }

    private static void addCustomItemsButtons(int i, final StaticPane pane) {
        if (i > 0) pane.addItem(new GuiItem(previous, e -> customItems.get(Math.min(i - 1, customItems.size() - 1)).show(e.getWhoClicked())), 0, 0);
        if (i < pageAmount - 1) pane.addItem(new GuiItem(next, e -> customItems.get(Math.min(i + 1, customItems.size() - 1)).show(e.getWhoClicked())), 8, 0);

        pane.addItem(new GuiItem(readmeCustomItems), 4, 0);
    }
}
