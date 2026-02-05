package me.karven.orderium.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.utils.ConvertUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Deprecated // Use NewOrderDialog
public class NewOrderGUI {
    private int amount = 1;
    private ItemStack item = ItemStack.of(Material.STONE);
    private double moneyPer = 1;
    private final StaticPane buttonsPane = new StaticPane(0, 0, 9, 3);
    private final ConfigManager cache;
    private final Orderium plugin;
    private ChestGui gui;

    private final Consumer<ItemStack> whenChooseItem = itemStack -> {
        item = itemStack;
        updateButtons();
    };

    private final Consumer<Integer> whenChangeAmount = newAmount -> {
        amount = newAmount;
        updateButtons();
    };

    private final Consumer<Double> whenChangeMoney = money -> {
        moneyPer = money;
        updateButtons();
    };

    public NewOrderGUI(Orderium plugin, Player p) {
        this.cache = plugin.getConfigs();
        this.plugin = plugin;
        final MiniMessage mm = plugin.mm;
        this.gui = new ChestGui(3, ComponentHolder.of(mm.deserialize(cache.getNewOrderTitle())));
    }

    private void updateButtons() {
        buttonsPane.addItem(ConvertUtils.parseButton(cache.getMaterialButton(item.getType()), e -> {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
//            ChooseItemGUI.choose(p, whenChooseItem);
        }), cache.getMaterialButton().getSlot() % 9, cache.getMaterialButton().getSlot() / 9);

    }

}
