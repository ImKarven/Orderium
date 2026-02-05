package me.karven.orderium.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.data.DBManager;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.gui.NewOrderDialog;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.NMSUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Orderium extends JavaPlugin {
    private static Orderium instance;
    private ConfigManager configs;
    private DBManager dbManager;
    private Economy econ;
    public final MiniMessage mm = MiniMessage.miniMessage();
    public static final ClientVersion VERSION = ClientVersion.V_1_21_11;

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
        configs = new ConfigManager(this);
        dbManager = new DBManager(this);
        Order.init(this);
        ConvertUtils.init(this);
        NMSUtils.init(this);
        ChooseItemGUI.init(this);
        SignGUI.init(this);
        NewOrderDialog.init(this);
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
