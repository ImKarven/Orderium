package me.karven.orderium.utils;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import me.karven.orderium.obj.SlotInfo;
import me.karven.orderium.obj.SortTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ConvertUtils {
    private static DBManager dbManager;
    private static Orderium plugin;
    private static MiniMessage mm;
    private static ConfigManager cache;

    public static void init(Orderium pl) {
        plugin = pl;
        dbManager = pl.getDbManager();
        mm = pl.mm;
        cache = pl.getConfigs();
    }


    public static List<Order> convertOrders(ResultSet raw) {
        final List<Order> orders = new ArrayList<>();
        if (raw == null) return orders;
        try {
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
                        ));
            }
            raw.close();
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        return orders;
    }

    public static GuiItem parseOrder(Order order, List<String> rawLore, Consumer<InventoryClickEvent> action) {
        final ItemStack item = order.item();
        final String playerName = Bukkit.getOfflinePlayer(order.owner()).getName();
        final String pName = playerName == null ? String.valueOf(order.owner()) : playerName;
        final List<Component> lore = rawLore.stream().map(str -> mm.deserialize(str,
                Placeholder.unparsed("money-per", formatNumber(order.moneyPer())),
                Placeholder.unparsed("paid", formatNumber(order.moneyPer() * order.delivered())),
                Placeholder.unparsed("total", formatNumber(order.moneyPer() * order.amount())),
                Placeholder.unparsed("delivered", formatNumber(order.delivered())),
                Placeholder.unparsed("amount", formatNumber(order.amount())),
                Placeholder.unparsed("player", pName)
        ).decoration(TextDecoration.ITALIC, false)).toList();
        item.lore(lore);
        return new GuiItem(item, action);
    }

    public static GuiItem parseButton(SlotInfo info, Consumer<InventoryClickEvent> action, TagResolver... placeholders) {
        final ItemStack item = ItemStack.of(info.getType());
        item.editMeta(meta -> {
            meta.displayName(mm.deserialize(info.getDisplayName(), placeholders).decoration(TextDecoration.ITALIC, false));
            final List<Component> lore = info.getLore().stream().map(str -> mm.deserialize(str, placeholders).decoration(TextDecoration.ITALIC, false)).toList();
            meta.lore(lore);
        });
        return new GuiItem(item, action);
    }

    public static GuiItem parseSortButton(SlotInfo info, SortTypes type, Consumer<InventoryClickEvent> action) {
        final ItemStack item = ItemStack.of(info.getType());
        List<TagResolver> placeholders = new ArrayList<>(List.of(cache.getSortPlaceholders()));
        @Subst("ignored")
        final String identifier = type.getIdentifier();
        placeholders.add(Placeholder.parsed(identifier, cache.getSortPrefix() + type.getDisplay()));
        item.editMeta(meta -> {
            meta.displayName(mm.deserialize(info.getDisplayName(), TagResolver.resolver(placeholders)).decoration(TextDecoration.ITALIC, false));
            final List<Component> lore = info.getLore().stream().map(str -> mm.deserialize(str, TagResolver.resolver(placeholders)).decoration(TextDecoration.ITALIC, false)).toList();
            meta.lore(lore);
        });
        return new GuiItem(item, action);
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
        if (cnt >= 12) {
            return fancy(a / unit.get("T")) + "T";
        }
        if (cnt >= 9) {
            return fancy(a / unit.get("B")) + "B";
        }
        if (cnt >= 6) {
            return fancy(a / unit.get("M")) + "M";
        }
        if (cnt >= 3) {
            return fancy(a / unit.get("K")) + "K";
        }
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
        if (s.isEmpty()) return -1;
        try {
            final double num = Double.parseDouble(s);
            if (num < 1) return -1;
            return num;
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
}
