package me.karven.orderium.obj;

import lombok.Setter;

import java.util.UUID;

@Setter
public class MoneyTransaction {
    public UUID player;
    public double before;
    public double amount;
    public double after;
}
