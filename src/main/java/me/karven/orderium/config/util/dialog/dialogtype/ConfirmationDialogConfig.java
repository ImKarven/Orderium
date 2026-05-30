package me.karven.orderium.config.util.dialog.dialogtype;

import me.karven.orderium.config.util.DialogButtonConfig;
import org.jetbrains.annotations.NotNull;

public abstract class ConfirmationDialogConfig extends DialogConfig {
    public DialogButtonConfig yesButton;
    public DialogButtonConfig noButton;

    protected ConfirmationDialogConfig(final @NotNull String guiName) {
        super(guiName);
    }

    @Override
    public void reload() {
        super.reload();
        yesButton.reload(config);
        noButton.reload(config);
    }
}
