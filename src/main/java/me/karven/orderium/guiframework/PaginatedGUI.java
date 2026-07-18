package me.karven.orderium.guiframework;

import me.karven.orderium.obj.ItemClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class PaginatedGUI<T> {
    protected final int rows;
    protected final @NotNull Component title;
    protected final @NotNull Player player;
    protected final @NotNull List<InventoryGUI> builtGUIs = new ArrayList<>();
    protected final @NotNull Iterator<T> itemIterator;
    protected final @NotNull Function<T, ItemStack> convertFunction;
    protected final @NotNull Consumer<ItemClickContext<T>> clickAction;
    protected final @NotNull List<Integer> slots;
    public PaginatedGUI(
            final int rows,
            final @NotNull Component title,
            final @NotNull Iterable<T> items,
            final @NotNull Function<T, ItemStack> convertFunction,
            final @NotNull Consumer<ItemClickContext<T>> clickAction,
            final @NotNull Player player,
            final @NotNull List<Integer> slots
            ) {
        this.rows = rows;
        this.title = title;
        this.itemIterator = items.iterator();
        this.convertFunction = convertFunction;
        this.clickAction = clickAction;
        this.player = player;
        this.slots = slots;
    }


    public void open() {
        // show the first page to the player
        if (!builtGUIs.isEmpty()) {
            builtGUIs.getFirst().open(player);
            return;
        }
        openNextPage();
    }

    public @NotNull InventoryGUI getNextPage() {
        final InventoryGUI nextPage = new InventoryGUI(this.rows, this.title);
        nextPage.setOnClick(event -> event.setCancelled(true), InteractLocation.GLOBAL);
        nextPage.setOnDrag(event -> event.setCancelled(true), InteractLocation.GLOBAL);
        int index = 0;
        while (itemIterator.hasNext() && index < slots.size()) {
            final T object = itemIterator.next();
            final ItemStack item = convertFunction.apply(object);
            final InventoryItem inventoryItem = new InventoryItem(
                    item,
                    inventoryClickEvent -> clickAction.accept(new ItemClickContext<>(object, inventoryClickEvent))
            );
            nextPage.addItem(inventoryItem, slots.get(index++));
        }

        populateButtons(nextPage);
        builtGUIs.add(nextPage);
        return nextPage;
    }

    public boolean hasNextPage() {
        return itemIterator.hasNext();
    }

    public void openNextPage() {
        getNextPage().open(player);
    }

    protected abstract void populateButtons(final @NotNull InventoryGUI gui);

    /**
     * Skip to a specific page index
     * Used by refresh and sort buttons to preserve page after refreshing
     * @param skipTo the index of page to skip to
     * @return the page at that index
     */
    protected InventoryGUI skipPages(final int skipTo) {
        int index = 0;
        InventoryGUI currentPage = getNextPage();
        while (hasNextPage() && index < skipTo) {
            currentPage = getNextPage();
            index++;
        }
        return currentPage;
    }
}
