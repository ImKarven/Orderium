package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Pair;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
    private static Consumer<InventoryClickEvent> addCustomItem(int i) {
        return e -> {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) {
                e.getWhoClicked().sendRichMessage("<red>Null item");
                return;
            }

            if (clicked.isEmpty()) {
                e.getWhoClicked().sendRichMessage("<red>Empty item");
                return;
            }

            db.addCustomItem(clicked);
            createCustomItems();
            customItems.get(Math.min(i, customItems.size() - 1)).show(e.getWhoClicked());
        };

    }


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

    @SuppressWarnings("UnstableApiUsage")
    public static void createCustomItems() {
        customItems.clear();

        final Set<Pair<ItemStack, String>> items = db.getCustomItems();
        pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        ChestGui page = new ChestGui(6, "Custom Items");
        OutlinePane itemsPane = new OutlinePane(0, 0, 9, 5);
        StaticPane buttonsPane = new StaticPane(0, 5, 9, 1);
        addCustomItemsButtons(0, buttonsPane);

        page.setOnGlobalDrag(e -> e.setCancelled(true));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        int cnt = 0, i = 0;

        page.setOnBottomClick(addCustomItem(0));

        for (Pair<ItemStack, String> item : items) {
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

                page.setOnBottomClick(addCustomItem(i));
            }
            final int currentPage = i;
            final GuiItem guiItem = new GuiItem(ConvertUtils.addLore(item.first().clone(), List.of(
                    "",
                    "<white>Left-click to <red>remove<white> from custom items list",
                    "<white>Right-click to <yellow>edit<white> this item"
            )), e -> {
                switch (e.getClick()) {
                    case LEFT -> {
                        db.removeCustomItem(item.first());
                        createCustomItems();
                        customItems.get(Math.min(currentPage, customItems.size() - 1)).show(e.getWhoClicked());
                    }
                    case RIGHT -> {
                        final List<DialogBody> bodies = new LinkedList<>();
                        int j = 1;
                        final String[] searches = item.second().split(",");
                        for (String s : searches) {
                            if (s.isEmpty()) continue;
                            bodies.add(DialogBody.plainMessage(Component.text(j++ + ". " + s)));
                        }
                        bodies.addFirst(DialogBody.item(item.first()).description(DialogBody.plainMessage(Component.text("Search aliases of this custom item:"))).build());

                        final Dialog dialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(Component.text("Edit custom item"))
                                        .body(bodies)
                                        .inputs(
                                                List.of(
                                                        DialogInput.singleOption("choice", Component.text("Action"), List.of(
                                                                SingleOptionDialogInput.OptionEntry.create("add", Component.text("Add Search"), true),
                                                                SingleOptionDialogInput.OptionEntry.create("remove", Component.text("Remove Search"), false)
                                                        )).build(),
                                                        DialogInput.text("text", Component.text("Enter text or number")).build()
                                                )
                                        )
                                        .build()
                                )
                                .type(DialogType.confirmation(
                                        ActionButton.builder(Component.text("Confirm", NamedTextColor.GREEN))
                                                .action(DialogAction.customClick((v, player) -> {
                                                    final String choice = v.getText("choice");
                                                    final String text = v.getText("text");
                                                    if (text == null) return;
                                                    switch (choice) {
                                                        case "add" -> item.second += "," + text.trim().toLowerCase().replaceAll(" ", "_");

                                                        case "remove" -> {
                                                            final String[] indices = text.trim().split(",");
                                                            final List<String> toRev = new ArrayList<>();
                                                            for (String index : indices) {
                                                                try {
                                                                    toRev.add(searches[Integer.parseInt(index)]);
                                                                } catch (Exception ignored) {}
                                                            }
                                                            final List<String> searchList = new ArrayList<>(List.of(searches));
                                                            searchList.removeAll(toRev);
                                                            item.second = String.join(",", searchList);
                                                        }

                                                        case null, default -> {}
                                                    }
                                                    db.updateCustomItemSearch(  item);
                                                }, ClickCallback.Options.builder().build()))
                                                .build(),
                                        ActionButton.builder(Component.text("Cancel", NamedTextColor.RED)).build()
                                ))

                        );
                        e.getWhoClicked().showDialog(dialog);
                    }
                }
            });

            itemsPane.addItem(guiItem);

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
