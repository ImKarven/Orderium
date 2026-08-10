package me.karven.orderium.gui.scrollable;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.DeliverGUI;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.gui.YourOrderGUI;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.PaginatedGUI;
import me.karven.orderium.guiframework.ScrollableGUI;
import me.karven.orderium.obj.ItemClickContext;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ScrollableMainGUI extends ScrollableGUI<Order> {
    private final Config config;
    private final String search;
    private final int sortIndex;

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public ScrollableMainGUI(final @NotNull Player player) {
        this(player, 0, "");
    }

    public ScrollableMainGUI(final @NotNull Player player, final String search) {
        this(player, 0, search);
    }

    public ScrollableMainGUI(final @NotNull Player player, final int sortIndex, final @NotNull String search) {
        final Config config = Config.config;

        final List<Order> activeOrders = DataCache.getInstance().getSortedOrders(config.mainGUIConfig.sortsOrderConfig.index(sortIndex)).stream().filter(Order::isActive).toList();
        final List<Order> matchedOrders = search.isEmpty() ? activeOrders : AlgoUtils.searchOrder(search, activeOrders);
        final Consumer<ItemClickContext<Order>> clickAction = context -> {
            final InventoryClickEvent event = context.event();
            final Order order = context.object();
            if (event.getClick() == ClickType.RIGHT && player.hasPermission("orderium.admin.edit-orders")) {
                final Dialog dialog = AdminToolGUI.createEditOrder(context.object());
                player.showDialog(dialog);
                return;
            }
            if (player.getUniqueId().equals(order.getOwnerUniqueId())) {
                player.sendRichMessage(config.deliverSelf);
                return;
            }
            final InventoryGUI deliverGUI = new DeliverGUI(order).getGUI();
            deliverGUI.open(player);
        };
        super(
                config.mainGUIConfig.rows,
                9, // TODO: .
                mm.deserialize(config.mainGUIConfig.title),
                matchedOrders,
                Order::mainGUIItemStack,
                clickAction,
                player,
                config.mainGUIConfig.orderConfig.slots
        );
        this.config = config;
        this.search = search;
        this.sortIndex = sortIndex;
    }

    @Override
    protected void populateButtons(final @NotNull InventoryGUI gui) {
        final int index = this.currentIndex;

        gui.addItem(
                config.mainGUIConfig.refreshButton.item(_ -> {
                    final ScrollableMainGUI mainGUI = new ScrollableMainGUI(player, sortIndex, search);
                    mainGUI.skip(index);
                    mainGUI.open();
                    PlayerUtils.playSound(player, config.refreshSound);

                }),
                config.mainGUIConfig.refreshButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.sortButton.item(_ -> {
                    final ScrollableMainGUI mainGUI = new ScrollableMainGUI(player, sortIndex + 1 == config.mainGUIConfig.sortsOrderConfig.orderArray.size() ? 0 : sortIndex + 1, search);
                    mainGUI.skip(index);
                    mainGUI.open();
                    PlayerUtils.playSound(player, config.sortSound);

                }, config.mainGUIConfig.sortsOrderConfig.index(sortIndex)),
                config.mainGUIConfig.sortButton.slot
        );
        gui.addItem(
                config.mainGUIConfig.searchButton.item(_ -> SignGUI.newSession(
                        player,
                        (s) -> DispatchUtil.entity(player, () -> {
                            final ScrollableMainGUI mainGUI = new ScrollableMainGUI(player, s);
                            mainGUI.open();
                        }),
                        config.signGUIConfig.signLines, config.signGUIConfig.signType(), config.signGUIConfig.queryLine
                )),
                config.mainGUIConfig.searchButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.yourOrdersButton.item(_ -> YourOrderGUI.open(player)),
                config.mainGUIConfig.yourOrdersButton.slot
        );
    }

}
