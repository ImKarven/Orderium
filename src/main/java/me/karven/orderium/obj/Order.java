package me.karven.orderium.obj;

import me.karven.orderium.api.events.PlayerCancelOrderEvent;
import me.karven.orderium.api.events.PlayerCollectItemsEvent;
import me.karven.orderium.api.events.PlayerCreateOrderEvent;
import me.karven.orderium.api.events.PlayerDeliverOrderEvent;
import me.karven.orderium.config.Config;
import me.karven.orderium.gui.YourOrderGUI;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.utils.ConvertUtils.formatNumber;

// TODO: Replace `item` with OrderItem instead of ItemStack.
// Problem: how to store it in database?
public class Order implements me.karven.orderium.api.Order {
    private final @NotNull OfflinePlayer ownerPlayer;
    private final @Nullable String ownerName;
    private ItemStack mainGUIItemStack;
    private ItemStack yourOrdersGUIItemStack;
    public final int id;
    public final UUID owner;
    public final ItemStack item;
    public double moneyPer;
    public int amount;
    public int delivered;
    public int inStorage;
    public long expiresAt;

    public Order(int id, UUID owner, ItemStack item, double moneyPer, int amount, int delivered, int inStorage, long expiresAt) {
        this.id = id;
        this.owner = owner;
        this.item = item;
        this.moneyPer = moneyPer;
        this.amount = amount;
        this.delivered = delivered;
        this.inStorage = inStorage;
        this.expiresAt = expiresAt;

        this.ownerPlayer = Bukkit.getOfflinePlayer(owner);
        this.ownerName = ownerPlayer.getName();
    }

    public void reload() {
        final Config config = Config.config;
        this.mainGUIItemStack = syncItemStack(config.mainGUIConfig.orderConfig.lore.stream().map(this::deserializeText).toList());
        this.yourOrdersGUIItemStack = syncItemStack(config.yourOrdersGUIConfig.orderConfig.lore.stream().map(this::deserializeText).toList());
    }

    private @NotNull ItemStack syncItemStack(final List<Component> lore) {
        final ItemStack result = item.clone();
        result.lore(lore);
        return result;
    }

    public boolean isActive() { return delivered < amount && expiresAt > System.currentTimeMillis(); }

    public @NotNull InventoryItem yourOrdersInventoryItem(final Consumer<InventoryClickEvent> action) {
        return new InventoryItem(yourOrdersGUIItemStack, action);
    }

    public @NotNull InventoryItem mainInventoryItem(final Consumer<InventoryClickEvent> action) {
        return new InventoryItem(mainGUIItemStack, action);
    }

    public @NotNull ItemStack yourOrdersItemStack() {
        return yourOrdersGUIItemStack;
    }

    public @NotNull ItemStack mainGUIItemStack() {
        return mainGUIItemStack;
    }

    public @NotNull Component deserializeText(final @NotNull String text) {
        return Values.minimessage.deserialize(text, placeholders());
    }

    public @NotNull TagResolver[] placeholders() {
        final String playerName = ownerName == null ? owner.toString() : ownerName;
        long millis = expiresAt - System.currentTimeMillis();
        long sec = millis / 1000;
        long min = sec / 60;
        long hour = min / 60;
        final long day = hour / 24;
        hour %= 24;
        min %= 60;
        sec %= 60;
        millis %= 1000;
        final ItemMeta meta = item.getItemMeta();
        final Component itemName = meta.hasCustomName() ? meta.customName() : Component.translatable(item.translationKey());
        assert itemName != null;
        return new TagResolver[]{
                Placeholder.unparsed("money-per", formatNumber(moneyPer)),
                Placeholder.unparsed("paid", formatNumber(moneyPer * delivered)),
                Placeholder.unparsed("total", formatNumber(moneyPer * amount)),
                Placeholder.unparsed("delivered", formatNumber(delivered)),
                Placeholder.unparsed("amount", formatNumber(amount)),
                Placeholder.unparsed("in-storage", formatNumber(inStorage)),
                Placeholder.unparsed("player", playerName),
                Placeholder.component("item", itemName),
                Placeholder.component("order-status", Values.minimessage.deserialize(getStatus().getText(),
                        Placeholder.unparsed("day", String.valueOf(day)),
                        Placeholder.unparsed("hour", String.valueOf(hour)),
                        Placeholder.unparsed("minute", String.valueOf(min)),
                        Placeholder.unparsed("second", String.valueOf(sec)),
                        Placeholder.unparsed("millisecond", String.valueOf(millis))
                ))
        };
    }
    public @NotNull String[] stringPlaceholders() {
        final String playerName = ownerName == null ? owner.toString() : ownerName;
        long millis = expiresAt - System.currentTimeMillis();
        long sec = millis / 1000;
        long min = sec / 60;
        long hour = min / 60;
        final long day = hour / 24;
        hour %= 24;
        min %= 60;
        sec %= 60;
        millis %= 1000;
        final ItemMeta meta = item.getItemMeta();
        final Component customName = item.getItemMeta().customName();
        @SuppressWarnings("deprecation")
        final String itemName = meta.hasCustomName() && customName != null ? PlainTextComponentSerializer.plainText().serialize(customName) : item.getI18NDisplayName();
        assert itemName != null;
        return new String[]{
                "<money-per>", formatNumber(moneyPer),
                "<paid>", formatNumber(moneyPer * delivered),
                "<total>", formatNumber(moneyPer * amount),
                "<delivered>", formatNumber(delivered),
                "<amount>", formatNumber(amount),
                "<in-storage>", formatNumber(inStorage),
                "<player>", playerName,
                "<item>", itemName,
                "<order-status>", getStatus().getText()
                .replaceAll("<day>", String.valueOf(day))
                .replaceAll("<hour>", String.valueOf(hour))
                .replaceAll("<minute>", String.valueOf(min))
                .replaceAll("<second>", String.valueOf(sec))
                .replaceAll("<millisecond>", String.valueOf(millis))
        };
    }

    /// Must be called in the player region
    public void deliver(Player p, Iterable<ItemStack> items, boolean isAsync) {
        PlayerDeliverOrderEvent.Pre preEvent = new PlayerDeliverOrderEvent.Pre(p, this, isAsync);
        if (!preEvent.callEvent()) return;

        plugin.getStorage().deliverOrder(p, this, items).thenAccept(receive -> {
            double moneyReceived = receive; // I don't like working with wrapped class at all so will use primitive
            if (moneyReceived == 0.0) return;
            final Config config = Config.config;
            EconUtils.addMoney(p, moneyReceived);
            p.sendRichMessage(config.deliver, Placeholder.unparsed("money", formatNumber(moneyReceived)));
            PlayerUtils.playSound(p, config.deliverSound);

            if (config.webhookConfig.deliverOrderOption.enabled) {
                config.webhookConfig.deliverOrderOption.send(stringPlaceholders(), "<deliverer>", p.getName());
            }

            final PlayerDeliverOrderEvent.Post postEvent = new PlayerDeliverOrderEvent.Post(p, this, isAsync);

            final Player ownerPlayer = Bukkit.getPlayer(owner);
            if (ownerPlayer == null || !ownerPlayer.isOnline()) {
                reload();
                postEvent.callEvent();
                return;
            }
            final ItemMeta meta = item.getItemMeta();
            final Component displayName = meta == null ? null : meta.displayName();
            assert item.getType().getItemTranslationKey() != null;
            ownerPlayer.sendRichMessage(
                    config.receiveDelivery,
                    Placeholder.unparsed("deliverer", p.getName()),
                    Placeholder.unparsed("amount",  formatNumber((int) (moneyReceived / moneyPer))),
                    Placeholder.component("item", (displayName == null ? Component.translatable(item.getType().getItemTranslationKey()) : displayName))
            );
            postEvent.callEvent();
        });
    }

    /// Must be called in the player region
    public Response collect(String rawAmount) {
        final Player p = Bukkit.getPlayer(getOwnerUniqueId());
        if (p == null || !p.isOnline() || rawAmount == null) return Response.INVALID;
        final double dAmount = formatNumber(rawAmount);
        final int amount = (int) dAmount;
        if (dAmount == -1 || dAmount != amount) {
            p.sendRichMessage(Config.config.invalidInput);
            return Response.INVALID;
        }
        return collect(amount);
    }

    /// Must be called in the player region
    public Response collect(int amount) {
        final Player p = Bukkit.getPlayer(this.getOwnerUniqueId());
        if (p == null || !p.isOnline()) return Response.INVALID;
        final Config config = Config.config;

        if (amount > config.maxCollect && !p.hasPermission("orderium.bypass.max-collect")) {
            p.sendRichMessage(config.exceedMaxCollect);
            return Response.FAIL;
        }

        final int collectedInMinute = PDCUtils.getCollected(p);
        if (collectedInMinute > config.maxCollectPerMinute && !p.hasPermission("orderium.bypass.max-collect-per-minute")) {
            p.sendRichMessage(config.collectingTooFast);
            return Response.FAIL;
        }

        PlayerCollectItemsEvent.Pre preEvent = new PlayerCollectItemsEvent.Pre(p, amount, this, false);
        if (!preEvent.callEvent()) return Response.CANCELLED;

        plugin.getStorage().collectItems(this, amount).thenAccept(success -> {
            boolean succeeded = success;
            if (!succeeded) {
                p.sendRichMessage(config.invalidInput);
                return;
            }

            PDCUtils.setCollected(p, collectedInMinute + amount);

            PlayerUtils.give(p, getItem().clone(), amount, true);

            CustomMetrics.ITEMS_COLLECTED_CACHE.addAndGet(amount);

            if (config.webhookConfig.collectItemsOption.enabled) {
                config.webhookConfig.collectItemsOption.send(stringPlaceholders(), "<collect-amount>", String.valueOf(amount));
            }

            reload();

            PlayerCollectItemsEvent.Post postEvent = new PlayerCollectItemsEvent.Post(p, amount, this, true);
            postEvent.callEvent();
        });

        return Response.SCHEDULED;
    }


    public void cancel(Player p) {
        PlayerCancelOrderEvent.Pre preEvent = new PlayerCancelOrderEvent.Pre(p, this, false);
        if (!preEvent.callEvent()) return;
        YourOrderGUI.open(p, false);

        plugin.getStorage().cancelOrder(this).thenAccept(payBack -> {
            double reward = payBack;

            if (reward == -1.0d) {
                return;
            }
            this.expiresAt = System.currentTimeMillis() - 1;
            YourOrderGUI.open(p, true);
            EconUtils.addMoney(ownerPlayer, reward);
            final Config config = Config.config;
            if (config.webhookConfig.cancelOrderOption.enabled) {
                config.webhookConfig.cancelOrderOption.send(stringPlaceholders(), "<earn>", formatNumber(reward));
            }
            reload();
            PlayerCancelOrderEvent.Post postEvent = new PlayerCancelOrderEvent.Post(p, this, true);
            postEvent.callEvent();
        });
    }

    public OrderStatus getStatus() {
        if (delivered >= amount) return OrderStatus.COMPLETED;
        if (expiresAt < System.currentTimeMillis()) return OrderStatus.EXPIRED;
        return OrderStatus.AVAILABLE;
    }

    public boolean shouldBeDeleted() {
        return !isActive() && inStorage == 0;
    }

    public double getPaid() { return moneyPer * delivered; }

    @Override
    public int getId() {
        return this.id;
    }


    public @Nullable String getOwnerName() {
        return ownerName;
    }

    @Override
    public UUID getOwnerUniqueId() {
        return owner;
    }

    @Override
    public ItemStack getItem() {
        return this.item;
    }

    @Override
    public double getMoneyPer() {
        return this.moneyPer;
    }

    @Override
    public int getAmount() {
        return this.amount;
    }

    @Override
    public int getDelivered() {
        return this.delivered;
    }

    @Override
    public int getInStorage() {
        return this.inStorage;
    }

    @Override
    public long getExpiresAt() {
        return this.expiresAt;
    }

    @Override
    public void setDelivered(int delivered) {
        this.delivered = delivered;
        update("delivered", delivered);
    }

    @Override
    public void setInStorage(int inStorage) {
        this.inStorage = inStorage;
        update("in_storage", inStorage);
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;
        update("amount", amount);
    }

    @Override
    public void setMoneyPer(double moneyPer) {
        this.moneyPer = moneyPer;
        update("money_per", moneyPer);
    }

    private void update(String var, Object value) {
        if (shouldBeDeleted()) {
            plugin.getStorage().deleteOrder(this);
            return;
        }
        plugin.getStorage().updateOrder(this, var, value).thenAccept(_ -> reload());
    }

    /// Must be called in the player region
    public static Response create(Player p, ItemStack item, String rawMoneyPer, String rawAmount) {
        if (rawAmount == null || rawMoneyPer == null) return Response.INVALID;
        final double dAmount = formatNumber(rawAmount);
        final int amount = (int) dAmount;
        final double moneyPer = formatNumber(rawMoneyPer);
        if (dAmount == -1 || moneyPer == -1 || moneyPer < Config.config.minPrice || dAmount != amount) return Response.INVALID;

        return create(p, item, moneyPer, amount);
    }

    /// Must be called in the player region
    public static Response create(Player owner, ItemStack item, double moneyPer, int amount) {
        PlayerCreateOrderEvent.Pre event = new PlayerCreateOrderEvent.Pre(owner, item, moneyPer, amount, false);
        if (!event.callEvent()) return Response.CANCELLED;

        if (!EconUtils.removeMoney(owner, moneyPer * amount)) {
            return Response.FAIL;
        }
        ItemStack strippedItem = item.clone();
        strippedItem.setItemMeta(PDCUtils.removeOrderiumPD(strippedItem.getItemMeta()));
        plugin.getStorage().createOrder(owner.getUniqueId(), strippedItem, amount, moneyPer)
                .thenAccept(order -> {
                    CustomMetrics.ORDER_AMOUNT_CACHE.incrementAndGet();
                    final Config config = Config.config;
                    if (config.broadcastOrderCreation) {
                        final Component message = order.deserializeText(config.orderCreationBroadcast);

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(message);
                        }
                    }

                    if (config.webhookConfig.createOrderOption.enabled) {
                        config.webhookConfig.createOrderOption.send(order.stringPlaceholders());
                    }

                    order.reload();

                    PlayerCreateOrderEvent.Post postEvent = new PlayerCreateOrderEvent.Post(owner, order, true);
                    postEvent.callEvent();
                });
        return Response.SUCCESS;
    }

    public interface Response {
        Response INVALID = new Response() {};
        Response SUCCESS = new Response() {};
        Response FAIL = new Response() {};
        Response CANCELLED = new Response() {};
        Response SCHEDULED = new Response() {};
    }
}
