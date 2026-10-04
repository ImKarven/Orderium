package me.karven.orderium.order;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import me.karven.orderium.api.events.PlayerDeliverOrderEvent;
import me.karven.orderium.config.Config;
import me.karven.orderium.gui.DeliverGUI;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.SearchableItem;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.EconUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.utils.ConvertUtils.formatNumber;

public final class ItemOrder extends Order {

    // The data is the serialized item. Searchable items are stored with their search strings, see OrderItem#fromBytes
    public static final OrderType<ItemOrder> TYPE = new OrderType<>() {
        @Override
        public String key() {
            return "item";
        }

        @Override
        public ItemOrder decode(int id, UUID owner, byte[] data, OrderState state) {
            return new ItemOrder(id, owner, OrderItem.fromBytes(data), state);
        }
    };

    private final OrderItem item;

    private ItemOrder(int id, UUID owner, OrderItem item, OrderState state) {
        super(id, owner, state);
        this.item = item;
    }

    public static OrderDraft<ItemOrder> draft(final UUID owner, final OrderItem item, final OrderState state) {
        final ItemStack itemStack = item instanceof SearchableItem searchableItem ? searchableItem.getParsedItemStack() : item.getItemStack();
        return new OrderDraft<>(TYPE, owner, itemStack.serializeAsBytes(), state, id -> new ItemOrder(id, owner, item, state));
    }

    // Must be called in the player region
    public static Response create(Player owner, OrderItem item, double moneyPer, int amount) {
        final long expiresAt = System.currentTimeMillis() + Config.config.expiresAfter;
        final OrderDraft<ItemOrder> draft = draft(owner.getUniqueId(), item, OrderState.initial(moneyPer, amount, expiresAt));

        // TODO: Provide OrderItem to API
        return submit(owner, draft, item.getItemStack());
    }

    // Must be called in the player region
    public void deliver(Player p, Iterable<ItemStack> items, boolean isAsync) {
        PlayerDeliverOrderEvent.Pre preEvent = new PlayerDeliverOrderEvent.Pre(p, this, isAsync);
        if (!preEvent.callEvent()) {
            giveBack(p, items);
            return;
        }

        final ItemStack comparer = item.getItemStack();
        // Read once, so counting and taking the items can't disagree if the config is reloaded in between
        final boolean shulkerDelivering = Config.config.shulkerDelivering;
        final int offered = countDeliverable(items, comparer, shulkerDelivering);

        plugin.getOrderService().deliver(this, offered)
                .whenComplete((delivery, exception) -> {
                    if (exception != null) {
                        giveBack(p, items);
                        return;
                    }

                    final List<ItemStack> returnedItems = takeDelivered(items, comparer, delivery.units(), shulkerDelivering);
                    if (!returnedItems.isEmpty()) PlayerUtils.give(p, returnedItems, true);
                    if (delivery.units() == 0) return;

                    final double moneyReceived = delivery.payout();
                    final Config config = Config.config;
                    EconUtils.addMoney(p, moneyReceived);
                    p.sendRichMessage(config.deliver, Placeholder.unparsed("money", formatNumber(moneyReceived)));
                    PlayerUtils.playSound(p, config.deliverSound);

                    if (config.webhookConfig.deliverOrderOption.enabled) {
                        config.webhookConfig.deliverOrderOption.send(stringPlaceholders(), "<deliverer>", p.getName());
                    }

                    final PlayerDeliverOrderEvent.Post postEvent = new PlayerDeliverOrderEvent.Post(p, this, !Bukkit.isPrimaryThread());

                    final Player ownerPlayer = Bukkit.getPlayer(getOwnerUniqueId());
                    if (ownerPlayer == null || !ownerPlayer.isOnline()) {
                        reload();
                        postEvent.callEvent();
                        return;
                    }
                    final ItemStack itemStack = item.getItemStack();
                    final ItemMeta meta = itemStack.getItemMeta();
                    final Component displayName = meta == null ? null : meta.displayName();
                    assert itemStack.getType().getItemTranslationKey() != null;
                    ownerPlayer.sendRichMessage(
                            config.receiveDelivery,
                            Placeholder.unparsed("deliverer", p.getName()),
                            Placeholder.unparsed("amount", formatNumber(delivery.units())),
                            Placeholder.component("item", (displayName == null ? Component.translatable(itemStack.getType().getItemTranslationKey()) : displayName))
                    );
                    postEvent.callEvent();
                });
    }

    private static void giveBack(Player p, Iterable<ItemStack> items) {
        final List<ItemStack> returnedItems = new ArrayList<>();
        items.forEach(returnedItems::add);
        PlayerUtils.give(p, returnedItems, true);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static int countDeliverable(Iterable<ItemStack> items, ItemStack comparer, boolean shulkerDelivering) {
        int count = 0;
        for (final ItemStack item : items) {
            if (item == null || item.isEmpty()) continue;
            if (AlgoUtils.isSimilar(item, comparer)) {
                count += item.getAmount();
                continue;
            }
            if (!shulkerDelivering || !isShulkerBox(item)) continue;
            final ItemContainerContents shulkerContent = item.getData(DataComponentTypes.CONTAINER);
            if (shulkerContent == null) continue;
            for (final ItemStack content : shulkerContent.contents()) {
                if (content == null || content.isEmpty()) continue;
                if (AlgoUtils.isSimilar(content, comparer)) count += content.getAmount();
            }
        }
        return count;
    }

    // Must take items exactly the way countDeliverable counts them, otherwise the deliverer is paid for items they get back
    private static List<ItemStack> takeDelivered(Iterable<ItemStack> items, ItemStack comparer, int units, boolean shulkerDelivering) {
        int deliverable = units;
        final List<ItemStack> returnedItems = new ArrayList<>();
        for (final ItemStack original : items) {
            final ItemStack item = original.clone();
            if (!AlgoUtils.isSimilar(item, comparer)) {
                if (shulkerDelivering && isShulkerBox(item)) {
                    deliverable = takeFromShulkerBox(item, comparer, deliverable);
                }
                returnedItems.add(item);
                continue;
            }
            int itemAmount = item.getAmount();
            if (deliverable >= itemAmount) {
                deliverable -= itemAmount;
                continue;
            }
            item.setAmount(itemAmount - deliverable);
            returnedItems.add(item);
            deliverable = 0;
        }
        return returnedItems;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static int takeFromShulkerBox(ItemStack shulkerBox, ItemStack comparer, int deliverable) {
        ItemContainerContents shulkerContent = shulkerBox.getData(DataComponentTypes.CONTAINER);
        List<ItemStack> declinedItems = new ArrayList<>();
        if (shulkerContent == null) return deliverable;
        for (final ItemStack item : shulkerContent.contents()) {
            if (item == null || item.isEmpty()) {
                // Add empty items to keep the order of the items in the shulker
                declinedItems.add(ItemStack.empty());
                continue;
            }
            if (deliverable == 0) {
                declinedItems.add(item);
                continue;
            }
            if (!AlgoUtils.isSimilar(item, comparer)) {
                declinedItems.add(item);
                continue;
            }
            int itemAmount = item.getAmount();
            if (deliverable >= itemAmount) {
                deliverable -= itemAmount;
                declinedItems.add(ItemStack.empty());
                continue;
            }
            item.setAmount(itemAmount - deliverable);
            deliverable = 0;
            declinedItems.add(item);
        }
        ItemContainerContents contentAfterScan = ItemContainerContents.containerContents(declinedItems);
        shulkerBox.setData(DataComponentTypes.CONTAINER, contentAfterScan);
        return deliverable;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static boolean isShulkerBox(ItemStack item) {
        return item.hasData(DataComponentTypes.CONTAINER);
    }

    public OrderItem getOrderItem() {
        return item;
    }

    @Override
    public OrderType<ItemOrder> getType() {
        return TYPE;
    }

    @Override
    public ItemStack getIcon() {
        return item.getItemStack();
    }

    @Override
    protected Component displayName() {
        final ItemStack itemStack = item.getItemStack();
        final ItemMeta meta = itemStack.getItemMeta();
        Component itemName = meta.hasCustomName() ? meta.customName() : Component.translatable(itemStack.translationKey());
        if (itemName == null) itemName = Component.translatable(itemStack.translationKey());
        return itemName.hoverEvent(itemStack.asHoverEvent());
    }

    @Override
    protected String plainDisplayName() {
        final ItemStack itemStack = item.getItemStack();
        final ItemMeta meta = itemStack.getItemMeta();
        final Component customName = meta.customName();
        final String itemName = meta.hasCustomName() && customName != null ? PlainTextComponentSerializer.plainText().serialize(customName) : itemStack.getI18NDisplayName();
        assert itemName != null;
        return itemName;
    }

    @Override
    public boolean matchesSearch(String query) {
        return AlgoUtils.searchWrappedItem(item, query);
    }

    @Override
    public void openDelivery(Player player) {
        new DeliverGUI(this).getGUI().open(player);
    }

    @Override
    protected void handOut(Player owner, int amount) {
        PlayerUtils.give(owner, item.getItemStack(), amount, true);
    }
}
