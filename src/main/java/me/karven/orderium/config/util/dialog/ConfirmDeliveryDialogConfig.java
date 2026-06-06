package me.karven.orderium.config.util.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.component.dialog.DialogButtonConfig;
import me.karven.orderium.config.util.component.dialog.ItemlessItemDialogBodyConfig;
import me.karven.orderium.config.util.component.dialog.MessageDialogBodyConfig;
import me.karven.orderium.config.util.dialog.dialogtype.ConfirmationDialogConfig;
import me.karven.orderium.utils.Values;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ConfirmDeliveryDialogConfig extends ConfirmationDialogConfig {
    public final @NotNull MessageDialogBodyConfig textBody = new MessageDialogBodyConfig("text-body");
    public final @NotNull ItemlessItemDialogBodyConfig itemBody = new ItemlessItemDialogBodyConfig("item-body");

    public ConfirmDeliveryDialogConfig() {
        super("confirm-delivery");

        yesButton = new DialogButtonConfig("buttons.confirm");
        noButton = new DialogButtonConfig("buttons.cancel");
    }

    public @NotNull Dialog dialog(final @NotNull ItemStack item, final @NotNull String formattedAmount,
                                  final @NotNull DialogActionCallback yesAction, final @NotNull DialogActionCallback noAction) {
        final TagResolver placeholder = Placeholder.unparsed("amount", formattedAmount);
        return Dialog.create(builder -> builder.empty()
                .type(DialogType.confirmation(
                        yesButton.button(yesAction, placeholder),
                        noButton.button(noAction, placeholder)
                ))
                .base(DialogBase.builder(Values.minimessage.deserialize(title, placeholder))
                        .canCloseWithEscape(canCloseWithEsc)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(textBody.body(placeholder), itemBody.body(item, placeholder)))
                        .build()
                )
        );
    }

    @Override
    public void reload() throws IOException {
        super.reload();
        textBody.reload(config);
        itemBody.reload(config);
    }

    @Override
    public void save() {
        super.save();
        textBody.save(config);
        itemBody.save(config);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        textBody.setDefault(config);
        itemBody.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.confirm-delivery.title");
        canCloseWithEsc = true;
        yesButton.migrateV5(oldConfig, "gui.confirm-delivery.confirm");
        noButton.migrateV5(oldConfig, "gui.confirm-delivery.cancel");
        textBody.migrateV5(oldConfig, "gui.confirm-delivery.body");
        itemBody.migrateV5(oldConfig, "gui.confirm-delivery.transaction-message");

        saveToFile();
    }
}
