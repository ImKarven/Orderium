package me.karven.orderium.storage.sql;

import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;
import me.karven.orderium.storage.repository.ItemRepository;
import me.karven.orderium.utils.Log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public abstract class SqlItemRepository extends SqlRepository implements ItemRepository {
    protected static final String CUSTOM_ITEMS_TABLE = "orderium_custom_items_v2";
    protected static final String BLACKLIST_TABLE = "orderium_blacklist";

    protected SqlItemRepository(final SqlDatabase database) {
        super(database);
    }

    @Override
    public final String schemaName() {
        return "items";
    }

    @Override
    public List<CustomItem> findCustomItems() {
        return database.withConnection(connection -> {
            try (
                    final PreparedStatement statement = connection.prepareStatement("SELECT item, search FROM " + CUSTOM_ITEMS_TABLE);
                    final ResultSet result = statement.executeQuery()
            ) {
                final List<CustomItem> items = new ArrayList<>();
                while (result.next()) {
                    final String search = result.getString("search");
                    try {
                        items.add(new CustomItem(result.getBytes("item"), search == null ? new String[0] : search.split(",")));
                    } catch (RuntimeException e) {
                        Log.error("Skipping a custom item, its data could not be read", e);
                    }
                }
                return items;
            }
        });
    }

    @Override
    public void addCustomItem(final CustomItem item) {
        database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement("INSERT INTO " + CUSTOM_ITEMS_TABLE + " (item, search) VALUES (?, ?)")) {
                statement.setBytes(1, item.getItemAsBytes());
                statement.setString(2, item.getParsedSearches());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public void removeCustomItem(final CustomItem item) {
        database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement("DELETE FROM " + CUSTOM_ITEMS_TABLE + " WHERE item = ?")) {
                statement.setBytes(1, item.getItemAsBytes());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public void updateCustomItemSearches(final CustomItem item) {
        database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE " + CUSTOM_ITEMS_TABLE + " SET search = ? WHERE item = ?")) {
                statement.setString(1, item.getParsedSearches());
                statement.setBytes(2, item.getItemAsBytes());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public List<BlacklistedItem> findBlacklist() {
        return database.withConnection(connection -> {
            try (
                    final PreparedStatement statement = connection.prepareStatement("SELECT item FROM " + BLACKLIST_TABLE);
                    final ResultSet result = statement.executeQuery()
            ) {
                final List<BlacklistedItem> items = new ArrayList<>();
                while (result.next()) {
                    try {
                        items.add(new BlacklistedItem(result.getBytes("item")));
                    } catch (RuntimeException e) {
                        Log.error("Skipping a blacklisted item, its data could not be read", e);
                    }
                }
                return items;
            }
        });
    }

    @Override
    public void addBlacklisted(final BlacklistedItem item) {
        database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement("INSERT INTO " + BLACKLIST_TABLE + " (item) VALUES (?)")) {
                statement.setBytes(1, item.getItemAsBytes());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public void removeBlacklisted(final BlacklistedItem item) {
        database.withConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement("DELETE FROM " + BLACKLIST_TABLE + " WHERE item = ?")) {
                statement.setBytes(1, item.getItemAsBytes());
                return statement.executeUpdate();
            }
        });
    }
}
