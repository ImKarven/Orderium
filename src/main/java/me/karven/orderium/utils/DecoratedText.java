package me.karven.orderium.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

public class DecoratedText {
    public static Component buttonName(final @NotNull String name) {
        return Component.text(name).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
    }

    public static Component buttonLore(final @NotNull String loreLine) {
        return Component.text(loreLine).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
    }
}
