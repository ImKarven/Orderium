package me.karven.orderium.config.util.dialog.mangeorderdialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.DialogButtonConfig;
import me.karven.orderium.config.util.dialog.dialogtype.DialogConfig;
import me.karven.orderium.utils.Values;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialogConfig extends DialogConfig {
    public final @NotNull DialogButtonConfig collectItemsButtonConfig = new DialogButtonConfig("manage-order.buttons.collect-items");
    public final @NotNull DialogButtonConfig cancelOrderButtonConfig = new DialogButtonConfig("manage-order.buttons.cancel-order");

    public ManageOrderDialogConfig() {
        super("manage-order");
    }

    public @NotNull Dialog dialog(
            final @NotNull DialogActionCallback clickCollectItemsButtonAction,
            final @NotNull DialogActionCallback clickCancelOrderButtonAction
    ) {
        return Dialog.create(builder -> builder.empty()
                .type(DialogType.multiAction(List.of(
                        collectItemsButtonConfig.button(clickCollectItemsButtonAction),
                        cancelOrderButtonConfig.button(clickCancelOrderButtonAction)
                )).build())
                .base(DialogBase.builder(Values.minimessage.deserialize(title))
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .canCloseWithEscape(canCloseWithEsc)
                        .build()
                )
        );
    }

    @Override
    public void reload() {
        title = config.getString("manage-order.title");
        canCloseWithEsc = config.getBoolean("manage-order.can-close-with-escape");
        collectItemsButtonConfig.reload(config);
        cancelOrderButtonConfig.reload(config);
    }

    @Override
    public void save() {
        config.set("manage-order.title", title);
        config.set("manage-order.can-close-with-escape", canCloseWithEsc);
        collectItemsButtonConfig.save(config);
        cancelOrderButtonConfig.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("manage-order.title", title);
        config.addDefault("manage-order.can-close-with-escape", canCloseWithEsc);
        collectItemsButtonConfig.setDefault(config);
        cancelOrderButtonConfig.setDefault(config);
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.manage-order.title");
        canCloseWithEsc = true;
        collectItemsButtonConfig.migrateV5(oldConfig, "gui.manage-order.collect-items-");
        cancelOrderButtonConfig.migrateV5(oldConfig, "gui.manage-order.cancel-order-");
    }

    @Override
    public void applyDefaultValues() {
        canCloseWithEsc = true;
        title = "Manage Order";
        collectItemsButtonConfig.label = "Collect Items";
        collectItemsButtonConfig.tooltip = "Click to collect items from this order";
        collectItemsButtonConfig.width = 150;
        cancelOrderButtonConfig.label = "Cancel Order";
        cancelOrderButtonConfig.tooltip = "Click to cancel the order";
        cancelOrderButtonConfig.width = 150;
    }
}
