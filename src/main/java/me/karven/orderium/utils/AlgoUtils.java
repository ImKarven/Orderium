package me.karven.orderium.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SortTypes;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class AlgoUtils {

    private static final Registry<MusicInstrument> instrumentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.INSTRUMENT);
    private static final Registry<PotionEffectType> potionEffectRegistry = Registry.MOB_EFFECT;

    public static List<ItemStack> searchItem(String query, Collection<ItemStack> items) {
        final String q = query.toLowerCase().trim().replaceAll(" ", "_");
        final List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : items) {
            if (search(item, q)) result.add(item);
        }
        return result;
    }

    public static List<Order> searchOrder(String query, Collection<Order> orders) {
        final String q = query.toLowerCase().trim().replaceAll(" ", "_");
        final List<Order> result = new ArrayList<>();
        for (Order order : orders) {
            if (search(order.item(), q)) result.add(order);
        }
        return result;
    }

    private static boolean search(final ItemStack item, String q) {

        if (PDCUtils.hasCustomSearch(item.getItemMeta())) {
            final String customSearch = PDCUtils.getSearch(item.getItemMeta());
            final int len = customSearch.length();
            StringBuilder curr = new StringBuilder();
            for (int i = 0; i < len; i++) {
                final char c =  customSearch.charAt(i);
                if (c == ',') {
                    if (curr.toString().contains(q)) return true;
                    curr = new StringBuilder();
                }
                curr.append(c);
            }
            return curr.toString().contains(q);
        }

        final Material type = item.getType();
        final String name = item.getType().getKey().value().toLowerCase();
        if (name.contains(q)) return true;

        switch (type) {
            case ENCHANTED_BOOK -> {
                final ItemEnchantments enchantments = item.getData(DataComponentTypes.STORED_ENCHANTMENTS);
                if (enchantments == null) return false;
                for (final Map.Entry<Enchantment, Integer> entry : enchantments.enchantments().entrySet()) {
                    final String enchant = entry.getKey().getKey().toString();
                    if (enchant.contains(q)) return true;
                }
                return false;
            }

            case GOAT_HORN -> {
                final MusicInstrument instrument = item.getData(DataComponentTypes.INSTRUMENT);
                if (instrument == null) return false;
                final NamespacedKey key = instrumentRegistry.getKey(instrument);
                if (key == null) return false;
                final String instrumentName = key.value();
                return (instrumentName.contains(q));
            }

            case POTION, LINGERING_POTION, SPLASH_POTION -> {
                final PotionContents potions = item.getData(DataComponentTypes.POTION_CONTENTS);
                if (potions == null) return false;
                for (final PotionEffect effect : potions.allEffects()) {
                    final NamespacedKey key = potionEffectRegistry.getKey(effect.getType());
                    if (key == null) return false;
                    final String effectName = key.toString();
                    if (effectName.contains(q)) return true;
                }
                return false;
            }
        }
        return false;
    }

    public static Comparator<ItemStack> getComparator(SortTypes sortType) {
        switch (sortType) {
            case A_Z -> { return getComparator(false); }
            case Z_A -> { return getComparator(true); }
        }
        return null;
    }

    public static Comparator<ItemStack> getComparator(boolean reverse) {
        return (a, b) -> {
            if (reverse) {
                final ItemStack tmp = a;
                a = b;
                b = tmp;
            }
            final Material typeA = a.getType();
            final Material typeB = b.getType();
            final int s = typeA.toString().compareTo(typeB.toString());
            if (s != 0)  return s;
            switch (typeA) {
                case ENCHANTED_BOOK -> {
                    final ItemEnchantments enchantmentsA = a.getData(DataComponentTypes.STORED_ENCHANTMENTS);
                    final ItemEnchantments enchantmentsB = b.getData(DataComponentTypes.STORED_ENCHANTMENTS);
                    if (enchantmentsA == null || enchantmentsB == null) break;
                    if (enchantmentsA.enchantments().isEmpty() || enchantmentsB.enchantments().isEmpty()) break;
                    final Enchantment enchantmentA = enchantmentsA.enchantments().keySet().iterator().next();
                    final Enchantment enchantmentB = enchantmentsB.enchantments().keySet().iterator().next();
                    final String nameA = enchantmentA.getKey().toString();
                    final String nameB = enchantmentB.getKey().toString();
                    final int compared = nameA.compareTo(nameB);
                    if (compared != 0) return compared;
                    final int levelA = enchantmentsA.enchantments().values().iterator().next();
                    final int levelB = enchantmentsB.enchantments().values().iterator().next();
                    if (levelA != levelB) return levelA - levelB;
                }

                case GOAT_HORN -> {
                    final MusicInstrument instrumentA = a.getData(DataComponentTypes.INSTRUMENT);
                    final MusicInstrument instrumentB = b.getData(DataComponentTypes.INSTRUMENT);
                    if (instrumentA == null || instrumentB == null) break;
                    final NamespacedKey keyA = instrumentRegistry.getKey(instrumentA);
                    final NamespacedKey keyB = instrumentRegistry.getKey(instrumentB);
                    if (keyA == null || keyB == null) break;
                    final String nameA = keyA.toString();
                    final String nameB = keyB.toString();
                    final int compared = nameA.compareTo(nameB);
                    if (compared != 0) return compared;
                }

                case POTION, LINGERING_POTION, SPLASH_POTION -> {
                    final PotionContents potionsA = a.getData(DataComponentTypes.POTION_CONTENTS);
                    final PotionContents potionsB = b.getData(DataComponentTypes.POTION_CONTENTS);
                    if (potionsA == null || potionsB == null) break;
                    if (potionsA.allEffects().isEmpty() || potionsB.allEffects().isEmpty()) break;
                    final String nameA = potionsA.allEffects().getFirst().getType().toString();
                    final String nameB = potionsB.allEffects().getFirst().getType().toString();
                    final int compared = nameA.compareTo(nameB);
                    if (compared != 0) return compared;
                }
            }

            final byte[] b1 = a.serializeAsBytes();
            final byte[] b2 = b.serializeAsBytes();
            final int l1 = b1.length;
            final int l2 = b2.length;
            if (l1 != l2) return l1 - l2;
            for (int i = 0; i < l1; i++) {
                if (b1[i] != b2[i]) return b1[i] - b2[i];
            }
            return 0;
        };
    }
}
