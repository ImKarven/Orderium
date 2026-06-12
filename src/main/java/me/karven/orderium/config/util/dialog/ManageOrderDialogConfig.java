package me.karven.orderium.config.util.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.karven.orderium.config.util.GUIConfigFile;
import me.karven.orderium.config.util.IConfigFile;
import me.karven.orderium.config.util.component.dialog.DialogButtonConfig;
import me.karven.orderium.config.util.component.dialog.ItemlessItemDialogBodyConfig;
import me.karven.orderium.config.util.component.dialog.TextDialogInputConfig;
import me.karven.orderium.utils.Log;
import me.karven.orderium.utils.Values;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialogConfig extends GUIConfigFile {
    public final ManageOrder manageOrder = new ManageOrder(this);
    public final CollectItems collectItems = new CollectItems(this);
    public final CancelOrder cancelOrder = new CancelOrder(this);

    public ManageOrderDialogConfig() {
        super("manage-order");
    }

    @Override
    public void reload() {
        manageOrder.reload();
        collectItems.reload();
        cancelOrder.reload();
    }

    @Override
    public void save() {
        manageOrder.save();
        collectItems.save();
        cancelOrder.save();
    }

    @Override
    public void setDefault() {
        manageOrder.setDefault();
        collectItems.setDefault();
        cancelOrder.setDefault();
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        manageOrder.migrateV5(oldConfig);
        collectItems.migrateV5(oldConfig);
        cancelOrder.migrateV5(oldConfig);

        save();

        try {
            config.save();
        } catch (Exception e) {
            Log.error("Failed to migrate config", e);
        }
    }

    @Override
    public void applyDefaultValues() {
        manageOrder.applyDefaultValues();
        collectItems.applyDefaultValues();
        cancelOrder.applyDefaultValues();
    }

    public static class ManageOrder implements IConfigFile {
        private final ManageOrderDialogConfig parent;
        public String title;
        public boolean canCloseWithEsc;
        public final @NotNull DialogButtonConfig collectItemsButtonConfig = new DialogButtonConfig("manage-order.buttons.collect-items");
        public final @NotNull DialogButtonConfig cancelOrderButtonConfig = new DialogButtonConfig("manage-order.buttons.cancel-order");

        public ManageOrder(final @NotNull ManageOrderDialogConfig parent) {
            this.parent = parent;
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
                            .pause(false)
                            .afterAction(DialogBase.DialogAfterAction.NONE)
                            .canCloseWithEscape(canCloseWithEsc)
                            .build()
                    )
            );
        }

        @Override
        public void reload() {
            title = parent.config.getString("manage-order.title");
            canCloseWithEsc = parent.config.getBoolean("manage-order.can-close-with-escape");
            collectItemsButtonConfig.reload(parent.config);
            cancelOrderButtonConfig.reload(parent.config);
        }

        @Override
        public void save() {
            parent.config.set("manage-order.title", title);
            parent.config.set("manage-order.can-close-with-escape", canCloseWithEsc);
            collectItemsButtonConfig.save(parent.config);
            cancelOrderButtonConfig.save(parent.config);
        }

        @Override
        public void setDefault() {
            parent.config.addDefault("manage-order.title", title);
            parent.config.addDefault("manage-order.can-close-with-escape", canCloseWithEsc);
            collectItemsButtonConfig.setDefault(parent.config);
            cancelOrderButtonConfig.setDefault(parent.config);
        }

        @Override
        public void migrateV5(@NotNull ConfigFile oldConfig) {
            title = oldConfig.getString("gui.manage-order.title");
            canCloseWithEsc = true;
            collectItemsButtonConfig.migrateV5(oldConfig, "gui.manage-order.collect-items");
            cancelOrderButtonConfig.migrateV5(oldConfig, "gui.manage-order.cancel-order");
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

    public static class CollectItems implements IConfigFile {
        private final ManageOrderDialogConfig parent;
        public String title;
        public boolean canCloseWithEsc;
        public final @NotNull DialogButtonConfig yesButton = new DialogButtonConfig("collect-items.buttons.confirm");
        public final @NotNull DialogButtonConfig noButton = new DialogButtonConfig("collect-items.buttons.cancel");
        public final @NotNull ItemlessItemDialogBodyConfig body = new ItemlessItemDialogBodyConfig("collect-items.body");
        public final @NotNull TextDialogInputConfig amountInputConfig = new TextDialogInputConfig("collect-items.amount-input");

        public CollectItems(final @NotNull ManageOrderDialogConfig parent) {
            this.parent = parent;
        }

        public @NotNull Dialog dialog(final @NotNull ItemStack item, final @NotNull TagResolver @NotNull [] placeholders, final @Nullable DialogActionCallback yesAction, final @Nullable DialogActionCallback noAction) {
            return Dialog.create(builder -> builder.empty()
                    .type(DialogType.confirmation(
                            yesButton.button(yesAction),
                            noButton.button(noAction)
                    ))
                    .base(DialogBase.builder(Values.minimessage.deserialize(title))
                            .canCloseWithEscape(canCloseWithEsc)
                            .body(List.of(body.body(item, placeholders)))
                            .inputs(List.of(amountInputConfig.input("amount")))
                            .build()
                    )
            );
        }

        @Override
        public void reload() {
            title = parent.config.getString("collect-items.title");
            canCloseWithEsc = parent.config.getBoolean("collect-items.can-close-with-escape");
            body.reload(parent.config);
            yesButton.reload(parent.config);
            noButton.reload(parent.config);
            amountInputConfig.reload(parent.config);
        }

        @Override
        public void save() {
            parent.config.set("collect-items.title", title);
            parent.config.set("collect-items.can-close-with-escape", canCloseWithEsc);
            body.save(parent.config);
            yesButton.save(parent.config);
            noButton.save(parent.config);
            amountInputConfig.save(parent.config);
        }

        @Override
        public void setDefault() {
            parent.config.addDefault("collect-items.title", title);
            parent.config.addDefault("collect-items.can-close-with-escape", canCloseWithEsc);
            body.setDefault(parent.config);
            yesButton.setDefault(parent.config);
            noButton.setDefault(parent.config);
            amountInputConfig.setDefault(parent.config);
        }

        @Override
        public void migrateV5(@NotNull ConfigFile oldConfig) {
            title = oldConfig.getString("gui.collect-items.title");
            canCloseWithEsc = true;
            body.migrateV5(oldConfig, "gui.collect-items.body");
            yesButton.migrateV5(oldConfig, "gui.collect-items.confirm");
            noButton.migrateV5(oldConfig, "gui.collect-items.cancel");
            amountInputConfig.migrateV5(oldConfig, "gui.collect-items.amount-label");
        }

        @Override
        public void applyDefaultValues() {
            title = "Collect Items";
            canCloseWithEsc = true;
            yesButton.label = "<green>Confirm";
            yesButton.tooltip = "Click to confirm";
            yesButton.width = 150;
            noButton.label = "<red>Cancel";
            noButton.tooltip = "Click to cancel";
            noButton.width = 150;
            body.description.contents = "You are collecting items from this order. You can collect up to <aqua><in-storage> <item>";
            body.description.width = 200;
            body.width = 16;
            body.height = 16;
            body.showDecoration = true;
            body.showTooltip = true;
            amountInputConfig.label = "Amount";
            amountInputConfig.width = 200;
            amountInputConfig.labelVisible = true;
            amountInputConfig.initial = "1";
            amountInputConfig.maxLength = 32;
        }
    }

    public static class CancelOrder implements IConfigFile {
        private final ManageOrderDialogConfig parent;
        public String title;
        public boolean canCloseWithEsc;
        public final @NotNull DialogButtonConfig yesButton = new DialogButtonConfig("collect-items.buttons.confirm");
        public final @NotNull DialogButtonConfig noButton = new DialogButtonConfig("collect-items.buttons.cancel");
        public final @NotNull ItemlessItemDialogBodyConfig body = new ItemlessItemDialogBodyConfig("cancel-order.body");

        public CancelOrder(final @NotNull ManageOrderDialogConfig parent) {
            this.parent = parent;
        }

        public @NotNull Dialog dialog(final @NotNull ItemStack item, final @NotNull DialogActionCallback yesAction, final @Nullable DialogActionCallback noAction) {
            return Dialog.create(builder -> builder.empty()
                    .type(DialogType.confirmation(
                            yesButton.button(yesAction),
                            noButton.button(noAction)
                    ))
                    .base(DialogBase.builder(Values.minimessage.deserialize(title))
                            .canCloseWithEscape(canCloseWithEsc)
                            .pause(false)
                            .afterAction(DialogBase.DialogAfterAction.NONE)
                            .body(List.of(body.body(item)))
                            .build()
                    )
            );
        }

        @Override
        public void reload() {
            title = parent.config.getString("cancel-order.title");
            canCloseWithEsc = parent.config.getBoolean("cancel-order.can-close-with-escape");
            body.reload(parent.config);
            yesButton.reload(parent.config);
            noButton.reload(parent.config);
        }

        @Override
        public void save() {
            parent.config.set("cancel-order.title", title);
            parent.config.set("cancel-order.can-close-with-escape", canCloseWithEsc);
            body.save(parent.config);
            yesButton.save(parent.config);
            noButton.save(parent.config);
        }

        @Override
        public void setDefault() {
            parent.config.addDefault("cancel-order.title", title);
            parent.config.addDefault("cancel-order.can-close-with-escape", canCloseWithEsc);
            body.setDefault(parent.config);
            yesButton.setDefault(parent.config);
            noButton.setDefault(parent.config);
        }

        @Override
        public void migrateV5(@NotNull ConfigFile oldConfig) {
            title = oldConfig.getString("gui.cancel-order.title");
            canCloseWithEsc = true;
            body.migrateV5(oldConfig, "gui.cancel-order.body");
            yesButton.migrateV5(oldConfig, "gui.cancel-order.confirm");
            noButton.migrateV5(oldConfig, "gui.cancel-order.cancel");
        }

        @Override
        public void applyDefaultValues() {
            title = "Cancel Order";
            canCloseWithEsc = true;
            yesButton.label = "<green>Confirm";
            yesButton.tooltip = "Click to confirm the cancellation of this order";
            yesButton.width = 150;
            noButton.label = "<red>Cancel";
            noButton.tooltip = "Click to cancel the cancellation of this order";
            noButton.width = 150;
            body.description.contents = "You are cancelling this order. It will be expired";
            body.description.width = 250;
            body.width = 16;
            body.height = 16;
            body.showDecoration = true;
            body.showTooltip = true;
        }
    }
}
