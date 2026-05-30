package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import me.karven.orderium.utils.Values;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class DialogButtonConfig extends ComponentConfig {
    public String label;
    public String tooltip;
    public int width;

    public DialogButtonConfig(@NotNull String path) {
        super(path);
    }

    public @NotNull ActionButton button(final @NotNull DialogActionCallback clickAction, final @NotNull TagResolver... placeholders) {
        return ActionButton.builder(Values.minimessage.deserialize(label, placeholders))
                .tooltip(Values.minimessage.deserialize(tooltip, placeholders))
                .width(width)
                .action(DialogAction.customClick(clickAction, Values.CLICK_CALLBACK_DEFAULT_OPTIONS))
                .build();
    }

    @Override
    public void reload(@NotNull ConfigFile config) {
        label = config.getString(path + ".label");
        tooltip = config.getString(path + ".tooltip");
        width = config.getInteger(path + ".width");
    }

    @Override
    public void save(@NotNull ConfigFile config) {
        config.set(path + ".label", label);
        config.set(path + ".tooltip", tooltip);
        config.set(path + ".width", width);
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault(path + ".label", label);
        config.addDefault(path + ".tooltip", tooltip);
        config.addDefault(path + ".width", width);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig, @NotNull String path) {
        label = oldConfig.getString(path + "-button");
        tooltip = oldConfig.getString(path + "-tooltip");
        if (path.startsWith("gui.new-order."))
            width = oldConfig.getInteger("gui.new-order.button-width");
        else
            width = 150;
    }
}
