package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.google.common.collect.ImmutableList;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

import static me.karven.orderium.load.Orderium.plugin;

public class AdminToolGUI {

    private static final List<ChestGui> blacklist = new ArrayList<>();
    private static final List<ChestGui> customItems = new ArrayList<>();

    // Using shared page amount but it's fine I guess
    private static int pageAmount;

    private static final Consumer<InventoryClickEvent> viewWiki = e -> {
        e.getWhoClicked().closeInventory();
        e.getWhoClicked().sendRichMessage("<gray>>> <blue><u><click:open_url:'https://github.com/ImKarven/Orderium/wiki/Blacklist-&-Custom-items'>Click here</click></u> <white>to view the wiki");
    };

    private static final ItemStack next = ItemStack.of(Material.ARROW);
    private static final ItemStack previous = ItemStack.of(Material.ARROW);

    private static final ItemStack readmeBlacklist = ItemStack.of(Material.KNOWLEDGE_BOOK);
    private static final ItemStack readmeCustomItems = ItemStack.of(Material.KNOWLEDGE_BOOK);

    private static final GuiItem itemRmBlacklist = new GuiItem(readmeBlacklist, viewWiki);
    private static final GuiItem itemRmCustomItems = new GuiItem(readmeCustomItems, viewWiki);

    private static Consumer<InventoryClickEvent> addCustomItem(int i) {
        return e -> {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.isEmpty()) return;
            CustomItem customItem = new CustomItem(clicked.serializeAsBytes());
            plugin.getStorage().addCustomItem(customItem);
            plugin.getDataCache().getCustomItems().add(customItem);
            createCustomItems();
            customItems.get(Math.min(i, customItems.size() - 1)).show(e.getWhoClicked());
        };

    }


    public static void init() {
        next.editMeta(meta -> {
            meta.displayName(nameDeco("Next"));
            meta.lore(List.of(
                    Component.empty(),
                    loreDeco("Click to go to the next page")
            ));
        });

        previous.editMeta(meta -> {
            meta.displayName(nameDeco("Back"));
            meta.lore(List.of(
                    Component.empty(),
                    loreDeco("Click to go to the previous page")
            ));
        });

        readmeBlacklist.editMeta(meta -> {
            meta.displayName(nameDeco("Blacklist Items"));
            meta.lore(List.of(
                    Component.empty(),
                    loreDeco("View the wiki for usage")
            ));
        });

        readmeCustomItems.editMeta(meta -> {
            meta.displayName(nameDeco("Custom Items"));
            meta.lore(List.of(
                    Component.empty(),
                    loreDeco("View the wiki for usage")
            ));
        });

        createBlacklist();
        createCustomItems();
    }

    private static Component nameDeco(String name) {
        return Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
    }

    private static Component loreDeco(String lore) {
        return Component.text(lore, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
    }

    public static void createBlacklist() {
        blacklist.clear();

        final Set<BlacklistedItem> items = plugin.getDataCache().getBlacklist();
        pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        ChestGui page = new ChestGui(6, "Blacklisted Items");
        OutlinePane itemsPane = new OutlinePane(9, 5);
        StaticPane buttonsPane = new StaticPane(9, 1);
        addBlacklistButtons(0, buttonsPane);

        page.setOnGlobalDrag(e -> e.setCancelled(true));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        int cnt = 0, i = 0;

        for (BlacklistedItem blacklistedItem : items) {
            if (cnt == 45) {
                cnt = 0;
                i++;
                page.addPane(Slot.fromXY(0, 0), itemsPane);
                page.addPane(Slot.fromXY(0, 5), buttonsPane);
                blacklist.add(page);

                page = new ChestGui(6, "Blacklisted Items");
                itemsPane = new OutlinePane(9, 5);
                buttonsPane = new StaticPane(9, 1);
                addBlacklistButtons(i, buttonsPane);

                page.setOnGlobalDrag(e -> e.setCancelled(true));
                page.setOnGlobalClick(e -> e.setCancelled(true));
            }
            final int currentPage = i;
            itemsPane.addItem(new GuiItem(ConvertUtils.addLore(blacklistedItem.getItemStack(), List.of(
                    "",
                    "<white>Click to <red>remove<white> from blacklist"
            )), e -> {
                plugin.getStorage().removeBlacklist(blacklistedItem);
                createBlacklist();
                blacklist.get(Math.min(currentPage, blacklist.size() - 1)).show(e.getWhoClicked());
            }));

            cnt++;
        }

        page.addPane(Slot.fromXY(0, 0), itemsPane);
        page.addPane(Slot.fromXY(0, 5), buttonsPane);
        blacklist.add(page);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void createCustomItems() {
        customItems.clear();

        final Set<CustomItem> items = plugin.getDataCache().getCustomItems();
        pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        ChestGui page = new ChestGui(6, "Custom Items");
        OutlinePane itemsPane = new OutlinePane(9, 5);
        StaticPane buttonsPane = new StaticPane(9, 1);
        addCustomItemsButtons(0, buttonsPane);

        page.setOnGlobalDrag(e -> e.setCancelled(true));
        page.setOnGlobalClick(e -> e.setCancelled(true));
        int cnt = 0, i = 0;

        page.setOnBottomClick(addCustomItem(0));

        for (CustomItem item : items) {
            if (cnt == 45) {
                cnt = 0;
                i++;
                page.addPane(Slot.fromXY(0, 0), itemsPane);
                page.addPane(Slot.fromXY(0, 5), buttonsPane);
                customItems.add(page);

                page = new ChestGui(6, "Custom Items");
                itemsPane = new OutlinePane(9, 5);
                buttonsPane = new StaticPane(9, 1);
                addCustomItemsButtons(i, buttonsPane);

                page.setOnGlobalDrag(e -> e.setCancelled(true));
                page.setOnGlobalClick(e -> e.setCancelled(true));

                page.setOnBottomClick(addCustomItem(i));
            }
            final int currentPage = i;
            ItemStack stack = item.getItemStack();
            final GuiItem guiItem = new GuiItem(ConvertUtils.addLore(stack.clone(), List.of(
                    "",
                    "<white>Shift-right-click to <red>remove<white> from custom items list",
                    "<white>Left-click to <yellow>edit<white> this item",
                    "<white>Right-click to <aqua>get<white> this item"
            )), e -> {
                switch (e.getClick()) {
                    case RIGHT -> {
                        if (e.getWhoClicked() instanceof Player player)
                            PlayerUtils.give(player, stack.clone(), false);
                    }

                    case SHIFT_RIGHT -> {
                        plugin.getStorage().removeCustomItem(item);
                        items.remove(item);
                        createCustomItems();
                        customItems.get(Math.min(currentPage, customItems.size() - 1)).show(e.getWhoClicked());
                    }
                    case LEFT -> {
                        final List<DialogBody> bodies = new LinkedList<>();
                        int j = 1;
                        final ImmutableList<String> searches = item.getSearches();
                        for (String s : searches) {
                            if (s.isEmpty()) continue;
                            bodies.add(DialogBody.plainMessage(Component.text(j++ + ". " + s)));
                        }
                        String noneText = searches.isEmpty() ? " None" : "";
                        bodies.addFirst(DialogBody.item(stack).description(DialogBody.plainMessage(Component.text("Search aliases of this custom item:" + noneText))).build());

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
                                                .action(DialogAction.customClick((v, _) -> {
                                                    final String choice = v.getText("choice");
                                                    final String text = v.getText("text");
                                                    if (text == null) return;
                                                    switch (choice) {
                                                        case "add" -> item.addAllSearches(Arrays.asList(text.trim().toLowerCase().replace(" ", "_").split(",")));

                                                        case "remove" -> {
                                                            String[] indicesString = text.trim().split(",");
                                                            final List<String> toRev = new ArrayList<>();
                                                            for (String index : indicesString) {
                                                                try {
                                                                    toRev.add(searches.get(Integer.parseInt(index)));
                                                                } catch (Exception ignored) {} // User didn't input a valid index or number
                                                            }
                                                            item.removeAllSearches(toRev);
                                                        }

                                                        case null, default -> {}
                                                    }
                                                    plugin.getStorage().updateCustomItemSearch(item);
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

        page.addPane(Slot.fromXY(0, 0), itemsPane);
        page.addPane(Slot.fromXY(0, 5), buttonsPane);
        customItems.add(page);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Dialog createEditOrder(Order order) {

        DialogBody body = DialogBody.item(order.getItem()).description(DialogBody.plainMessage(Component.text("You're editing this order"))).build();

        DialogInput option = DialogInput.singleOption("option", Component.text("Action"), List.of(
                SingleOptionDialogInput.OptionEntry.create("change_amount", Component.text("Change Amount"), true),
                SingleOptionDialogInput.OptionEntry.create("change_delivered", Component.text("Change Delivered"), false),
                SingleOptionDialogInput.OptionEntry.create("change_in_storage", Component.text("Change In Storage"), false),
                SingleOptionDialogInput.OptionEntry.create("change_money_per", Component.text("Change Money Per"), false)
        )).build();

        DialogInput value = DialogInput.text("value", Component.text("Value")).build();

        ActionButton confirm = ActionButton.builder(Component.text("Confirm", NamedTextColor.GREEN))
                .action(DialogAction.customClick((view, player) -> {
                    if (!(player instanceof Player p)) return;
                    String chosen = view.getText("option");
                    double num = ConvertUtils.formatNumber(view.getText("value"));
                    final int intNum = (int) num;
                    if (num == -1 || chosen == null || (!chosen.equals("change_money_per") && num != intNum)) {
                        p.sendRichMessage("<red>Invalid value");
                        return;
                    }
                    switch (chosen) {
                        case "change_amount" -> order.setAmount(intNum);
                        case "change_delivered" -> order.setDelivered(intNum);
                        case "change_in_storage" -> order.setInStorage(intNum);
                        case "change_money_per" -> order.setMoneyPer(num);
                        default -> {
                            p.sendRichMessage("<red>Failed to set value");
                            return;
                        }
                    }

                    p.sendRichMessage("<green>Successful");
                }, ClickCallback.Options.builder().build()))
                .build();

        ActionButton cancel = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED)).build();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit this order"))
                        .body(List.of(body))
                        .inputs(List.of(option, value))
                        .build()
                )
                .type(DialogType.confirmation(confirm, cancel))
        );
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

        pane.addItem(itemRmBlacklist, 4, 0);
    }

    private static void addCustomItemsButtons(int i, final StaticPane pane) {
        if (i > 0) pane.addItem(new GuiItem(previous, e -> customItems.get(Math.min(i - 1, customItems.size() - 1)).show(e.getWhoClicked())), 0, 0);
        if (i < pageAmount - 1) pane.addItem(new GuiItem(next, e -> customItems.get(Math.min(i + 1, customItems.size() - 1)).show(e.getWhoClicked())), 8, 0);

        pane.addItem(itemRmCustomItems, 4, 0);
    }
}
