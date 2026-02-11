package me.karven.orderium.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.gui.*;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.EconUtils;
import me.karven.orderium.utils.NMSUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Orderium extends JavaPlugin {
    private static Orderium instance;
    private ConfigManager configs;
    private DBManager dbManager;
    private Economy econ;
    public final MiniMessage mm = MiniMessage.miniMessage();
    public final String VERSION = getVersion();

    private String getVersion() {
        final String mcVer = Bukkit.getMinecraftVersion();
        switch (mcVer) {
            case "1.21", "1.21.1" -> { return "1_21_1"; }
            case "1.21.2", "1.21.3" ->  { return "1_21_3"; }
            case "1.21.4" -> { return "1_21_4"; }
            case "1.21.5" -> { return "1_21_5"; }
            case "1.21.6" -> { return "1_21_6"; }
            case  "1.21.7", "1.21.8" -> { return "1_21_8"; }
            case "1.21.9", "1.21.10" -> { return "1_21_10"; }
            case "1.21.11" -> { return "1_21_11"; }
        }
        getLogger().severe("You're using an unsupported server version!");
        return null;
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
        if (VERSION == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupEconomy()) {
            getLogger().severe("Orderium disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveResource("items.db", true);

        configs = new ConfigManager(this);
        dbManager = new DBManager(this);
        MainGUI.init(this);
        EconUtils.init(this);
        Order.init(this);
        ConvertUtils.init(this);

        NMSUtils.init(this).thenAccept(ignored -> ChooseItemGUI.init(this));

        NewOrderDialog.init(this);
        DeliveryConfirmDialog.init(this);
        ManageOrderDialog.init(this);
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
