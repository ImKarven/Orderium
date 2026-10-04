package me.karven.orderium.utils;

import com.google.common.base.Preconditions;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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

    public static int calculatePageAmount(final int slotCount, final int amountPerPage) {
        Preconditions.checkArgument(slotCount >= 0, "Slot count must be non-negative");
        Preconditions.checkArgument(amountPerPage > 0, "Amount per page must be positive");
        if (slotCount == 0) return 1;
        return 1 + ((slotCount - 1) / amountPerPage);
    }

    public static String replaceText(final String text, final String... replacements) {
        String result = text;
        for (int i = 0; i < replacements.length; i += 2) {
            result = result.replaceAll(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    private static final Map<String, Double> unit = Map.of(
            "K", 1000d,
            "M", 1000000d,
            "B", 1000000000d,
            "T", 1000000000000d
    );

    public static String formatNumber(double a) {
        if (a < 0) return "-" + formatNumber(-a);
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
        return NBTSerializer.deserializeItemStack(section);
    }

    public static @NotNull Object serializeItem(final @NotNull ItemStack item) {
        return NBTSerializer.serializeItemStack(item);
    }
}
