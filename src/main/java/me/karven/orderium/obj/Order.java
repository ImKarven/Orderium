package me.karven.orderium.obj;

import lombok.Getter;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Getter
public class Order implements me.karven.orderium.api.Order {
    public final int id;
    public final UUID owner;
    public final ItemStack item;
    public double moneyPer;
    public int amount;
    public int delivered;
    public int inStorage;
    public long expiresAt;

    private static DBManager db;
    private static ConfigManager cache;

    public static void init(Orderium plugin) {
        db = plugin.getDbManager();
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

    public CompletableFuture<Integer> deliver(Player deliverer, int a) {
        final Function<Integer, Integer> func = exceeded -> {
            final double earning = moneyPer * (a - exceeded);
            EconUtils.addMoney(deliverer, earning);
            deliverer.sendRichMessage(cache.getDelivered(), Placeholder.unparsed("money", ConvertUtils.formatNumber(earning)));
            PlayerUtils.playSound(deliverer, cache.getDeliverSound());

            final Player ownerPlayer = Bukkit.getPlayer(owner);
            if (ownerPlayer == null || !ownerPlayer.isOnline()) return exceeded;
            final ItemMeta meta = item.getItemMeta();
            final Component displayName = meta == null ? null : meta.displayName();
            assert item.getType().getItemTranslationKey() != null;
            ownerPlayer.sendRichMessage(
                    cache.getReceiveDelivery(),
                    Placeholder.unparsed("deliverer", deliverer.getName()),
                    Placeholder.unparsed("amount",  ConvertUtils.formatNumber(a)),
                    Placeholder.component("item", (displayName == null ? Component.translatable(item.getType().getItemTranslationKey()) : displayName))
            );
            return exceeded;
        };
        return db.deliverOrder(this, a).thenApply(func);
    }

    /// Returns the amount of money the player should get in return
    public double cancel() {
        this.expiresAt = System.currentTimeMillis() - 1;
        return (amount - delivered) * moneyPer;
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
        db.updateOrder(this, var, value);
    }
}
