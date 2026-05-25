package me.karven.orderium.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.guiframework.GUIListener;
import me.karven.orderium.listener.ContainerContentListener;
import me.karven.orderium.listener.DialogListener;
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

import static me.karven.orderium.data.ConfigCache.cache;

public final class Orderium extends JavaPlugin {
    public static final Orderium plugin = new Orderium();
    public final int bStatsID = 27569;
    public Metrics metrics = null;
    public boolean shouldEnable = true; // This would be false if the config file failed to load
    public static boolean isFolia;

    private Storage storage;
    private Economy econ;
    public final MiniMessage mm = MiniMessage.miniMessage();

    public Storage getStorage() { return storage; }
    public @NotNull DataCache getDataCache() { return DataCache.getInstance(); }
    public Economy getEcon() { return econ; }

    public void setStorage(Storage storage) { this.storage = storage; }

    @Override
    public void onEnable() {
        if (!plugin.shouldEnable) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupEconomy()) {
            Log.warn("Orderium disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        isFolia = isFolia();

        Storage.init();
        storage = createStorage();
        EconUtils.init();
        AdminToolGUI.init();

        ChooseItemGUI.init();

        reloadBStats();

        if (cache.checkForUpdates) {
            Bukkit.getAsyncScheduler().runNow(this, task -> {
               final String newVer = UpdateUtils.checkForUpdates();
               if (newVer == null) return;
               Log.warn("A new version of Orderium (" + newVer + ") is available");
               Log.info(mm.deserialize("<aqua>Download it on <green>Modrinth<gray>: <blue><u>https://modrinth.com/plugin/orderium/version/" + newVer));
            });
        }

        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new DialogListener(), this);
        Bukkit.getPluginManager().registerEvents(new DisconnectListener(), this);
        PacketEvents.getAPI().getEventManager().registerListener(new SignGUI(), PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager().registerListener(new ContainerContentListener(), PacketListenerPriority.NORMAL);

        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                DispatchUtil.entity(p, () -> PDCUtils.removeCollected(p));
            }

        }, 1, 1, TimeUnit.MINUTES);

        Log.info("Orderium enabled");
    }

    public Storage createStorage() {
        switch (cache.storageMethod) {
            case SQLITE -> {
                return SQLStorage.sqlite();
            }
            case MYSQL -> {
                return SQLStorage.mysql();
            }
            default -> {
                return SQLStorage.h2();
            }
        }
    }

    private boolean checkVault() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    private boolean setupEconomy() {
        if (!checkVault()) return false;
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

    public void reloadBStats() {

        if (cache.bStats) {
            if (metrics == null)
                metrics = new Metrics(plugin, bStatsID);
        } else if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }
}
