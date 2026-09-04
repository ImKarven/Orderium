package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import me.karven.orderium.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class SearchDialog {
    private static final DialogActionCallback cancelAction = (_, audience) -> audience.closeDialog();

    public static @NotNull Dialog getDialog(final @NotNull Consumer<@NotNull String> action) {
        final DialogActionCallback searchAction = (view, audience) -> {
            final String search = view.getText("search");
            if (search == null) {
                audience.closeDialog();
                return;
            }

            action.accept(search);
        };

        return Config.config.searchDialogConfig.dialog(searchAction, cancelAction);
    }
}
