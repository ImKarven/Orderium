package me.karven.orderium.listener;

import me.karven.orderium.utils.Log;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.NotNull;

import static me.karven.orderium.Orderium.plugin;

public class ServerLoadListener implements Listener {

    // This event is fired when all plugins are enabled
    // So we can safely check if economy is hooked
    // Economy plugin can be enabled after ours, so it's not suitable to check economy on enable.
    @EventHandler
    public void onPostWorldInitialization(final @NotNull ServerLoadEvent event) {
        if (event.getType().equals(ServerLoadEvent.LoadType.RELOAD)) {
            Log.warn("Bukkit reloading is NOT supported. Expect bugs and errors.");
            return;
        }
        plugin.setupEconomy();

        plugin.postEconomyRegistration();
    }
}
