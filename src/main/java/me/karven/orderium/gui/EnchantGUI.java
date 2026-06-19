package me.karven.orderium.gui;

import me.karven.orderium.config.Config;
import me.karven.orderium.guiframework.InteractLocation;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.obj.orderitem.EnchantableItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.VanillaItem;
import me.karven.orderium.utils.Log;
import me.karven.orderium.utils.PDCUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static me.karven.orderium.Orderium.plugin;

/**
 * GUI that lets players select enchantments of their item
 */
public class EnchantGUI {
    // Store all the applicable enchantments and their current levels.
    private final ConcurrentHashMap<Enchantment, Integer> enchantsWithLevel = new ConcurrentHashMap<>();
    private InventoryGUI gui;

    /**
     * Create an EnchantGUI and shows it to the player
     * @param item the original item
     * @param action the action to perform after enchantments are applied, will be null if the player exits the GUI
     */
    public EnchantGUI(EnchantableItem item, Consumer<OrderItem> action) {
        if (!(item instanceof VanillaItem vanillaItem)) {
            Log.error("Failed to process enchant GUI", new IllegalArgumentException("Unsupported item"));
            return;
        }
        Collection<Enchantment> enchantable = item.getEnchantable();
        if (enchantable.isEmpty()) {
            action.accept(item);
            return;
        }
        MiniMessage mm = plugin.mm;
        final Config config = Config.config;
        this.gui = new InventoryGUI(config.enchantGUIConfig.rows, mm.deserialize(config.enchantGUIConfig.title));
        gui.setOnClick(event -> event.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(event -> event.setCancelled(true), InteractLocation.GLOBAL);

        ItemStack enchantedItem = item.getItemStack();

        Consumer<InventoryClickEvent> confirmAction = event -> {
            OrderItem copy = vanillaItem.copy();
            enchantedItem.editMeta(PDCUtils::clear);
            copy.setItemStack(enchantedItem);
            action.accept(copy);
        };
        InventoryItem displayItem = new InventoryItem(enchantedItem);
        InventoryItem confirmItem = config.enchantGUIConfig.confirmButton.item(confirmAction);

        gui.addItem(displayItem, 0);
        gui.addItem(confirmItem, config.enchantGUIConfig.confirmButton.slot);

        int currentSlotIndex = 0;
        for (Enchantment enchantment : enchantable) {
            Component enchantmentName = enchantment.description();
            final TagResolver enchantmentPlaceholder = Placeholder.component("enchantment", enchantmentName);
            ItemStack bookItem = config.enchantGUIConfig.enchantmentConfig.itemRepresentation.clone();
            bookItem.editMeta(meta ->
                    meta.displayName(
                            mm.deserialize(
                                    config.enchantGUIConfig.enchantmentConfig.inactiveName,
                                    enchantmentPlaceholder
                            )
                    )
            );
            InventoryItem guiItem = new InventoryItem(bookItem);

            TriConsumer<Integer, Integer, Integer> changeLevel = (start, end, increment) -> {
                int newLevel = enchantsWithLevel.compute(enchantment, (enchant, currentLevel) -> {
                    if (currentLevel == null || currentLevel == 0) return start;
                    if (currentLevel.equals(end)) return 0;
                    return currentLevel + increment;
                });
                guiItem.getItem().editMeta(meta -> {
                    if (newLevel == 0) {
                        meta.displayName(mm.deserialize(config.enchantGUIConfig.enchantmentConfig.inactiveName, enchantmentPlaceholder));
                    } else {
                        meta.displayName(mm.deserialize(
                                config.enchantGUIConfig.enchantmentConfig.activeName, enchantmentPlaceholder,
                                Placeholder.unparsed("level", String.valueOf(newLevel)),
                                Placeholder.component("roman-numeral-level", Component.translatable("enchantment.level." + newLevel))
                        ));
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
            guiItem.setOnClick(clickAction);
            gui.addItem(guiItem, config.enchantGUIConfig.enchantmentConfig.slots.get(currentSlotIndex++));
        }
    }

    public @Nullable InventoryGUI getGUI() {
        return gui;
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
