package me.karven.orderium.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import lombok.val;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.gui.*;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
public final class Orderium extends JavaPlugin {
    private static Orderium instance;
    private ConfigManager configs;
    private DBManager dbManager;
    private Economy econ;
    public final MiniMessage mm = MiniMessage.miniMessage();
    public int VERSION;

    private CompletableFuture<Boolean> initVersion() {
        val ret = new CompletableFuture<Boolean>();
        // noinspection deprecation
        final int dataVer = Bukkit.getUnsafe().getDataVersion();
        dbManager.dataVersions().thenAccept(dataVersion -> {
            if (dataVer < 4438) { // 1.21.7 in data version
                ret.complete(false);
                return;
            }
            int maxVer = -1;
            for (int ver : dataVersion) {
                if (ver > maxVer && ver <= dataVer) maxVer = ver;
                if (dataVer == ver) {
                    VERSION = ver;
                    ret.complete(true);
                    return;
                }
            }
            VERSION = maxVer;
            ret.complete(true);
        });
        return ret;
    }

    public static Orderium getInst() {
        return instance;
    }

    @Override
    public void onLoad() {
        PacketEvents.getAPI().getEventManager().registerListener(new SignGUI(), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        instance = this;
        if (!setupEconomy()) {
            getLogger().severe("Orderium disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        UpdateUtils.init(this);
        if (!UpdateUtils.downloadItems()) {
            saveResource("items.db", false);
        }

        configs = new ConfigManager(this);
        dbManager = new DBManager(this);
        AdminToolGUI.init(this);
        MainGUI.init(this);
        YourOrderGUI.init(this);
        EconUtils.init(this);
        Order.init(this);
        ConvertUtils.init(this);
        PlayerUtils.init(this);
        PDCUtils.init(this);

        initVersion().thenAccept(success -> {
            if (!success) {
                this.getLogger().severe("You are using an unsupported server version (< 1.21.7). Please update to use the plugin");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
            NMSUtils.init(this).thenAccept(ignored -> ChooseItemGUI.init(this));
        });

        NewOrderDialog.init(this);
        DeliveryConfirmDialog.init(this);
        ManageOrderDialog.init(this);

        if (configs.isBStats()) {
            final int pluginId = 27569;
            final Metrics metrics = new Metrics(this, pluginId);
        }

        Bukkit.getAsyncScheduler().runAtFixedRate(this, t -> {

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getScheduler().run(this, t2 -> PDCUtils.removeCollected(p), null);
            }

        }, 1, 1, TimeUnit.MINUTES);
    }

    public void reloadConfig() {
        configs.reload(true);
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
}
