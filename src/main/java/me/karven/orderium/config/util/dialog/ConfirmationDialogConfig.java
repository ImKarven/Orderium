package me.karven.orderium.config.util.dialog;

import me.karven.orderium.config.util.DialogButtonConfig;
import me.karven.orderium.config.util.GUIConfigFile;
import org.jetbrains.annotations.NotNull;

public abstract class ConfirmationDialogConfig extends GUIConfigFile {
    public String title;
    public boolean canCloseWithEsc;
    public DialogButtonConfig yesButton;
    public DialogButtonConfig noButton;

    protected ConfirmationDialogConfig(@NotNull String guiName) {
        super(guiName);
    }

    @Override
    public void reload() {
        title = config.getString("title");
        canCloseWithEsc = config.getBoolean("can-close-with-escape");
        yesButton.reload(config);
        noButton.reload(config);
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("can-close-with-escape", canCloseWithEsc);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("can-close-with-escape", canCloseWithEsc);
    }

    @Override
    public void applyDefaultValues() {
        canCloseWithEsc = true;
    }
}
