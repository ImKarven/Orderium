package me.karven.orderium.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import lombok.Setter;
import me.karven.orderium.data.ConfigCache;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.folia.IFFolia;
import me.karven.orderium.gui.*;
import me.karven.orderium.listener.ContainerContentListener;
import me.karven.orderium.listener.DialogListener;
import me.karven.orderium.listener.DisconnectListener;
import me.karven.orderium.obj.Order;
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

import java.util.concurrent.TimeUnit;

@Getter
public final class Orderium extends JavaPlugin {
    public static Orderium plugin;
    public static boolean isFolia;
    public final DialogListener DIALOG_LISTENER = new DialogListener();
    public final DisconnectListener DISCONNECT_LISTENER = new DisconnectListener();

    private ConfigCache configs;
    @Setter
    private Storage storage;
    private DataCache dataCache;
    private Economy econ;
    public final MiniMessage mm = MiniMessage.miniMessage();
    public int VERSION = -1;

    @Override
    public void onLoad() {
        PacketEvents.getAPI().getEventManager().registerListener(new SignGUI(), PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager().registerListener(new ContainerContentListener(), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        plugin = this;
        Log.init();
        isFolia = isFolia();
        if (!setupEconomy()) {
            Log.warn("Orderium disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        UpdateUtils.init();
        if (!UpdateUtils.downloadItems()) {
            saveResource("items.db", false);
        }
        saveResource("modified_items.db", false);

        new IFFolia(this);
        dataCache = new DataCache();
        configs = new ConfigCache();
        Storage.init();
        storage = createStorage();
        AlgoUtils.init();
        MainGUI.init();
        EconUtils.init();
        Order.init();
        ConvertUtils.init();
        PlayerUtils.init();
        AdminToolGUI.init();

        ChooseItemGUI.init();

        NewOrderDialog.init();
        DeliveryConfirmDialog.init();
        ManageOrderDialog.init();

        if (configs.isBStats()) {
            final int pluginId = 27569;
            new Metrics(this, pluginId);
        }

        if (configs.isCheckForUpdates()) {
            Bukkit.getAsyncScheduler().runNow(this, t -> {
               final String newVer = UpdateUtils.checkForUpdates();
               if (newVer == null) return;
               Log.warn("A new version of Orderium (" + newVer + ") is available");
               Log.info(mm.deserialize("<aqua>Download it on <green>Modrinth<gray>: <blue><u>https://modrinth.com/plugin/orderium/version/" + newVer));
            });
        }

        Bukkit.getPluginManager().registerEvents(DISCONNECT_LISTENER, this);
        Bukkit.getPluginManager().registerEvents(DIALOG_LISTENER, this);

        Bukkit.getAsyncScheduler().runAtFixedRate(this, t -> {

            for (Player p : Bukkit.getOnlinePlayers()) {
                DispatchUtil.entity(p, () -> PDCUtils.removeCollected(p));
            }

        }, 1, 1, TimeUnit.MINUTES);
    }

    public Storage createStorage() {
        switch (configs.getStorageMethod()) {
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
