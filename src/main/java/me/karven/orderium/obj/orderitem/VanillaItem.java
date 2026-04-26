package me.karven.orderium.obj.orderitem;

import com.google.common.collect.ImmutableList;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.karven.orderium.utils.Log;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class VanillaItem implements EnchantableItem {
    private ItemStack item;
    private final List<Enchantment> enchantable = new ArrayList<>();

    /**
     * Create a vanilla item
     * @param item the bukkit item stack
     * @param autoGenerateEnchantable if it should check all enchantments and see which ones are enchantable
     */
    public VanillaItem(@NotNull ItemStack item, boolean autoGenerateEnchantable) {
        this.item = item;

        if (!autoGenerateEnchantable) return;
        Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        for (Enchantment enchantment : enchantmentRegistry) {
            if (!enchantment.canEnchantItem(item)) continue;
            enchantable.add(enchantment);
        }
    }

    @Override
    public void addEnchantable(@NonNull Enchantment enchantment) {
        enchantable.add(enchantment);
    }

    @Override
    public void removeEnchantable(@NonNull Enchantment enchantment) {
        enchantable.remove(enchantment);
    }

    @Override
    public void setEnchantable(@NonNull List<@NotNull Enchantment> enchantments) {
        enchantable.clear();
        enchantable.addAll(enchantments);
    }

    @Override
    public @NonNull ImmutableList<@NotNull Enchantment> getEnchantable() {
        return ImmutableList.copyOf(enchantable);
    }

    @Override
    public @NonNull ItemStack getItemStack() {
        return item;
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
