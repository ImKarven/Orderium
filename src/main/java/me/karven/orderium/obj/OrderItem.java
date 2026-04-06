package me.karven.orderium.obj;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.Getter;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderItem {
    private final ItemStack item;
    private final String searchKey;
    private final List<Enchantment> enchantable;
    private static final Registry<Enchantment> enchantRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

    public OrderItem(ItemStack item) {
        this(item, "");
    }

    public OrderItem(ItemStack item, List<Enchantment> enchantable) {
        this(item, "", enchantable);
    }

    public OrderItem(ItemStack item, String searchKey) {
        this.item = item;
        this.searchKey = searchKey;
        this.enchantable = new ArrayList<>();

        for (Enchantment enchantment : enchantRegistry) {
            if (!enchantment.canEnchantItem(item) || enchantment.isCursed()) continue;
            enchantable.add(enchantment);
        }
    }

    public OrderItem(ItemStack item, String searchKey, List<Enchantment> enchantable) {
        this.item = item;
        this.searchKey = searchKey;
        this.enchantable = enchantable;
    }
}
