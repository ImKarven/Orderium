package me.karven.orderium.config.util.component.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import me.karven.orderium.config.util.component.ComponentConfig;
import me.karven.orderium.utils.Values;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class MessageDialogBodyConfig extends ComponentConfig {
    public String contents;
    public int width;

    public MessageDialogBodyConfig(final @NotNull String path) {
        super(path);
    }

    public @NotNull PlainMessageDialogBody body(final @NotNull TagResolver... tagResolvers) {
        return DialogBody.plainMessage(Values.minimessage.deserialize(contents, tagResolvers), width);
    }

    @Override
    public void reload(@NotNull ConfigFile config) {
        contents = config.getString(path + ".contents");
        width = config.getInteger(path + ".width");
    }

    @Override
    public void save(@NotNull ConfigFile config) {
        config.set(path + ".contents", contents);
        config.set(path + ".width", width);
    }

    @Override
    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault(path + ".contents", contents);
        config.addDefault(path + ".width", width);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig, @NotNull String path) {
        contents = oldConfig.getString(path);
        width = 200;
    }
}
