package me.karven.orderium.storage;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.Title;

import java.io.File;
import java.util.List;
import java.util.Locale;

// A separate file from config.yml because the storage is opened before config.yml is loaded
public record StorageSettings(StorageType type, MySql mysql, Pool pool) {

    public record MySql(String host, int port, String database, String username,
                        String password, List<String> properties) {}

    public record Pool(int maximumPoolSize, int minimumIdle, long maxLifetime, long connectionTimeout) {}

    public static StorageSettings load(final File dataFolder) throws Exception {
        final ConfigFile file = ConfigFile.loadConfig(new File(dataFolder, "storage.yml"));
        file.setTitle(new Title().withWidth(80)
                .addSolidLine()
                .addLine("Orderium storage settings")
                .addLine("Changes only apply after a server restart")
                .addSolidLine());

        file.addDefault("type", StorageType.SQLITE.name(),
                """
                Where Orderium stores its data:
                SQLITE: a local file (data.db in this folder), nothing to set up
                MYSQL: a MySQL or compatible server, required to share orders between servers
                Existing data is not moved when this is changed
                """
        );

        file.addComment("mysql", "Only used when type is MYSQL");
        file.addDefault("mysql.host", "localhost");
        file.addDefault("mysql.port", 3306);
        file.addDefault("mysql.database", "orderium");
        file.addDefault("mysql.username", "root");
        file.addDefault("mysql.password", "");
        file.addDefault("mysql.properties", List.of(),
                """
                Extra JDBC connection properties as key=value, for example:
                properties:
                  - useSSL=false
                  - allowPublicKeyRetrieval=true
                """
        );

        file.addComment("pool", "Database connection pool, only used when type is MYSQL. SQLite always uses a single connection");
        file.addDefault("pool.maximum-pool-size", 10, "The maximum amount of connections");
        file.addDefault("pool.minimum-idle", 10, "The amount of connections kept open while idle");
        file.addDefault("pool.max-lifetime", 1800000L, "How long a connection is kept at most, in milliseconds. Keep it below the wait_timeout of the server");
        file.addDefault("pool.connection-timeout", 5000L, "How long to wait for a connection before failing, in milliseconds");

        file.save();

        final String rawType = file.getString("type", StorageType.SQLITE.name());
        final StorageType type;
        try {
            type = StorageType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storage type '" + rawType + "' in storage.yml, use SQLITE or MYSQL", e);
        }

        final MySql mysql = new MySql(
                file.getString("mysql.host", "localhost"),
                file.getInteger("mysql.port", 3306),
                file.getString("mysql.database", "orderium"),
                file.getString("mysql.username", "root"),
                file.getString("mysql.password", ""),
                List.copyOf(file.getStringList("mysql.properties"))
        );

        final Pool pool = new Pool(
                file.getInteger("pool.maximum-pool-size", 10),
                file.getInteger("pool.minimum-idle", 10),
                file.getLong("pool.max-lifetime", 1800000L),
                file.getLong("pool.connection-timeout", 5000L)
        );

        return new StorageSettings(type, mysql, pool);
    }
}
