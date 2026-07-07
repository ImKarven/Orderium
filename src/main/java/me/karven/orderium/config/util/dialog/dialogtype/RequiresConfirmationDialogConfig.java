package me.karven.orderium.config.util.dialog.dialogtype;

import org.jetbrains.annotations.NotNull;

public abstract class RequiresConfirmationDialogConfig extends ConfirmationDialogConfig {
    public String confirmTooltip;

    protected RequiresConfirmationDialogConfig(@NotNull String guiName) {
        super(guiName);
    }

    @Override
    public void reload() {
        super.reload();
        confirmTooltip = config.getString("confirm-tooltip");
    }

    @Override
    public void save() {
        super.save();
        config.set("confirm-tooltip", confirmTooltip);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        config.addDefault("confirm-tooltip", confirmTooltip);
    }
}
