package me.karven.orderium.config.util.dialog.dialogtype;

import me.karven.orderium.config.util.GUIConfigFile;
import org.jetbrains.annotations.NotNull;

public abstract class DialogConfig extends GUIConfigFile {
    public String title;
    public boolean canCloseWithEsc;

    public DialogConfig(final @NotNull String guiName) {
        super(guiName);
    }

    @Override
    public void reload() {
        title = config.getString("title");
        canCloseWithEsc = config.getBoolean("can-close-with-escape");
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
