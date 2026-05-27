package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class MessageDialogBodyConfig extends ComponentConfig {
    public PlainMessageDialogBody body;

    public MessageDialogBodyConfig(final @NotNull String path) {
        super(path);
    }

    @Override
    public void reload(@NotNull ConfigFile config) {
        final String contents = config.getString(path + ".contents");
        final int width = config.getInteger(path + ".width");
        body = DialogBody.plainMessage(
                contents == null ? Component.empty() : MiniMessage.miniMessage().deserialize(contents),
                width
        );
    }

    @Override
    public void save(@NotNull ConfigFile config) {
        config.set(path + ".contents", MiniMessage.miniMessage().serialize(body.contents()));
        config.set(path + ".width", body.width());
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault(path + ".contents", MiniMessage.miniMessage().serialize(body.contents()));
        config.addDefault(path + ".width", body.width());
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig, @NotNull String path) {
        final String contents = oldConfig.getString(path);
        body = DialogBody.plainMessage(contents == null ? Component.empty() : MiniMessage.miniMessage().deserialize(contents));
    }
}
