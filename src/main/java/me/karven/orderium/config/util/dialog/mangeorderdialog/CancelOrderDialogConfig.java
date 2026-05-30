package me.karven.orderium.config.util.dialog.mangeorderdialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.DialogButtonConfig;
import me.karven.orderium.config.util.MessageDialogBodyConfig;
import me.karven.orderium.config.util.dialog.dialogtype.ConfirmationDialogConfig;
import me.karven.orderium.utils.Values;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CancelOrderDialogConfig extends ConfirmationDialogConfig {
    public final @NotNull MessageDialogBodyConfig body = new MessageDialogBodyConfig("cancel-order.body");

    public CancelOrderDialogConfig() {
        super("manage-order");
        yesButton = new DialogButtonConfig("cancel-order.buttons.confirm");
        noButton = new DialogButtonConfig("cancel-order.buttons.cancel");
    }

    public @NotNull Dialog dialog(final @NotNull DialogActionCallback yesAction, final @NotNull DialogActionCallback noAction) {
        return Dialog.create(builder -> builder.empty()
                .type(DialogType.confirmation(
                        yesButton.button(yesAction),
                        noButton.button(noAction)
                ))
                .base(DialogBase.builder(Values.minimessage.deserialize(title))
                        .canCloseWithEscape(canCloseWithEsc)
                        .body(List.of(body.body()))
                        .build()
                )
        );
    }

    @Override
    public void reload() {
        title = config.getString("cancel-order.title");
        canCloseWithEsc = config.getBoolean("cancel-order.can-close-with-escape");
        body.reload(config);
        yesButton.reload(config);
        noButton.reload(config);
    }

    @Override
    public void save() {
        config.set("cancel-order.title", title);
        config.set("cancel-order.can-close-with-escape", canCloseWithEsc);
        body.reload(config);
        yesButton.reload(config);
        noButton.reload(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("cancel-order.title", title);
        config.addDefault("cancel-order.can-close-with-escape", canCloseWithEsc);
        body.setDefault(config);
        yesButton.setDefault(config);
        noButton.setDefault(config);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.cancel-order.title");
        canCloseWithEsc = true;
        body.migrateV5(oldConfig, "gui.cancel-order.body");
        yesButton.migrateV5(oldConfig, "gui.cancel-order.confirm");
        noButton.migrateV5(oldConfig, "gui.cancel-order.cancel");
    }

    @Override
    public void applyDefaultValues() {
        title = "Cancel Order";
        canCloseWithEsc = true;
        yesButton.label = "<green>Confirm";
        yesButton.tooltip = "Click to confirm the cancellation of this order";
        yesButton.width = 150;
        noButton.label = "<red>Cancel";
        noButton.tooltip = "Click to cancel the cancellation of this order";
        noButton.width = 150;
        body.contents = "You are cancelling this order. It will be expired";
        body.width = 200;
    }
}
