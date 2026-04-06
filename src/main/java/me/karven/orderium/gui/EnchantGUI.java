package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.karven.orderium.obj.OrderItem;
import me.karven.orderium.utils.Log;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI that lets players select enchantments of their item
 */
public class EnchantGUI {
    // Store all the applicable enchantments and their current levels.
    HashMap<Enchantment, Integer> enchantsWithLevel = new HashMap<>();

    /**
     * Create an EnchantGUI and shows it to the player
     * @param player the player
     * @param item the original item
     * @param action the action to perform after enchantments are applied, will be null if the player exits the GUI
     */
    public EnchantGUI(Player player, OrderItem item, Consumer<ItemStack> action) {
        // TODO: replace texts from config where relevant
        List<Enchantment> enchantable = item.getEnchantable();
        int length = enchantable.size();
        // Create the GUI. Use 4 rows if more than 9 enchantments
        int addition = length > 9 ? 1 : 0;
        ChestGui gui = new ChestGui(3 + addition, "enchant gui");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.setOnGlobalDrag(event -> event.setCancelled(true));

        StaticPane topPane = new StaticPane(9, 1);
        Consumer<InventoryClickEvent> confirmAction = e -> {
            NewOrderDialog.newSession(player, item.getItem());
        };
        GuiItem displayItem = new GuiItem(item.getItem());
        GuiItem confirmItem = new GuiItem(ItemStack.of(Material.GREEN_WOOL), confirmAction);

        topPane.addItem(displayItem, 0, 0);
        topPane.addItem(confirmItem, 8, 0);

        OutlinePane enchantmentsPane = new OutlinePane(9, 1 + addition);

        for (int i = 0; i < length; i++) {
            Enchantment enchantment = enchantable.get(i);
            ItemStack bookItem = ItemStack.of(Material.ENCHANTED_BOOK);
            bookItem.editMeta(meta -> {
               meta.setEnchantmentGlintOverride(true);
               meta.displayName(enchantment.description().color(NamedTextColor.GRAY));
               meta.lore(List.of(
                       Component.text("right click to decrease level"),
                       Component.text("left click to increase level")
               ));
            });
            GuiItem guiItem = new GuiItem(bookItem);

            TriConsumer<Integer, Integer, Integer> changeLevel = (start, end, increment) -> {
                int newLevel = enchantsWithLevel.compute(enchantment, (key, currentLevel) -> {
                    if (currentLevel == null || currentLevel == 0) return start;
                    if (currentLevel.equals(end)) return 0;
                    return currentLevel + increment;
                });
                Log.info("" + newLevel);
                guiItem.getItem().editMeta(meta -> {
                    switch (newLevel) {
                        case 0 -> meta.displayName(enchantment.description().color(NamedTextColor.GRAY));
                        case 1 -> meta.displayName(enchantment.description().color(NamedTextColor.LIGHT_PURPLE));
                        default -> meta.displayName(enchantment.description().append(Component.text(" " + newLevel)).color(NamedTextColor.LIGHT_PURPLE));
                    }
                });
                ItemStack displayItemStack = displayItem.getItem();
                if (newLevel == 0) displayItemStack.removeEnchantment(enchantment);
                else if (!conflicts(displayItemStack, enchantment)) displayItemStack.addUnsafeEnchantment(enchantment, newLevel);
                gui.update();
            };

            Consumer<InventoryClickEvent> clickAction = e -> {
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
