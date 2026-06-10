package me.karven.orderium.config.util.dialog.dialogtype;

import me.karven.orderium.config.util.component.dialog.DialogButtonConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class ConfirmationDialogConfig extends DialogConfigFile {
    public DialogButtonConfig yesButton;
    public DialogButtonConfig noButton;

    protected ConfirmationDialogConfig(final @NotNull String guiName) {
        super(guiName);
    }

    @Override
    public void reload() throws IOException {
        super.reload();
        yesButton.reload(config);
        noButton.reload(config);
    }

    @Override
    public void save() {
        super.save();
        yesButton.save(config);
        noButton.save(config);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        yesButton.setDefault(config);
        noButton.setDefault(config);
    }
}
