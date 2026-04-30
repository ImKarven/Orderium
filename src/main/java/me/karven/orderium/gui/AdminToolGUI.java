package me.karven.orderium.gui;

import com.google.common.collect.ImmutableList;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.InventoryItem;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static me.karven.orderium.load.Orderium.plugin;

public class AdminToolGUI {
    private static final List<InventoryGUI> blacklist = new CopyOnWriteArrayList<>();
    private static final List<InventoryGUI> customItems = new CopyOnWriteArrayList<>();

    private static final Consumer<InventoryClickEvent> viewWiki = e -> {
        e.getWhoClicked().closeInventory();
        e.getWhoClicked().sendRichMessage("<gray>>> <blue><u><click:open_url:'https://github.com/ImKarven/Orderium/wiki/Blacklist-&-Custom-items'>Click here</click></u> <white>to view the wiki");
    };

    private static final ItemStack next = ItemStack.of(Material.ARROW);
    private static final ItemStack previous = ItemStack.of(Material.ARROW);

    private static final ItemStack readmeBlacklist = ItemStack.of(Material.KNOWLEDGE_BOOK);
    private static final ItemStack readmeCustomItems = ItemStack.of(Material.KNOWLEDGE_BOOK);

    private static final InventoryItem itemRmBlacklist = new InventoryItem(readmeBlacklist, viewWiki);
    private static final InventoryItem itemRmCustomItems = new InventoryItem(readmeCustomItems, viewWiki);

    private static Consumer<InventoryClickEvent> addCustomItem(int i) {
        return e -> {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.isEmpty()) return;
            CustomItem customItem = new CustomItem(clicked.serializeAsBytes());
            plugin.getStorage().addCustomItem(customItem);
            plugin.getDataCache().getCustomItems().add(customItem);
            createCustomItems();
            PlayerUtils.openGUI(e.getWhoClicked(), customItems.get(Math.min(i, customItems.size() - 1)), false);
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
        int pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        InventoryGUI page = new InventoryGUI(6, Component.text("Blacklisted Items"));
        addBlacklistButtons(0, pageAmount, page);

        page.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        page.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        int cnt = 0, i = 0;

        for (BlacklistedItem blacklistedItem : items) {
            if (cnt == 45) {
                cnt = 0;
                i++;
                blacklist.add(page);

                page = new InventoryGUI(6, Component.text("Blacklisted Items"));
                addBlacklistButtons(i, pageAmount, page);

                page.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
                page.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
            }
            final int currentPage = i;
            page.addItem(new InventoryItem(ConvertUtils.addLore(blacklistedItem.getItemStack(), List.of(
                    "",
                    "<white>Click to <red>remove<white> from blacklist"
            )), e -> {
                items.remove(blacklistedItem);
                plugin.getStorage().removeBlacklist(blacklistedItem);

                createBlacklist();
                PlayerUtils.openGUI(e.getWhoClicked(), blacklist.get(Math.min(currentPage, blacklist.size() - 1)), false);
            }), cnt++);
        }
        blacklist.add(page);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void createCustomItems() {
        customItems.clear();

        final Set<CustomItem> items = plugin.getDataCache().getCustomItems();
        int pageAmount = ConvertUtils.ceil_div(items.size(), 45);

        InventoryGUI page = new InventoryGUI(6, Component.text("Custom Items"));
        addCustomItemsButtons(0, pageAmount, page);

        page.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        page.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);

        page.setOnClick(addCustomItem(0), InteractLocation.BOTTOM);

        int cnt = 0, i = 0;
        for (CustomItem item : items) {
            if (cnt == 45) {
                cnt = 0;
                i++;
                customItems.add(page);

                page = new InventoryGUI(6, Component.text("Custom Items"));
                addCustomItemsButtons(i, pageAmount, page);

                page.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
                page.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);

                page.setOnClick(addCustomItem(i), InteractLocation.BOTTOM);
            }
            final int currentPage = i;
            ItemStack stack = item.getItemStack();
            final InventoryItem guiItem = new InventoryItem(ConvertUtils.addLore(stack.clone(), List.of(
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
                        PlayerUtils.openGUI(e.getWhoClicked(), customItems.get(Math.min(currentPage, customItems.size() - 1)), false);
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

            page.addItem(guiItem, cnt++);
        }
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
        PlayerUtils.openGUI(p, blacklist.getFirst(), true);
    }

    public static void openCustomItems(Player p) {
        PlayerUtils.openGUI(p, customItems.getFirst(), true);
    }

    private static void addBlacklistButtons(int i, int pageAmount, final InventoryGUI gui) {
        if (i > 0) gui.addItem(new InventoryItem(previous, e -> PlayerUtils.openGUI(e.getWhoClicked(), blacklist.get(Math.min(i - 1, blacklist.size() - 1)), false)), 45);
        if (i < pageAmount - 1) gui.addItem(new InventoryItem(next, e -> PlayerUtils.openGUI(e.getWhoClicked(), blacklist.get(Math.min(i + 1, blacklist.size() - 1)), false)), 53);

        gui.addItem(itemRmBlacklist, 49);
    }

    private static void addCustomItemsButtons(int i, int pageAmount, final InventoryGUI gui) {
        if (i > 0) gui.addItem(new InventoryItem(previous, e -> PlayerUtils.openGUI(e.getWhoClicked(), customItems.get(Math.min(i - 1, customItems.size() - 1)), false)), 45);
        if (i < pageAmount - 1) gui.addItem(new InventoryItem(next, e -> PlayerUtils.openGUI(e.getWhoClicked(), customItems.get(Math.min(i + 1, customItems.size() - 1)), false)), 53);

        gui.addItem(itemRmCustomItems, 49);
    }
}
