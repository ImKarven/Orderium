package me.karven.orderium.utils;

import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static me.karven.orderium.Orderium.plugin;

@SuppressWarnings("UnstableApiUsage")
public class ConvertUtils {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static ItemType getItemType(final @Nullable String identifier) {
        if (identifier == null) return ItemType.STONE;
        final String[] components = identifier.split(":");
        if (components.length != 2) return ItemType.STONE;
        final ItemType itemType = Registry.ITEM.get(new NamespacedKey(components[0], components[1]));
        if (itemType == null) return ItemType.STONE;
        return itemType;
    }

    public static List<Order> convertOrders(ResultSet raw) {
        final List<Order> orders = new ArrayList<>();
        if (raw == null) return orders;
        try (raw) {
            while (raw.next()) {
                orders.add(
                        new Order(raw.getInt(1),
                                new UUID(raw.getLong(2), raw.getLong(3)),
                                ItemStack.deserializeBytes(raw.getBytes(4)),
                                raw.getDouble(5),
                                raw.getInt(6),
                                raw.getInt(7),
                                raw.getInt(8),
                                raw.getLong(9)
                        )
                );
            }
        } catch (SQLException e) {
            Log.error("Failed to fetch order from database", e);
        }

        return orders;
    }

    public static List<BlacklistedItem> convertBlacklistedItems(ResultSet raw) {
        final List<BlacklistedItem> items = new ArrayList<>();
        if (raw == null) return items;
        try (raw) {

            while (raw.next()) {
                items.add(new BlacklistedItem(raw.getBytes(1)));
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch item from database", e);
        }

        return items;
    }

    public static List<CustomItem> convertCustomItems(ResultSet raw) {
        final List<CustomItem> items = new ArrayList<>();
        if (raw == null) return items;
        try (raw) {
            while (raw.next()) {
                final byte[] itemBytes = raw.getBytes(1);
                final String search = raw.getString(2);
                items.add(new CustomItem(itemBytes, search.split(",")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch searchable item from database", e);
        }

        return items;

    }

    public static ItemStack addLore(ItemStack item, List<String> toAdd) {
        item.editMeta(meta -> {
            if (!meta.hasLore()) {
                meta.lore(toAdd.stream().map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false)).toList());
                return;
            }
            final List<Component> lore = meta.lore();
            assert lore != null;
            lore.addAll(toAdd.stream().map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false)).toList());
            meta.lore(lore);
        });
        return item;
    }

    public static int ceil_div(int a, int b) {
        return 1 + ((a - 1) / b);
    }

    private static final HashMap<String, Double> unit = new HashMap<>();

    static {
        unit.put("K", 1000d);
        unit.put("M", 1000000d);
        unit.put("B", 1000000000d);
        unit.put("T", 1000000000000d);
    }

    public static String formatNumber(double a) {
        if (a < 0) return "";
        int cnt = (int) Math.log10(a);
        if (cnt >= 12) return fancy(a / unit.get("T")) + "T";
        if (cnt >= 9) return fancy(a / unit.get("B")) + "B";
        if (cnt >= 6) return fancy(a / unit.get("M")) + "M";
        if (cnt >= 3) return fancy(a / unit.get("K")) + "K";
        return fancy(a);
    }

    private static String fancy(double a) {
        return removeDecimal(round2dp(a));
    }

    private static String removeDecimal(double a) {
        final String res = String.valueOf(a);
        if (a == Math.floor(a)) {
            return res.substring(0, res.length() - 2);
        }
        return res;
    }

    private static double round2dp(double a) {
        return Math.round(a * 100d) / 100d;
    }

    public static double formatNumber(String s) {
        if (s == null || s.isEmpty()) return -1;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            if (s.length() == 1) return -1;
        }
        double num;
        try {
            num = Double.parseDouble(s.substring(0, s.length() - 1));
        } catch (Exception e2) { return -1; }
        final String suffix = s.substring(s.length() - 1).toUpperCase();
        if (!unit.containsKey(suffix)) return -1;
        num *= unit.get(suffix);
        return num;
    }

    public static @NotNull ItemStack deserializeItem(final @NotNull ConfigSection section) {
        final HashMap<String, Object> serialized = new HashMap<>();
        Log.info("Deserializing item. Key-value's list:");
        for (final String key : section.getKeys(false, true)) {
            if (!key.equals("components")) {
                serialized.put(key, section.get(key));
                Log.info(key + ": " + section.get(key));
                continue;
            }

            final HashMap<String, String> componentMap = new HashMap<>();
            final ConfigSection componentSection = section.getConfigSection(key);
            for (final String componentKey : componentSection.getKeys(false, true)) {
                componentMap.put(componentKey, componentSection.getString(componentKey));
            }
            serialized.put(key, componentMap);
        }
        Log.info("");

        return ItemStack.deserialize(serialized);
    }
}
