package me.karven.orderium.order;

import me.karven.orderium.api.events.PlayerCancelOrderEvent;
import me.karven.orderium.api.events.PlayerCollectItemsEvent;
import me.karven.orderium.api.events.PlayerCreateOrderEvent;
import me.karven.orderium.config.Config;
import me.karven.orderium.gui.YourOrderGUI;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.obj.OrderStatus;
import me.karven.orderium.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Consumer;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.utils.ConvertUtils.formatNumber;

public abstract class Order implements me.karven.orderium.api.Order {
    private final Object lock = new Object();
    private final int id;
    private final UUID owner;
    private final @NotNull OfflinePlayer ownerPlayer;
    private final @Nullable String ownerName;
    private volatile OrderState state;
    private volatile boolean removed = false;

    private ItemStack mainGUIItemStack;
    private ItemStack yourOrdersGUIItemStack;
    private boolean hasOrderStatusInMainGUIOrderConfigLore = true;
    private boolean hasOrderStatusInYourOrdersGUIOrderConfigLore = true;

    protected Order(int id, UUID owner, OrderState state) {
        this.id = id;
        this.owner = owner;
        this.state = state;
        this.ownerPlayer = Bukkit.getOfflinePlayer(owner);
        this.ownerName = ownerPlayer.getName();
    }

    public abstract OrderType<?> getType();

    public abstract ItemStack getIcon();

    protected abstract Component displayName();

    protected abstract String plainDisplayName();

    public abstract boolean matchesSearch(String query);

    // call this on the players region thread
    public abstract void openDelivery(Player player);

    protected abstract void handOut(Player owner, int amount);

    private void checkOrderStatusExistenceInOrderConfigsLore(final Config config) {
        for (final String line : config.mainGUIConfig.orderConfig.lore) {
            if (line.contains("<order-status>")) {
                hasOrderStatusInMainGUIOrderConfigLore = true;
                break;
            }
        }

        for (final String line : config.yourOrdersGUIConfig.orderConfig.lore) {
            if (line.contains("<order-status>")) {
                hasOrderStatusInYourOrdersGUIOrderConfigLore = true;
                break;
            }
        }
    }

    public void reload() {
        final Config config = Config.config;
        checkOrderStatusExistenceInOrderConfigsLore(config);
        reloadMainGUIItemStack(config);
        reloadYourOrdersGUIItemStack(config);
    }

    private void reloadMainGUIItemStack(final Config config) {
        this.mainGUIItemStack = syncItemStack(config.mainGUIConfig.orderConfig.lore.stream().map(this::deserializeText).toList());
    }

    private void reloadYourOrdersGUIItemStack(final Config config) {
        this.yourOrdersGUIItemStack = syncItemStack(config.yourOrdersGUIConfig.orderConfig.lore.stream().map(this::deserializeText).toList());
    }

    private @NotNull ItemStack syncItemStack(final List<Component> lore) {
        final ItemStack result = getIcon();
        result.lore(lore);
        return result;
    }

    public boolean isActive() { return state.isActive(System.currentTimeMillis()); }

    public @NotNull InventoryItem yourOrdersInventoryItem(final Consumer<InventoryClickEvent> action) {
        return new InventoryItem(yourOrdersItemStack(), action);
    }

    public @NotNull InventoryItem mainInventoryItem(final Consumer<InventoryClickEvent> action) {
        return new InventoryItem(mainGUIItemStack(), action);
    }

    public @NotNull ItemStack yourOrdersItemStack() {
        if (hasOrderStatusInYourOrdersGUIOrderConfigLore)
            reloadYourOrdersGUIItemStack(Config.config);
        return yourOrdersGUIItemStack;
    }

    public @NotNull ItemStack mainGUIItemStack() {
        if (hasOrderStatusInMainGUIOrderConfigLore)
            reloadMainGUIItemStack(Config.config);
        return mainGUIItemStack;
    }

    public @NotNull Component deserializeText(final @NotNull String text) {
        return Values.minimessage.deserialize(text, placeholders());
    }

    public @NotNull TagResolver[] placeholders() {
        final OrderState state = this.state;
        final String playerName = ownerName == null ? owner.toString() : ownerName;
        long millis = state.expiresAt() - System.currentTimeMillis();
        final Duration duration = Duration.ofMillis(millis);
        return new TagResolver[]{
                Placeholder.unparsed("money-per", formatNumber(state.moneyPer())),
                Placeholder.unparsed("paid", formatNumber(state.moneyPer() * state.delivered())),
                Placeholder.unparsed("total", formatNumber(state.moneyPer() * state.amount())),
                Placeholder.unparsed("delivered", formatNumber(state.delivered())),
                Placeholder.unparsed("amount", formatNumber(state.amount())),
                Placeholder.unparsed("in-storage", formatNumber(state.inStorage())),
                Placeholder.unparsed("player", playerName),
                Placeholder.component("item", displayName()),
                Placeholder.component("order-status", Values.minimessage.deserialize(getStatus().getText(),
                        Placeholder.unparsed("day", String.valueOf(duration.toDays())),
                        Placeholder.unparsed("hour", String.valueOf(duration.toHoursPart())),
                        Placeholder.unparsed("minute", String.valueOf(duration.toMinutesPart())),
                        Placeholder.unparsed("second", String.valueOf(duration.toSecondsPart())),
                        Placeholder.unparsed("millisecond", String.valueOf(duration.toMillisPart()))
                ))
        };
    }

    public @NotNull String[] stringPlaceholders() {
        final OrderState state = this.state;
        final String playerName = ownerName == null ? owner.toString() : ownerName;
        long millis = state.expiresAt() - System.currentTimeMillis();
        final Duration duration = Duration.ofMillis(millis);
        return new String[]{
                "<money-per>", formatNumber(state.moneyPer()),
                "<paid>", formatNumber(state.moneyPer() * state.delivered()),
                "<total>", formatNumber(state.moneyPer() * state.amount()),
                "<delivered>", formatNumber(state.delivered()),
                "<amount>", formatNumber(state.amount()),
                "<in-storage>", formatNumber(state.inStorage()),
                "<player>", playerName,
                "<item>", plainDisplayName(),
                "<order-status>", getStatus().getText()
                .replaceAll("<day>", String.valueOf(duration.toDays()))
                .replaceAll("<hour>", String.valueOf(duration.toHoursPart()))
                .replaceAll("<minute>", String.valueOf(duration.toMinutesPart()))
                .replaceAll("<second>", String.valueOf(duration.toSecondsPart()))
                .replaceAll("<millisecond>", String.valueOf(duration.toMillisPart()))
        };
    }

    // Must be called in the player region
    public Response collect(String rawAmount) {
        final Player p = Bukkit.getPlayer(getOwnerUniqueId());
        if (p == null || !p.isOnline() || rawAmount == null) return Response.INVALID;
        final double dAmount = formatNumber(rawAmount);
        final int amount = (int) dAmount;
        if (dAmount <= 0 || dAmount != amount) {
            p.sendRichMessage(Config.config.invalidInput);
            return Response.INVALID;
        }
        return collect(amount);
    }

    // Must be called in the player region
    public Response collect(int amount) {
        final Player p = Bukkit.getPlayer(this.getOwnerUniqueId());
        if (p == null || !p.isOnline()) return Response.INVALID;
        final Config config = Config.config;

        if (amount <= 0) {
            p.sendRichMessage(config.invalidInput);
            return Response.INVALID;
        }

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

        plugin.getOrderService().collect(this, amount)
                .exceptionally(exception -> false)
                .thenAccept(succeeded -> {
                    if (!succeeded) {
                        p.sendRichMessage(config.invalidInput);
                        return;
                    }

                    PDCUtils.setCollected(p, collectedInMinute + amount);

                    handOut(p, amount);

                    CustomMetrics.ITEMS_COLLECTED_CACHE.addAndGet(amount);

                    if (config.webhookConfig.collectItemsOption.enabled) {
                        config.webhookConfig.collectItemsOption.send(stringPlaceholders(), "<collect-amount>", String.valueOf(amount));
                    }

                    reload();

                    PlayerCollectItemsEvent.Post postEvent = new PlayerCollectItemsEvent.Post(p, amount, this, !Bukkit.isPrimaryThread());
                    postEvent.callEvent();
                });

        return Response.SCHEDULED;
    }

    public void cancel(Player p) {
        PlayerCancelOrderEvent.Pre preEvent = new PlayerCancelOrderEvent.Pre(p, this, false);
        if (!preEvent.callEvent()) return;
        YourOrderGUI.open(p, false);

        plugin.getOrderService().cancel(this)
                .exceptionally(exception -> OptionalDouble.empty())
                .thenAccept(refund -> {
                    if (refund.isEmpty()) return;
                    final double reward = refund.getAsDouble();

                    YourOrderGUI.open(p, true);
                    EconUtils.addMoney(ownerPlayer, reward);
                    final Config config = Config.config;
                    if (config.webhookConfig.cancelOrderOption.enabled) {
                        config.webhookConfig.cancelOrderOption.send(stringPlaceholders(), "<earn>", formatNumber(reward));
                    }
                    reload();
                    PlayerCancelOrderEvent.Post postEvent = new PlayerCancelOrderEvent.Post(p, this, !Bukkit.isPrimaryThread());
                    postEvent.callEvent();
                });
    }

    // Must be called in the player region
    protected static Response submit(final Player owner, final OrderDraft<?> draft, final ItemStack eventItem) {
        final OrderState state = draft.state();
        PlayerCreateOrderEvent.Pre event = new PlayerCreateOrderEvent.Pre(owner, eventItem, state.moneyPer(), state.amount(), false);
        if (!event.callEvent()) return Response.CANCELLED;

        final double cost = state.moneyPer() * state.amount();
        if (!EconUtils.removeMoney(owner, cost)) {
            return Response.FAIL;
        }
        plugin.getOrderService().create(draft)
                .whenComplete((order, exception) -> {
                    if (exception != null) {
                        EconUtils.addMoney(owner, cost);
                        return;
                    }

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

                    PlayerCreateOrderEvent.Post postEvent = new PlayerCreateOrderEvent.Post(owner, order, !Bukkit.isPrimaryThread());
                    postEvent.callEvent();
                });
        return Response.SUCCESS;
    }

    public OrderStatus getStatus() {
        final OrderState state = this.state;
        if (state.delivered() >= state.amount()) return OrderStatus.COMPLETED;
        if (state.expiresAt() < System.currentTimeMillis()) return OrderStatus.EXPIRED;
        return OrderStatus.AVAILABLE;
    }

    public boolean shouldBeDeleted() {
        return state.isRemovable(System.currentTimeMillis());
    }

    public double getPaid() {
        final OrderState state = this.state;
        return state.moneyPer() * state.delivered();
    }

    public OrderState getState() {
        return state;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public @Nullable String getOwnerName() {
        return ownerName;
    }

    public OfflinePlayer getOwner() {
        return ownerPlayer;
    }

    @Override
    public UUID getOwnerUniqueId() {
        return owner;
    }

    @Override
    @Deprecated(forRemoval = true)
    public ItemStack getItem() {
        return getIcon();
    }

    @Override
    public double getMoneyPer() {
        return state.moneyPer();
    }

    @Override
    public int getAmount() {
        return state.amount();
    }

    @Override
    public int getDelivered() {
        return state.delivered();
    }

    @Override
    public int getInStorage() {
        return state.inStorage();
    }

    @Override
    public long getExpiresAt() {
        return state.expiresAt();
    }

    @Override
    public void setDelivered(int delivered) {
        edit(Field.DELIVERED, delivered);
    }

    @Override
    public void setInStorage(int inStorage) {
        edit(Field.IN_STORAGE, inStorage);
    }

    @Override
    public void setAmount(int amount) {
        edit(Field.AMOUNT, amount);
    }

    @Override
    public void setMoneyPer(double moneyPer) {
        edit(Field.MONEY_PER, moneyPer);
    }

    private void edit(final Field field, final Number value) {
        plugin.getOrderService().edit(this, field, value)
                .exceptionally(exception -> false)
                .thenAccept(updated -> {
                    if (updated) reload();
                });
    }


    // Only OrderCache changes these while holding the lock, so the sorted indexes stay consistent
    Object getLock() {
        return lock;
    }

    void setState(final OrderState state) {
        this.state = state;
    }

    boolean isRemoved() {
        return removed;
    }

    void markRemoved() {
        this.removed = true;
    }

    public interface Response {
        Response INVALID = new Response() {};
        Response SUCCESS = new Response() {};
        Response FAIL = new Response() {};
        Response CANCELLED = new Response() {};
        Response SCHEDULED = new Response() {};
    }

    public enum Field {
        DELIVERED,
        IN_STORAGE,
        AMOUNT,
        MONEY_PER
    }
}
