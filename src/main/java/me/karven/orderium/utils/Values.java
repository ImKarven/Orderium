package me.karven.orderium.utils;

import dev.faststats.ErrorTracker;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Values {
    public static final ClickCallback.Options CLICK_CALLBACK_DEFAULT_OPTIONS = ClickCallback.Options.builder().build();
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    public static final MiniMessage minimessage = MiniMessage.miniMessage();
}
