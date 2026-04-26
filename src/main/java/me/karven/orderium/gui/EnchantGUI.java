package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.obj.orderitem.EnchantableItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static me.karven.orderium.load.Orderium.plugin;

/**
 * GUI that lets players select enchantments of their item
 */
public class EnchantGUI {
    // Store all the applicable enchantments and their current levels.
    ConcurrentHashMap<Enchantment, Integer> enchantsWithLevel = new ConcurrentHashMap<>();

    /**
     * Create an EnchantGUI and shows it to the player
     * @param player the player
     * @param item the original item
     * @param action the action to perform after enchantments are applied, will be null if the player exits the GUI
     */
    public EnchantGUI(Player player, EnchantableItem item, Consumer<OrderItem> action) {
        Collection<Enchantment> enchantable = item.getEnchantable();
        if (enchantable.isEmpty()) {
            action.accept(item);
            return;
        }
        MiniMessage mm = plugin.mm;
        ConfigCache cache = plugin.getConfigs();
        int length = enchantable.size();
        // Create the GUI. Use more rows if more than 9 enchantments
        int enchantmentsRows = Math.min(4, length / 9 + 1); // amount of rows for enchantment books
        ChestGui gui = new ChestGui(2 + enchantmentsRows, ComponentHolder.of(mm.deserialize(cache.getEnchantItemTitle())));
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.setOnGlobalDrag(event -> event.setCancelled(true));

        ItemStack enchantedItem = item.getItemStack();

        StaticPane topPane = new StaticPane(9, 1);
        Consumer<InventoryClickEvent> confirmAction = _ -> {
            OrderItem copy = item.clone();
            if (copy != null) copy.setItemStack(enchantedItem);
            action.accept(copy);
        };
        GuiItem displayItem = new GuiItem(item.getItemStack());
        GuiItem confirmItem = ConvertUtils.parseButton(cache.getConfirmEnchantButton(), confirmAction);

        topPane.addItem(displayItem, 0, 0);
        topPane.addItem(confirmItem, cache.getConfirmEnchantButton().getSlot(), 0);

        OutlinePane enchantmentsPane = new OutlinePane(9, enchantmentsRows);
        Component activePrefix = mm.deserialize(cache.getEnchantActivePrefix());
        Component inactivePrefix = mm.deserialize(cache.getEnchantInactivePrefix());
        for (Enchantment enchantment : enchantable) {
            Component enchantmentName = enchantment.description().decoration(TextDecoration.ITALIC, false);
            ItemStack bookItem = ItemStack.of(Material.ENCHANTED_BOOK);
            bookItem.editMeta(meta -> {
               meta.setEnchantmentGlintOverride(true);
               meta.displayName(inactivePrefix.append(enchantmentName));
               meta.lore(cache.getEnchantLore().stream().map(raw -> mm.deserialize(raw).decoration(TextDecoration.ITALIC, false)).toList());
            });
            GuiItem guiItem = new GuiItem(bookItem);

            TriConsumer<Integer, Integer, Integer> changeLevel = (start, end, increment) -> {
                int newLevel = enchantsWithLevel.compute(enchantment, (_, currentLevel) -> {
                    if (currentLevel == null || currentLevel == 0) return start;
                    if (currentLevel.equals(end)) return 0;
                    return currentLevel + increment;
                });
                guiItem.getItem().editMeta(meta -> {
                    switch (newLevel) {
                        case 0 -> meta.displayName(inactivePrefix.append(enchantmentName));
                        case 1 -> meta.displayName(activePrefix.append(enchantmentName));
                        default -> meta.displayName(activePrefix.append(enchantmentName.append(Component.text(" " + newLevel))));
                    }
                });
                if (newLevel == 0) enchantedItem.removeEnchantment(enchantment);
                else if (!conflicts(enchantedItem, enchantment)) enchantedItem.addUnsafeEnchantment(enchantment, newLevel);
                gui.update();
            };

            Consumer<InventoryClickEvent> clickAction = e -> {
                if (conflicts(enchantedItem, enchantment)) return;
                switch (e.getClick()) {
                    case RIGHT -> changeLevel.accept(enchantment.getMaxLevel(), 0, -1); // Decrease level
                    case LEFT -> changeLevel.accept(1, enchantment.getMaxLevel(), 1); // Increase level
                }
            };
            guiItem.setAction(clickAction);
            enchantmentsPane.addItem(guiItem);
        }

        gui.addPane(Slot.fromXY(0, 0), topPane);
        gui.addPane(Slot.fromXY(0, 2), enchantmentsPane);

        PlayerUtils.openGui(player, gui);
    }

    /**
     * Check if an item stack has an enchantment that conflicts with the specified one.
     * @param item the item
     * @param enchantment the specified enchantment
     * @return {@code true} if there are conflict enchantments
     */
    private boolean conflicts(ItemStack item, Enchantment enchantment) {
        for (Enchantment itemEnchantment : item.getEnchantments().keySet()) {
            if (enchantment.equals(itemEnchantment) || enchantment.isCursed()) continue;
            if (enchantment.conflictsWith(itemEnchantment)) return true;
        }
        return false;
    }
}
