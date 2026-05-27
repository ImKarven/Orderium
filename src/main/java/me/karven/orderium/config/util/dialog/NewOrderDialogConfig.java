package me.karven.orderium.config.util.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.DialogButtonConfig;
import me.karven.orderium.config.util.ItemlessItemDialogBodyConfig;
import me.karven.orderium.config.util.TextDialogInputConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialogConfig extends ConfirmationDialogConfig {
    private static final @NotNull ClickCallback.Options DEFAULT_OPTIONS = ClickCallback.Options.builder().build();
    public final @NotNull ItemlessItemDialogBodyConfig bodyConfig = new ItemlessItemDialogBodyConfig("body");
    public final @NotNull TextDialogInputConfig amountInputConfig = new TextDialogInputConfig("inputs.amount");
    public final @NotNull TextDialogInputConfig moneyPerItemInputConfig = new TextDialogInputConfig("inputs.money-per-item");

    public NewOrderDialogConfig() {
        super("new-order-dialog");
        yesButton = new DialogButtonConfig("buttons.confirm");
        noButton = new DialogButtonConfig("buttons.change-item");
    }

    public @NotNull Dialog dialog(final @NotNull ItemStack item, final @NotNull DialogActionCallback yesAction, final @NotNull DialogActionCallback noAction) {
        final MiniMessage mm = MiniMessage.miniMessage();
        return Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase
                                .builder(mm.deserialize(title))
                                .body(List.of(bodyConfig.body(item)))
                                .canCloseWithEscape(canCloseWithEsc)
                                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                                .inputs(List.of(amountInputConfig.input("amount"), moneyPerItemInputConfig.input("money-per-item")))
                                .build()
                        )
                        .type(DialogType.confirmation(
                                ActionButton
                                        .builder(mm.deserialize(yesButton.label))
                                        .tooltip(mm.deserialize(yesButton.tooltip))
                                        .width(yesButton.width)
                                        .action(DialogAction.customClick(yesAction, DEFAULT_OPTIONS))
                                        .build(),
                                ActionButton
                                        .builder(mm.deserialize(noButton.label))
                                        .tooltip(mm.deserialize(noButton.tooltip))
                                        .width(noButton.width)
                                        .action(DialogAction.customClick(noAction, DEFAULT_OPTIONS))
                                        .build()
                        ))
        );
    }

    @Override
    public void reload() {
        super.reload();
        bodyConfig.reload(config);
    }

    @Override
    public void save() {
        super.save();
        bodyConfig.save(config);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        bodyConfig.setDefault(config);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        bodyConfig.migrateV5(oldConfig, "gui.new-order.item-description");
        yesButton.migrateV5(oldConfig, "gui.new-order.confirm");
        noButton.migrateV5(oldConfig, "gui.new-order.change-item");
        canCloseWithEsc = true;
    }

    @Override
    public void applyDefaultValues() {
        super.applyDefaultValues();
        title = "Create A New Order";
        bodyConfig.description.body = DialogBody.plainMessage(Component.text("You're creating an order for this item"), 210);
        bodyConfig.showDecoration = true;
        bodyConfig.showTooltip = true;
        bodyConfig.width = 16;
        bodyConfig.height = 16;
        yesButton.label = "<green>Confirm";
        yesButton.tooltip = "Click to confirm the order";
        yesButton.width = 150;
        noButton.label = "Change Item...";
        noButton.tooltip = "Click to change the item";
        noButton.width = 150;
    }
}
