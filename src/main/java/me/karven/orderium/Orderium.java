package me.karven.orderium;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.faststats.bukkit.BukkitContext;
import me.karven.orderium.config.Config;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.guiframework.GUIListener;
import me.karven.orderium.listener.ContainerContentListener;
import me.karven.orderium.listener.DisconnectListener;
import me.karven.orderium.listener.ServerLoadListener;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.storage.implementation.SQLStorage;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.Log;
import me.karven.orderium.utils.PDCUtils;
import me.karven.orderium.utils.UpdateUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.karven.orderium.config.Config.config;
import static me.karven.orderium.utils.Values.ERROR_TRACKER;

public final class Orderium extends JavaPlugin {
    public static Orderium plugin;
    public final int bStatsID = 27569;
    public org.bstats.bukkit.Metrics bStatsMetrics = null;
    public static boolean isFolia;

    public final String faststatsToken = "241271513528286847e7c7ee08df7ec9";
    private final BukkitContext faststatsContext = new BukkitContext.Factory(this, faststatsToken)
            .errorTrackerService(ERROR_TRACKER)
            .metrics(dev.faststats.Metrics.Factory::create)
            .create();

    private Storage storage;
    private Economy economy = null;
    public final MiniMessage mm = MiniMessage.miniMessage();

    public Storage getStorage() { return storage; }
    public @NotNull DataCache getDataCache() { return DataCache.getInstance(); }
    public Economy getEconomy() { return economy; }

    public void setStorage(Storage storage) { this.storage = storage; }

    @Override
    public void onEnable() {
        plugin = this;
        faststatsContext.ready();
        isFolia = isFolia();
        AdminToolGUI.init();
        try {
            Config.reload();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        storage = createStorage();
        OrderiumCommands.register();
        Bukkit.getPluginManager().registerEvents(new ServerLoadListener(), this);
        Log.info("Orderium enabled");
    }

    @Override
    public void onDisable() {
        faststatsContext.shutdown();
    }

    public void postEconomyRegistration() {
        if (economy == null) {
            Log.severe("No economy plugin found. Orderium cannot work without an economy.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        checkUpdates();
        registerListeners();
        startCollectLimitResetLoop();

        Log.info("Orderium initialization complete");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new DisconnectListener(), this);
        PacketEvents.getAPI().getEventManager().registerListener(new SignGUI(), PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager().registerListener(new ContainerContentListener(), PacketListenerPriority.NORMAL);
    }

    private void startCollectLimitResetLoop() {
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                DispatchUtil.entity(p, () -> PDCUtils.removeCollected(p));
            }

        }, 1, 1, TimeUnit.MINUTES);
    }

    private void checkUpdates() {
        if (config.checkForUpdates) {
            Bukkit.getAsyncScheduler().runNow(this, task -> {
                final String newVer = UpdateUtils.checkForUpdates();
                if (newVer == null) return;
                Log.warn("A new version of Orderium (" + newVer + ") is available");
                Log.info(mm.deserialize("<aqua>Download it on <green>Modrinth<gray>: <blue><u>https://modrinth.com/plugin/orderium/version/" + newVer));
            });
        }
    }

    public Storage createStorage() {
        return SQLStorage.sqlite();
    }

    private boolean checkVault() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    public void setupEconomy() {
        if (!checkVault()) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }
    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void reloadBStats(final Config config) {
        if (config.bStats) {
            if (bStatsMetrics == null)
                bStatsMetrics = new org.bstats.bukkit.Metrics(plugin, bStatsID);
        } else if (bStatsMetrics != null) {
            bStatsMetrics.shutdown();
            bStatsMetrics = null;
        }
    }
}
