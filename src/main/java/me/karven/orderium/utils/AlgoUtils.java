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
        final String q = fixQuery(query);
        final List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : items) {
            if (search(item, q)) result.add(item);
        }
        return result;
    }

    public static List<Order> searchOrder(String query, Collection<Order> orders) {
        final String q = fixQuery(query);
        final List<Order> result = new ArrayList<>();
        for (Order order : orders) {
            if (search(order.item(), q)) result.add(order);
        }
        return result;
    }

    private static String fixQuery(String query) {
        return query.toLowerCase().trim().replaceAll(" ", "_");
    }

    private static boolean search(final ItemStack item, String q) {
        if (PDCUtils.hasCustomSearch(item.getItemMeta())) {
            final String customSearch = PDCUtils.getSearch(item.getItemMeta());
            final int len = customSearch.length();
            StringBuilder curr = new StringBuilder();
            for (int i = 0; i < len; i++) {
                final char c = customSearch.charAt(i);
                if (c == ',') {
                    if (curr.toString().contains(q)) return true;
                    curr = new StringBuilder();
                    continue;
                }
                if (c == ' ') curr.append('_');
                else curr.append(c);
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
                    final int compared = compareEnchantments(enchantmentsA, enchantmentsB);
                    if (compared != 0) return compared;
                }

                case GOAT_HORN -> {
                    final MusicInstrument instrumentA = a.getData(DataComponentTypes.INSTRUMENT);
                    final MusicInstrument instrumentB = b.getData(DataComponentTypes.INSTRUMENT);
                    final int compared = compareInstruments(instrumentA, instrumentB);
                    if (compared != 0) return compared;
                }

                case POTION, LINGERING_POTION, SPLASH_POTION -> {
                    final PotionContents potionsA = a.getData(DataComponentTypes.POTION_CONTENTS);
                    final PotionContents potionsB = b.getData(DataComponentTypes.POTION_CONTENTS);
                    final int compared = comparePotionEffects(potionsA, potionsB);
                    if (compared != 0) return compared;
                }
            }

            final byte[] b1 = a.serializeAsBytes();
            final byte[] b2 = b.serializeAsBytes();
            return compareBytes(b1, b2);
        };
    }

    private static int compareEnchantments(ItemEnchantments a, ItemEnchantments b) {
        if (a == null || b == null) return 0;
        if (a.enchantments().isEmpty() || b.enchantments().isEmpty()) return 0;
        final Enchantment enchantmentA = a.enchantments().keySet().iterator().next();
        final Enchantment enchantmentB = b.enchantments().keySet().iterator().next();
        final String nameA = enchantmentA.getKey().toString();
        final String nameB = enchantmentB.getKey().toString();
        final int compared = nameA.compareTo(nameB);
        if (compared != 0) return compared;
        final int levelA = a.enchantments().values().iterator().next();
        final int levelB = b.enchantments().values().iterator().next();
        return levelA - levelB;
    }

    private static int compareInstruments(MusicInstrument a, MusicInstrument b) {
        if (a == null || b == null) return 0;
        final NamespacedKey keyA = instrumentRegistry.getKey(a);
        final NamespacedKey keyB = instrumentRegistry.getKey(b);
        if (keyA == null || keyB == null) return 0;
        final String nameA = keyA.toString();
        final String nameB = keyB.toString();
        return nameA.compareTo(nameB);
    }

    private static int comparePotionEffects(PotionContents a, PotionContents b) {
        if (a == null || b == null) return 0;
        if (a.allEffects().isEmpty() || b.allEffects().isEmpty()) return 0;
        final String nameA = a.allEffects().getFirst().getType().toString();
        final String nameB = b.allEffects().getFirst().getType().toString();
        return nameA.compareTo(nameB);
    }

    private static int compareBytes(byte[] a, byte[] b) {
        final int l1 = a.length;
        final int l2 = b.length;
        if (l1 != l2) return l1 - l2;
        for (int i = 0; i < l1; i++) {
            if (a[i] != b[i]) return a[i] - b[i];
        }
        return 0;
    }
}
