package me.karven.orderium.obj;

import lombok.Setter;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

@Setter
public class Order {
    public final int id;
    public final UUID owner;
    public final ItemStack item;
    public final double moneyPer;
    public final int amount;
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

    // Getters
    public int id() { return id; }
    public UUID owner() { return owner; }
    public ItemStack item() { return item; }
    public double moneyPer() { return moneyPer; }
    public int amount() { return amount; }
    public int delivered() { return delivered; }
    public int inStorage() { return inStorage; }
    public long expiresAt() { return expiresAt; }
    public double paid() { return moneyPer * delivered; }


    public boolean isActive() { return delivered < amount && expiresAt > System.currentTimeMillis(); }

    public void deliver(Player deliverer, int a) {
        db.deliverOrder(this, a);
        final double earning = moneyPer * a;
        EconUtils.addMoney(deliverer, earning);
        deliverer.sendRichMessage(cache.getDelivered(), Placeholder.unparsed("money", ConvertUtils.formatNumber(earning)));
        final Player ownerPlayer = Bukkit.getPlayer(owner);
        if (ownerPlayer == null || !ownerPlayer.isOnline()) return;
        final ItemMeta meta = item.getItemMeta();
        final Component displayName = meta == null ? null : meta.displayName();
        assert item.getType().getItemTranslationKey() != null;
        ownerPlayer.sendRichMessage(
                cache.getReceiveDelivery(),
                Placeholder.unparsed("deliverer", deliverer.getName()),
                Placeholder.unparsed("amount",  ConvertUtils.formatNumber(a)),
                Placeholder.component("item", (displayName == null ? Component.translatable(item.getType().getItemTranslationKey()) : displayName))
                );
    }

    public void cancel() {
        this.expiresAt = System.currentTimeMillis() - 1;
    }

    public OrderStatus getStatus() {
        if (delivered >= amount) return OrderStatus.COMPLETED;
        if (expiresAt < System.currentTimeMillis()) return OrderStatus.EXPIRED;
        return OrderStatus.AVAILABLE;
    }

    public boolean shouldBeDeleted() {
        return !isActive() && inStorage == 0;
    }
}
