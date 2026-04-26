package me.karven.orderium.obj;

import lombok.Getter;
import lombok.val;
import me.karven.orderium.api.events.PlayerCancelOrderEvent;
import me.karven.orderium.api.events.PlayerCollectItemsEvent;
import me.karven.orderium.api.events.PlayerCreateOrderEvent;
import me.karven.orderium.api.events.PlayerDeliverOrderEvent;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.gui.YourOrderGUI;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import me.karven.orderium.utils.PDCUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

import static me.karven.orderium.load.Orderium.plugin;

@Getter

// TODO: Replace `item` with OrderItem instead of ItemStack.
// Problem: how to store it in database?
public class Order implements me.karven.orderium.api.Order {
    public final int id;
    public final UUID owner;
    public final ItemStack item;
    public double moneyPer;
    public int amount;
    public int delivered;
    public int inStorage;
    public long expiresAt;

    private static ConfigCache cache;

    public static void init() {
        cache = plugin.getConfigs();
    }

    public Order(int id, UUID owner, ItemStack item, double moneyPer, int amount, int delivered, int inStorage, long expiresAt) {
        this.id = id;
        this.owner = owner;
        this.item = item;
        this.moneyPer = moneyPer;
        this.amount = amount;
        this.delivered = delivered;
        this.inStorage = inStorage;
        this.expiresAt = expiresAt;
    }


    public boolean isActive() { return delivered < amount && expiresAt > System.currentTimeMillis(); }

    /// Must be called in the player region
    public void deliver(Player p, Iterable<ItemStack> items) {
        val event = new PlayerDeliverOrderEvent(p, this);
        event.callEvent();

        plugin.getStorage().deliverOrder(p, this, items).thenAccept(receive -> {
            double moneyReceived = receive; // I don't like working with wrapped class at all so will use primitive
            if (moneyReceived == 0.0) return;
            EconUtils.addMoney(p, moneyReceived);
            p.sendRichMessage(cache.getDelivered(), Placeholder.unparsed("money", ConvertUtils.formatNumber(moneyReceived)));
            PlayerUtils.playSound(p, cache.getDeliverSound());

            final Player ownerPlayer = Bukkit.getPlayer(owner);
            if (ownerPlayer == null || !ownerPlayer.isOnline()) return;
            final ItemMeta meta = item.getItemMeta();
            final Component displayName = meta == null ? null : meta.displayName();
            assert item.getType().getItemTranslationKey() != null;
            ownerPlayer.sendRichMessage(
                    cache.getReceiveDelivery(),
                    Placeholder.unparsed("deliverer", p.getName()),
                    Placeholder.unparsed("amount",  ConvertUtils.formatNumber((int) (moneyReceived / moneyPer))),
                    Placeholder.component("item", (displayName == null ? Component.translatable(item.getType().getItemTranslationKey()) : displayName))
            );
        });
    }

    public Response collect(String rawAmount) {
        final Player p = Bukkit.getPlayer(getOwnerUniqueId());
        if (p == null || !p.isOnline() || rawAmount == null) return Response.INVALID;
        final double dAmount = ConvertUtils.formatNumber(rawAmount);
        final int amount = (int) dAmount;
        if (dAmount == -1 || dAmount != amount) {
            p.sendRichMessage(cache.getInvalidInput());
            return Response.INVALID;
        }
        return collect(amount);
    }

    /// Must be called in the player region
    public Response collect(int amount) {
        final Player p = Bukkit.getPlayer(this.getOwnerUniqueId());
        if (p == null || !p.isOnline()) return Response.INVALID;
        if (amount > cache.getMaxCollect() && !p.hasPermission("orderium.bypass.max-collect")) {
            p.sendRichMessage(cache.getExceedMaxCollect());
            return Response.FAIL;
        }

        final int collectedInMinute = PDCUtils.getCollected(p);
        if (collectedInMinute > cache.getMaxCollectPerMinute() && !p.hasPermission("orderium.bypass.max-collect-per-minute")) {
            p.sendRichMessage(cache.getCollectingTooFast());
            return Response.FAIL;
        }

        val event = new PlayerCollectItemsEvent(p, this, amount);
        if (!event.callEvent()) return Response.FAIL;

        plugin.getStorage().collectItems(this, amount).thenAccept(success -> {
            if (success.equals(Boolean.FALSE)) {
                p.sendRichMessage(cache.getInvalidInput());
                return;
            }

            PDCUtils.setCollected(p, collectedInMinute + amount);

            PlayerUtils.give(p, getItem().clone(), amount, true);
        });

        return Response.SCHEDULED;
    }


    public void cancel(Player p) {
        val event = new PlayerCancelOrderEvent(p, this);
        if (!event.callEvent()) return;

        plugin.getStorage().cancelOrder(this).thenAccept(payBack -> {
            if (payBack.equals(-1.0)) {
                return;
            }
            this.expiresAt = System.currentTimeMillis() - 1;
            YourOrderGUI.open(p);
            EconUtils.addMoney(Bukkit.getOfflinePlayer(getOwnerUniqueId()), payBack);
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
    public UUID getOwnerUniqueId() {
        return owner;
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
        plugin.getStorage().updateOrder(this, var, value);
    }

    public static Response create(Player p, ItemStack item, String rawMoneyPer, String rawAmount) {
        if (rawAmount == null || rawMoneyPer == null) return Response.INVALID;
        final double dAmount = ConvertUtils.formatNumber(rawAmount);
        final int amount = (int) dAmount;
        final double moneyPer = ConvertUtils.formatNumber(rawMoneyPer);
        if (dAmount == -1 || moneyPer == -1 || dAmount != amount) return Response.INVALID;

        return create(p, item, moneyPer, amount);
    }

    public static Response create(Player owner, ItemStack item, double moneyPer, int amount) {
        val event = new PlayerCreateOrderEvent(owner, item, moneyPer, amount);
        if (!event.callEvent()) return Response.CANCELLED;

        if (!EconUtils.removeMoney(owner, moneyPer * amount)) {
            return Response.FAIL;
        }
        ItemStack strippedItem = item.clone();
        strippedItem.setItemMeta(PDCUtils.removeOrderiumPD(strippedItem.getItemMeta()));
        plugin.getStorage().createOrder(owner.getUniqueId(), strippedItem, amount, moneyPer);
        return Response.SUCCESS;
    }

    public enum Response {
        INVALID,
        SUCCESS,
        FAIL,
        CANCELLED,
        SCHEDULED
    }
}
