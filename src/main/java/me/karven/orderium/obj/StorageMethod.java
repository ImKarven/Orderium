package me.karven.orderium.obj;

import lombok.Getter;

@Getter
public enum StorageMethod {
    SQLITE,
    MYSQL,
    H2
//
//    private String createOrderTableStatement;
//    private String createTransactionTableStatement;
//    private String logTransactionStatement;
//    private String orderInfoStatement;
//
//    public static StorageMethod fromString(String s) {
//        try {
//            return StorageMethod.valueOf(s.toUpperCase());
//        } catch (Exception e) {
//            Log.error("Invalid storage method '" + s + "', using h2", e);
//            return H2;
//        }
//    }
//    private static ConfigCache configs;
//    public static void init(Orderium orderium) {
//        configs = orderium.getConfigs();
//
//        SQLITE.createOrderTableStatement = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE() + " (id INTEGER PRIMARY KEY, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
//        SQLITE.createTransactionTableStatement = "CREATE TABLE IF NOT EXISTS " + TRANSACTION_TABLE() + " (time BIGINT PRIMARY KEY, player_most BIGINT, player_least BIGINT, before DOUBLE, amount DOUBLE, after DOUBLE)";
//        SQLITE.logTransactionStatement = "INSERT INTO " + TRANSACTION_TABLE() + " (time, player_most, player_least, before, amount, after) VALUES (?, ?, ?, ?, ?, ?)";
//        SQLITE.orderInfoStatement = "SELECT delivered, amount, in_storage FROM " + ORDER_TABLE() + " WHERE id = ?";
//
//        MYSQL.createOrderTableStatement = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE() + " (id INTEGER AUTO_INCREMENT PRIMARY KEY, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
//        MYSQL.createTransactionTableStatement = "CREATE TABLE IF NOT EXISTS " + TRANSACTION_TABLE() + " (time BIGINT PRIMARY KEY, player_most BIGINT, player_least BIGINT, `before` DOUBLE, amount DOUBLE, `after` DOUBLE)";
//        MYSQL.logTransactionStatement = "INSERT INTO " + TRANSACTION_TABLE() + " (time, player_most, player_least, `before`, amount, `after`) VALUES (?, ?, ?, ?, ?, ?)";
//        MYSQL.orderInfoStatement = "SELECT delivered, amount, in_storage FROM " + ORDER_TABLE() + " WHERE id = ? FOR UPDATE";
//    }
//    private static String ORDER_TABLE() { return configs.getTablePref() + "orders"; }
//    private static String TRANSACTION_TABLE() { return configs.getTablePref() + "transactions"; }


}
