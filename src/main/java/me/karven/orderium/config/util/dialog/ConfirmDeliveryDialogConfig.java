package me.karven.orderium.config.util.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.component.dialog.DialogButtonConfig;
import me.karven.orderium.config.util.component.dialog.MessageDialogBodyConfig;
import me.karven.orderium.config.util.dialog.dialogtype.ConfirmationDialogConfig;
import me.karven.orderium.utils.Values;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO: Add container alternative for all dialogs for geyser support
// TODO: MORE CUSTOMIZABILITY
@SuppressWarnings("UnstableApiUsage")
public class ConfirmDeliveryDialogConfig extends ConfirmationDialogConfig {
    public final @NotNull MessageDialogBodyConfig textBody = new MessageDialogBodyConfig("bodies.text");
    public final @NotNull MessageDialogBodyConfig transactionBody = new MessageDialogBodyConfig("bodies.transaction");

    public ConfirmDeliveryDialogConfig() {
        super("confirm-delivery");

        yesButton = new DialogButtonConfig("buttons.confirm");
        noButton = new DialogButtonConfig("buttons.cancel");
    }

    public @NotNull Dialog dialog(final @NotNull ItemStack item,
                                  final @NotNull String formattedAmount, final @NotNull String formattedReward,
                                  final @NotNull DialogActionCallback yesAction, final @NotNull DialogActionCallback noAction) {
        final TagResolver amountPlaceholder = Placeholder.unparsed("amount", formattedAmount);
        final TagResolver rewardPlaceholder = Placeholder.unparsed("money", formattedReward);
        return Dialog.create(builder -> builder.empty()
                .type(DialogType.confirmation(
                        yesButton.button(yesAction, amountPlaceholder),
                        noButton.button(noAction, amountPlaceholder)
                ))
                .base(DialogBase.builder(Values.minimessage.deserialize(title, amountPlaceholder))
                        .canCloseWithEscape(canCloseWithEsc)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(
                                textBody.body(amountPlaceholder, rewardPlaceholder),
                                DialogBody.item(item, DialogBody.plainMessage(Component.text(formattedAmount)), true, true, 16, 16),
                                transactionBody.body(amountPlaceholder, rewardPlaceholder)
                        ))
                        .build()
                )
        );
    }

    @Override
    public void reload() {
        super.reload();
        textBody.reload(config);
        transactionBody.reload(config);
    }

    @Override
    public void save() {
        super.save();
        textBody.save(config);
        transactionBody.save(config);
    }

    @Override
    public void setDefault() {
        super.setDefault();
        textBody.setDefault(config);
        transactionBody.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.confirm-delivery.title");
        canCloseWithEsc = true;
        yesButton.migrateV5(oldConfig, "gui.confirm-delivery.confirm");
        noButton.migrateV5(oldConfig, "gui.confirm-delivery.cancel");
        textBody.migrateV5(oldConfig, "gui.confirm-delivery.body");
        transactionBody.migrateV5(oldConfig, "gui.confirm-delivery.transaction-message");

        saveToFile();
    }

    @Override
    public void applyDefaultValues() {
        super.applyDefaultValues();
        title = "Confirm your Delivery";

        yesButton.label = "<green>Confirm";
        yesButton.tooltip = "Click to confirm the delivery";
        yesButton.width = 150;
        noButton.label = "<red>Cancel";
        noButton.tooltip = "Click to cancel the delivery";
        noButton.width = 150;
        textBody.contents = "You are delivering...";
        textBody.width = 200;
        transactionBody.contents = "You will get <green>$<money><white> in return";
        transactionBody.width = 200;
    }
}
