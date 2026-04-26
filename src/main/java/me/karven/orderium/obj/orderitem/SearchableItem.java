package me.karven.orderium.obj.orderitem;

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/// This interface is for order items that can be searched with custom queries instead of relying on the item type key
public interface SearchableItem extends OrderItem {
    /**
     * Add a string for searching this item
     * @param key the string
     */
    void addSearch(@NotNull String key);

    /**
     * Add all a list to the list of search strings
     * @param keys the list to add all
     */
    void addAllSearches(@NotNull Collection<@NotNull String> keys);

    /**
     * Remove a search string from this item
     * @param key the string
     */
    void removeSearch(@NotNull String key);

    /**
     * Remove all a list to the list of search strings
     * @param keys the list to remove all
     */
    void removeAllSearches(@NotNull Collection<@NotNull String> keys);

    /**
     * Clear the current and add all a list of search strings
     * @param searches the new list of search strings
     */
    void setSearches(@NotNull List<@NotNull String> searches);

    /**
     * Get a string containing all the search strings separated by commas
     * @return the parsed string
     */
    @NotNull String getParsedSearches();

    /**
     * Get an immutable list of all the search strings
     * @return the list
     */
    @NotNull ImmutableList<@NotNull String> getSearches();

    /**
     * Copy the original item stack and add the search strings as persistent data to it, essentially representing the searchable item as {@code ItemStack}
     * @return the item stack
     */
    @NotNull ItemStack getParsedItemStack();

    /**
     * Check if this item can be searched with a specified string
     * @param key the specified string
     * @return {@code true} if it can be searched
     */
    boolean canBeSearched(@NotNull String key);
}
