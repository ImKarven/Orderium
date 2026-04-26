package me.karven.orderium.obj.orderitem;

import com.google.common.collect.ImmutableList;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EnchantableItem extends OrderItem {
    /**
     * Make this item enchantable with an enchantment
     * @param enchantment the enchantment
     */
    void addEnchantable(@NotNull Enchantment enchantment);

    /**
     * Make this item no longer enchantable with an enchantment
     * @param enchantment the enchantment
     */
    void removeEnchantable(@NotNull Enchantment enchantment);

    /**
     * Clear the current and add all a list of enchantable enchantments
     * @param enchantments the new list of enchantable enchantments
     */
    void setEnchantable(@NotNull List<@NotNull Enchantment> enchantments);

    /**
     * Get an immutable list of all the enchantments that can be applied to this item
     * Note that some enchantments are incompatible with each other
     * @return the enchantments
     */
    @NotNull ImmutableList<@NotNull Enchantment> getEnchantable();
}
