package me.karven.orderium;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.karven.orderium.config.Config;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.guiframework.GUIListener;
import me.karven.orderium.listener.ContainerContentListener;
import me.karven.orderium.listener.DisconnectListener;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.storage.implementation.SQLStorage;
import me.karven.orderium.utils.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.karven.orderium.config.Config.config;

public final class Orderium extends JavaPlugin {
    public static Orderium plugin;
    public static boolean isFolia;

    private final SignGUI SIGN_LISTENER = new SignGUI();
    private final ContainerContentListener CONTAINER_CONTENT_LISTENER = new ContainerContentListener();
    private final GUIListener GUI_LISTENER = new GUIListener();
    private final DisconnectListener DISCONNECT_LISTENER = new DisconnectListener();

    private Storage storage;
    private Economy econ;
    public final MiniMessage mm = MiniMessage.miniMessage();

    public Storage getStorage() { return storage; }
    public @NotNull DataCache getDataCache() { return DataCache.getInstance(); }
    public Economy getEcon() { return econ; }

    public void setStorage(Storage storage) { this.storage = storage; }

    @Override
    public void onEnable() {
        plugin = this;
        if (!setupEconomy()) {
            Log.warn("Orderium disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            config = new Config();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        OrderiumCommands.register();

        isFolia = isFolia();
        PacketEvents.getAPI().getEventManager().registerListener(SIGN_LISTENER, PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager().registerListener(CONTAINER_CONTENT_LISTENER, PacketListenerPriority.NORMAL);
        
        Bukkit.getPluginManager().registerEvents(GUI_LISTENER, this);
        Bukkit.getPluginManager().registerEvents(DISCONNECT_LISTENER, this);

        UpdateUtils.init();
        Log.info("Orderium enabled");
        storage = createStorage();
        EconUtils.init();
        AdminToolGUI.init(); // need testing

        ChooseItemGUI.init(); // need testing

        if (config.bStats) {
            final int pluginId = 27569;
            new Metrics(this, pluginId);
        }

        if (config.checkForUpdates) {
            Bukkit.getAsyncScheduler().runNow(this, task -> {
               final String newVer = UpdateUtils.checkForUpdates();
               if (newVer == null) return;
               Log.warn("A new version of Orderium (" + newVer + ") is available");
               Log.info(mm.deserialize("<aqua>Download it on <green>Modrinth<gray>: <blue><u>https://modrinth.com/plugin/orderium/version/" + newVer));
            });
        }

        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {

            for (Player p : Bukkit.getOnlinePlayers()) {
                DispatchUtil.entity(p, () -> PDCUtils.removeCollected(p));
            }

        }, 1, 1, TimeUnit.MINUTES);
    }

    public Storage createStorage() {
        return SQLStorage.sqlite();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();

        return true;
    }
    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
