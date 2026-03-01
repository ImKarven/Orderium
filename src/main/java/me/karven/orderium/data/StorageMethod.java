package me.karven.orderium.data;

import me.karven.orderium.load.Orderium;

import java.util.logging.Level;

public enum StorageMethod {
    SQLITE,
    MYSQL;

    public static StorageMethod fromString(String s) {
        try {
            return StorageMethod.valueOf(s);
        } catch (Exception e) {
            Orderium.getInst().getLogger().log(Level.SEVERE, "Invalid storage method '" + s + "', using sqlite", e);
            return SQLITE;
        }
    }
}
