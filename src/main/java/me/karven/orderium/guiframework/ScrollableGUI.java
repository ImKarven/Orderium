package me.karven.orderium.guiframework;

import io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent;
import me.karven.orderium.config.Config;
import me.karven.orderium.obj.ItemClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ScrollableGUI<T> {
    protected int currentIndex = 0;
    protected final int rows;
    protected final int moveAmountPerScroll;
    protected final @NotNull Component title;
    protected final @NotNull Player player;
    protected final @NotNull InventoryGUI currentGUI;
    protected final @NotNull InventoryItem[] itemsArray;
    protected final @NotNull Function<T, ItemStack> convertFunction;
    protected final @NotNull Consumer<ItemClickContext<T>> clickAction;
    protected final @NotNull List<Integer> slots;

    public ScrollableGUI(
            final int rows,
            final int moveAmountPerScroll,
            final @NotNull Component title,
            final @NotNull List<T> items,
            final @NotNull Function<T, ItemStack> convertFunction,
            final @NotNull Consumer<ItemClickContext<T>> clickAction,
            final @NotNull Player player,
            final @NotNull List<Integer> slots,
            final @NotNull Config config
            ) {
        this.rows = rows;
        this.moveAmountPerScroll = moveAmountPerScroll;
        this.title = title;
        this.itemsArray = new InventoryItem[items.size()];
        this.convertFunction = convertFunction;
        this.clickAction = clickAction;
        this.player = player;
        this.slots = slots;

        this.currentGUI = new InventoryGUI(rows, title);

        final Consumer<PlayerBundleItemSelectEvent> scrollAction = event -> {
            final PlayerBundleItemSelectEvent.Direction direction = event.getDirection();
            if (direction == PlayerBundleItemSelectEvent.Direction.UNKNOWN) return;
            if (itemsArray.length <= slots.size()) return; // Too little items to scroll
            int moveAmount = direction.getDelta() * moveAmountPerScroll;
            int newIndex = validateIndex(currentIndex + moveAmount);
            if (newIndex == currentIndex) return;
            currentIndex = newIndex;
            update();
            open();
        };

        int i = 0;
        for (final T object : items) {
            itemsArray[i++] = new InventoryItem(
                    convertFunction.apply(object),
                    event -> clickAction.accept(new ItemClickContext<>(object, event)),
                    scrollAction
            );
        }
        update(config);
    }

    /**
     * Check if index is not overflown, return the index, clamped to the limit if overflown
     * @param index the index to validate
     * @return the validated index
     */
    private int validateIndex(final int index) {
        return Math.clamp(
                index,
                0,
                itemsArray.length - slots.size()
        );
    }

    public void update() {
        for (int i = 0; i < slots.size() && i + currentIndex < itemsArray.length; i++) {
            currentGUI.addItem(itemsArray[i + currentIndex], slots.get(i));
        }
        populateButtons(currentGUI);
    }

    public void update(final @NotNull Config config) {
        for (int i = 0; i < slots.size() && i + currentIndex < itemsArray.length; i++) {
            currentGUI.addItem(itemsArray[i + currentIndex], slots.get(i));
        }
        populateButtons(currentGUI, config);
    }

    public void open() {
        currentGUI.open(player);
    }
    protected abstract void populateButtons(final @NotNull InventoryGUI gui);
    protected abstract void populateButtons(final @NotNull InventoryGUI gui, final @NotNull Config config);

    /**
     * Skip to a specific index
     * Used by refresh and sort buttons to preserve page after refreshing
     * @param skipTo the index in the stream to skip to
     */
    protected void skip(final int skipTo) {
        currentIndex = validateIndex(skipTo);
        update();
    }
}
