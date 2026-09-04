package me.karven.orderium.config.util.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.component.dialog.DialogButtonConfig;
import me.karven.orderium.config.util.component.dialog.TextDialogInputConfig;
import me.karven.orderium.config.util.dialog.dialogtype.ConfirmationDialogConfig;
import me.karven.orderium.utils.Values;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class SearchDialogConfig extends ConfirmationDialogConfig {
    public final @NotNull TextDialogInputConfig searchInputConfig = new TextDialogInputConfig("inputs.search");

    public SearchDialogConfig() {
        super("search-dialog");
        yesButton = new DialogButtonConfig("buttons.confirm");
        noButton = new DialogButtonConfig("buttons.cancel");
    }

    public @NotNull Dialog dialog(final @NotNull DialogActionCallback yesAction, final @NotNull DialogActionCallback noAction) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase
                        .builder(Values.minimessage.deserialize(title))
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .pause(false)
                        .canCloseWithEscape(canCloseWithEsc)
                        .inputs(List.of(
                                DialogInput.text(
                                        "search",
                                        searchInputConfig.width,
                                        Values.minimessage.deserialize(searchInputConfig.label),
                                        searchInputConfig.labelVisible,
                                        searchInputConfig.initial,
                                        searchInputConfig.maxLength,
                                        null
                                )
                        ))
                        .build()
                )
                .type(DialogType.confirmation(
                        yesButton.button(yesAction),
                        noButton.button(noAction)
                ))
        );
    }

    @Override
    public void reload() {
        super.reload();
        searchInputConfig.reload(config);
    }

    @Override
    public void save() {
        super.save();
        searchInputConfig.save(config);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        searchInputConfig.setDefault(config);
    }

    @Override
    public void applyDefaultValues() {
        super.applyDefaultValues();
        title = "Search";
        yesButton.label = "<green>Confirm";
        yesButton.tooltip = "Click to search";
        yesButton.width = 150;
        noButton.label = "<red>Cancel";
        noButton.tooltip = "Click to cancel";
        noButton.width = 150;
        searchInputConfig.initial = "";
        searchInputConfig.label = "Search";
        searchInputConfig.labelVisible = true;
        searchInputConfig.maxLength = 32;
        searchInputConfig.width = 200;
    }
}
