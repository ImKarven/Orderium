package me.karven.orderium.obj;

import lombok.Setter;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.load.Orderium;
import org.bukkit.inventory.ItemStack;

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
    public final long expiresAt;

    public static DBManager db;

    public static void init(Orderium plugin) {
        db = plugin.getDbManager();
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

    public void updateInStorage(int val) { inStorage = val; }

    public double deliver(int a) {
        return db.deliverOrder(this, a);
    }
}
