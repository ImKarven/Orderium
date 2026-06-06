package me.karven.orderium.config.util.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.component.dialog.DialogButtonConfig;
import me.karven.orderium.config.util.component.dialog.ItemlessItemDialogBodyConfig;
import me.karven.orderium.config.util.component.dialog.TextDialogInputConfig;
import me.karven.orderium.config.util.dialog.dialogtype.ConfirmationDialogConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialogConfig extends ConfirmationDialogConfig {
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
                                .inputs(List.of(amountInputConfig.input("amount"), moneyPerItemInputConfig.input("money_per")))
                                .build()
                        )
                        .type(DialogType.confirmation(
                                yesButton.button(yesAction),
                                noButton.button(noAction)
                        ))
        );
    }

    @Override
    public void reload() throws IOException {
        super.reload();
        bodyConfig.reload(config);
        amountInputConfig.reload(config);
        moneyPerItemInputConfig.reload(config);
    }

    @Override
    public void save() {
        super.save();
        bodyConfig.save(config);
        amountInputConfig.save(config);
        moneyPerItemInputConfig.save(config);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        bodyConfig.setDefault(config);
        amountInputConfig.setDefault(config);
        moneyPerItemInputConfig.setDefault(config);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        bodyConfig.migrateV5(oldConfig, "gui.new-order.item-description");
        bodyConfig.showDecoration = false;
        yesButton.migrateV5(oldConfig, "gui.new-order.confirm");
        noButton.migrateV5(oldConfig, "gui.new-order.change-item");
        canCloseWithEsc = true;
        amountInputConfig.migrateV5(oldConfig, "gui.new-order.amount-label");
        moneyPerItemInputConfig.migrateV5(oldConfig, "gui.new-order.money-per-label");

        saveToFile();
    }

    @Override
    public void applyDefaultValues() {
        super.applyDefaultValues();
        title = "Create A New Order";
        bodyConfig.description.contents = "You're creating an order for this item";
        bodyConfig.description.width = 210;
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
        amountInputConfig.label = "Amount";
        amountInputConfig.width = 200;
        amountInputConfig.initial = "1";
        amountInputConfig.labelVisible = true;
        amountInputConfig.maxLength = 32;
        moneyPerItemInputConfig.label = "Amount";
        moneyPerItemInputConfig.width = 200;
        moneyPerItemInputConfig.initial = "1";
        moneyPerItemInputConfig.labelVisible = true;
        moneyPerItemInputConfig.maxLength = 32;
    }
}
