package me.karven.orderium.api;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface Order {
    /**
     * @return the ID of this order
     */
    int getId();

    /**
     * @return the UUID of the player who created this order
     */
    UUID getOwnerUniqueId();

    /**
     * @return the item the owner chose to order
     */
    ItemStack getItem();

    /**
     * @return money per item of this order
     */
    double getMoneyPer();

    /**
     * @return amount of items the owner set to order
     */
    int getAmount();

    /**
     * @return the amount of items other players delivered to this order
     */
    int getDelivered();

    /**
     * @return the amount of items left in storage
     */
    int getInStorage();

    /**
     * @return the time this order will be expired in millisecond
     */
    long getExpiresAt();

    /**
     * Set the amount of delivered items
     * @param delivered amount to set
     */
    void setDelivered(int delivered);

    /**
     * Set the amount of items in storage
     * @param inStorage amount to set
     */
    void setInStorage(int inStorage);

    /**
     * Set the amount of items needed
     * @param amount amount to set
     */
    void setAmount(int amount);

    /**
     * Set the amount of money per item
     * @param moneyPer amount to set
     */
    void setMoneyPer(double moneyPer);
}
