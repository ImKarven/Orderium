package me.karven.orderium.utils;

import lombok.val;
import me.karven.orderium.api.events.PlayerCancelOrderEvent;
import me.karven.orderium.api.events.PlayerCollectItemsEvent;
import me.karven.orderium.api.events.PlayerCreateOrderEvent;
import me.karven.orderium.api.events.PlayerDeliverOrderEvent;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.Order;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class OrderUtils {
    private static DBManager db;
    private static ConfigManager cache;


    public static void init(Orderium plugin) {
        db = plugin.getDbManager();
        cache = plugin.getConfigs();
    }

    /// Must be called in the player's scheduler
    public static void deliver(Order order, Player p, Collection<ItemStack> items) {
        final int maxDeliverAmount = order.getAmount() - order.getDelivered();
        int currAmount = 0;
        boolean done = false;
        for (ItemStack item : items) {
            if (done) {
                PlayerUtils.give(p, item, false);
                continue;
            }

            if (!AlgoUtils.isSimilar(item, order.getItem())) {
                PlayerUtils.give(p, item, false);
                continue;
            }

            if (currAmount + item.getAmount() <= maxDeliverAmount) {
                currAmount += item.getAmount();
                continue;
            }

            // Split the item when it exceeds the max the player can deliver
            ItemStack toGive = item.clone();
            toGive.setAmount(item.getAmount() - (maxDeliverAmount - currAmount));
            PlayerUtils.give(p, toGive, false);
            currAmount = maxDeliverAmount;
            done = true;
        }
        if (currAmount == 0) return;

        val event = new PlayerDeliverOrderEvent(p, order, currAmount);
        if (!event.callEvent()) return;

        order.deliver(p, currAmount);
    }

    public static Response collect(Order order, String rawAmount) {
        final Player p = Bukkit.getPlayer(order.getOwnerUniqueId());
        if (p == null || !p.isOnline() || rawAmount == null) return Response.INVALID;
        final double dAmount = ConvertUtils.formatNumber(rawAmount);
        final int amount = (int) dAmount;
        if (dAmount == -1 || dAmount != amount) {
            p.sendRichMessage(cache.getInvalidInput());
            return Response.INVALID;
        }
        return collect(order, amount);
    }

    /// Must be called in the player's scheduler
    public static Response collect(Order order, int amount) {
        final Player p = Bukkit.getPlayer(order.getOwnerUniqueId());
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

        if (order.getInStorage() < amount) {
            p.sendRichMessage(cache.getInvalidInput());
            return Response.INVALID;
        }

        val event = new PlayerCollectItemsEvent(p, order, amount);
        if (!event.callEvent()) return Response.CANCELLED;

        PDCUtils.setCollected(p, collectedInMinute + amount);
        order.setInStorage(order.getInStorage() - amount);
        PlayerUtils.give(p, order.getItem().clone(), amount, false);

        return Response.SUCCESS;
    }

    public static void cancel(Player p, Order order) {
        val event = new PlayerCancelOrderEvent(p, order);
        if (!event.callEvent()) return;

        cancel(order);
    }

    /// Notes: This will not fire PlayerCancelOrderEvent. Use cancel(Player, Order) instead
    public static void cancel(Order order) {
        EconUtils.addMoney(Bukkit.getOfflinePlayer(order.getOwnerUniqueId()), order.cancel());
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
        if (!EconUtils.removeMoney(owner, moneyPer * amount)) {
            return Response.FAIL;
        }

        val event = new PlayerCreateOrderEvent(owner, item, moneyPer, amount);
        if (!event.callEvent()) return Response.CANCELLED;

        db.createOrder(owner.getUniqueId(), item, moneyPer, amount);
        return Response.SUCCESS;
    }


    public enum Response {
        INVALID,
        SUCCESS,
        FAIL,
        CANCELLED
    }
}
