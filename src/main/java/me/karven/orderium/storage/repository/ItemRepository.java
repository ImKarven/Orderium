package me.karven.orderium.storage.repository;

import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;

import java.util.List;

public interface ItemRepository {

    List<CustomItem> findCustomItems();

    void addCustomItem(CustomItem item);

    void removeCustomItem(CustomItem item);

    void updateCustomItemSearches(CustomItem item);

    List<BlacklistedItem> findBlacklist();

    void addBlacklisted(BlacklistedItem item);

    void removeBlacklisted(BlacklistedItem item);
}
