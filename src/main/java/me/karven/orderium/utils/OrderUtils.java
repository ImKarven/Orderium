package me.karven.orderium.utils;

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

    public static void deliver(Order order, Player p, Collection<ItemStack> items) {
        final int maxDeliverAmount = order.amount() - order.delivered();
        int currAmount = 0;
        boolean done = false;
        for (ItemStack item : items) {
            if (done) {
                PlayerUtils.give(p, item);
                continue;
            }

            if (!AlgoUtils.isSimilar(item, order.item())) {
                PlayerUtils.give(p, item);
                continue;
            }

            if (currAmount + item.getAmount() <= maxDeliverAmount) {
                currAmount += item.getAmount();
                continue;
            }

            // Split the item when it exceeds the max the player can deliver
            ItemStack toGive = item.clone();
            toGive.setAmount(item.getAmount() - (maxDeliverAmount - currAmount));
            PlayerUtils.give(p, toGive);
            currAmount = maxDeliverAmount;
            done = true;
        }
        order.deliver(p, currAmount);
    }

    public static Response collect(Order order, String rawAmount) {
        final Player p = Bukkit.getPlayer(order.owner());
        if (p == null || !p.isOnline() || rawAmount == null) return Response.INVALID;
        final double dAmount = ConvertUtils.formatNumber(rawAmount);
        final int amount = (int) dAmount;
        if (dAmount == -1 || dAmount != amount) {
            p.sendRichMessage(cache.getInvalidInput());
            return Response.INVALID;
        }
        return collect(order, amount);
    }

    /// Must be called in an entity scheduler
    public static Response collect(Order order, int amount) {
        final Player p = Bukkit.getPlayer(order.owner());
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

        if (order.inStorage() < amount) {
            p.sendRichMessage(cache.getInvalidInput());
            return Response.INVALID;
        }

        PDCUtils.setCollected(p, collectedInMinute + amount);

        order.setInStorage(order.inStorage() - amount);
        PlayerUtils.give(p, order.item().clone(), amount);
        return Response.SUCCESS;
    }

    public static void cancel(Order order) {
        EconUtils.addMoney(Bukkit.getOfflinePlayer(order.owner()), order.cancel());
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
        db.createOrder(owner.getUniqueId(), item, moneyPer, amount);
        return Response.SUCCESS;
    }


    public enum Response {
        INVALID,
        SUCCESS,
        FAIL
    }
}
