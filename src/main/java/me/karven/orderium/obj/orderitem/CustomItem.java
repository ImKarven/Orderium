package me.karven.orderium.obj.orderitem;

import com.google.common.collect.ImmutableList;
import me.karven.orderium.utils.Log;
import me.karven.orderium.utils.PDCUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CustomItem implements SearchableItem, SerializedItem {
    private final byte[] itemAsBytes;
    private ItemStack item;
    private final List<String> searches;

    public CustomItem(byte @NotNull [] itemAsBytes) {
        this.itemAsBytes = itemAsBytes;
        this.item = ItemStack.deserializeBytes(itemAsBytes);
        this.searches = new ArrayList<>();
    }

    public CustomItem(byte @NotNull [] itemAsBytes, String @NotNull [] searches) {
        this(itemAsBytes);
        this.searches.addAll(Arrays.asList(searches));
    }

    public CustomItem(byte @NotNull [] itemAsBytes, List<@NotNull String> searches) {
        this(itemAsBytes);
        this.searches.addAll(searches);
    }

    @Override
    public void addSearch(@NonNull String key) {
        searches.add(key);
    }

    @Override
    public void addAllSearches(@NotNull Collection<@NotNull String> keys) {
        searches.addAll(keys);
    }

    @Override
    public void removeSearch(@NonNull String key) {
        searches.remove(key);
    }

    @Override
    public void removeAllSearches(@NotNull Collection<@NotNull String> keys) {
        this.searches.removeAll(keys);
    }

    @Override
    public void setSearches(@NonNull List<@NotNull String> searches) {
        this.searches.clear();
        this.searches.addAll(searches);
    }

    @Override
    public @NotNull String getParsedSearches() {
        return String.join(", ", searches);
    }

    @Override
    public @NonNull ImmutableList<@NotNull String> getSearches() {
        return ImmutableList.copyOf(searches);
    }

    @Override
    public @NotNull ItemStack getParsedItemStack() {
        ItemStack item = getItemStack();
        item.editMeta(meta -> {
            PDCUtils.setSearch(meta, String.join(",", searches));
        });
        return item;
    }

    @Override
    public boolean canBeSearched(@NonNull String key) {
        for (String search : searches) {
            if (search.contains(key)) return true;
        }
        return false;
    }

    @Override
    public byte @NotNull [] getItemAsBytes() {
        return itemAsBytes;
    }

    @Override
    public @NonNull ItemStack getItemStack() {
        return item.clone();
    }

    @Override
    public @Nullable OrderItem clone() {
        try {
            return (OrderItem) super.clone();
        } catch (CloneNotSupportedException e) {
            Log.error("clone failed", e);
        }
        return null;
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        this.item = itemStack;
    }
}
