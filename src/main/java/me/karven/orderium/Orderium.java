package me.karven.orderium;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.faststats.bukkit.BukkitContext;
import me.karven.orderium.config.Config;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.data.VanillaItems;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.guiframework.GUIListener;
import me.karven.orderium.listener.DisconnectListener;
import me.karven.orderium.listener.ServerLoadListener;
import me.karven.orderium.order.OrderService;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.storage.StorageSettings;
import me.karven.orderium.utils.*;
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
    public static boolean isFolia;

    public final String faststatsToken = "241271513528286847e7c7ee08df7ec9";
    private final BukkitContext faststatsContext = new BukkitContext.Factory(this, faststatsToken)
            .errorTrackerService(ERROR_TRACKER)
            .metrics(factory -> factory
                    .addMetric(CustomMetrics.API_USAGE)
                    .addMetric(CustomMetrics.ORDER_AMOUNT)
                    .addMetric(CustomMetrics.ITEMS_COLLECTED)
                    .addMetric(CustomMetrics.EXPERIMENTAL_FEATURES)
                    .onFlush(CustomMetrics::FLUSH)
                    .create())
            .create();

    private Storage storage;
    private OrderService orderService;
    private Economy economy = null;
    public final MiniMessage mm = MiniMessage.miniMessage();

    public Storage getStorage() { return storage; }
    public OrderService getOrderService() { return orderService; }
    public @NotNull DataCache getDataCache() { return DataCache.getInstance(); }
    public Economy getEconomy() { return economy; }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onEnable() {
        plugin = this;
        faststatsContext.ready();
        isFolia = isFolia();
        AdminToolGUI.init();

        getDataFolder().mkdirs();
        try {
            storage = Storage.open(StorageSettings.load(getDataFolder()), getDataFolder());
            orderService = new OrderService(storage);
            loadData();
        } catch (Exception e) {
            Log.error("Failed to load the storage. Check storage.yml. Orderium cannot work without it", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Log.info("Using " + storage.type() + " storage");

        try {
            Config.reload();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        OrderiumCommands.register();
        Bukkit.getPluginManager().registerEvents(new ServerLoadListener(), this);
        Log.info("Orderium enabled");
    }

    @Override
    public void onDisable() {
        faststatsContext.shutdown();
        if (storage != null) storage.close();
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
    }

    private void startCollectLimitResetLoop() {
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task ->
                        Bukkit.getGlobalRegionScheduler().run(this, _ -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p == null || !p.isOnline()) continue;
                                DispatchUtil.entity(p, () -> PDCUtils.removeCollected(p));
                            }
                        }),
                1, 1, TimeUnit.MINUTES);
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

    /// Load the orderable items and every order. Blocks until done
    private void loadData() {
        getDataCache().setItems(VanillaItems.load(), storage.items().findBlacklist(), storage.items().findCustomItems());
        orderService.loadAll();
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
}
