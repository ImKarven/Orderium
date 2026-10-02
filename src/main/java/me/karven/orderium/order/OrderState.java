package me.karven.orderium.order;


// Every persisted change increments version, and a change is only persisted if the stored version is still the one it was based on
public record OrderState(double moneyPer, int amount, int delivered, int inStorage, long expiresAt, int version) {

    public static OrderState initial(double moneyPer, int amount, long expiresAt) {
        return new OrderState(moneyPer, amount, 0, 0, expiresAt, 0);
    }

    public boolean isActive(long now) {
        return delivered < amount && expiresAt > now;
    }

    public boolean isRemovable(long now) {
        return !isActive(now) && inStorage == 0;
    }

    public int remaining() {
        return Math.max(0, amount - delivered);
    }

    public OrderState withMoneyPer(double moneyPer) {
        return new OrderState(moneyPer, amount, delivered, inStorage, expiresAt, version);
    }

    public OrderState withAmount(int amount) {
        return new OrderState(moneyPer, amount, delivered, inStorage, expiresAt, version);
    }

    public OrderState withDelivered(int delivered) {
        return new OrderState(moneyPer, amount, delivered, inStorage, expiresAt, version);
    }

    public OrderState withInStorage(int inStorage) {
        return new OrderState(moneyPer, amount, delivered, inStorage, expiresAt, version);
    }

    public OrderState withExpiresAt(long expiresAt) {
        return new OrderState(moneyPer, amount, delivered, inStorage, expiresAt, version);
    }

    public OrderState withVersion(int version) {
        return new OrderState(moneyPer, amount, delivered, inStorage, expiresAt, version);
    }
}
