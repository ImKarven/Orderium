package me.karven.orderium.obj;

import org.bukkit.event.inventory.InventoryClickEvent;

public record ItemClickContext <T> (T object, InventoryClickEvent event) {
}
