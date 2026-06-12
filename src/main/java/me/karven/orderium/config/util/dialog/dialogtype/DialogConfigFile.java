package me.karven.orderium.config.util.dialog.dialogtype;

import me.karven.orderium.config.util.GUIConfigFile;
import me.karven.orderium.utils.Log;
import org.jetbrains.annotations.NotNull;

public abstract class DialogConfigFile extends GUIConfigFile {
    public String title;
    public boolean canCloseWithEsc;

    public DialogConfigFile(final @NotNull String guiName) {
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

    public void saveToFile() {
        save();

        try {
            config.save();
        } catch (Exception e) {
            Log.error("Failed to save config to file", e);
        }
    }
}
