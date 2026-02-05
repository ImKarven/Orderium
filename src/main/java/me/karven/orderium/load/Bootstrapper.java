package me.karven.orderium.load;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.gui.ChooseItemGUI;
import me.karven.orderium.gui.MainGUI;
import me.karven.orderium.gui.NewOrderDialog;
import me.karven.orderium.gui.SignGUI;
import me.karven.orderium.obj.Order;
import me.karven.orderium.utils.NMSUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class Bootstrapper implements PluginBootstrap {

    private static LiteralCommandNode<CommandSourceStack> getOrderCmd(String alias) {
        return Commands.literal(alias)
                .requires(predicate -> predicate.getExecutor() instanceof Player)
                .executes(ctx -> {
                    new MainGUI(Orderium.getInst(), (Player) ctx.getSource().getExecutor(), 0);

                    return 1;
                })
                .build();
    }

    private static LiteralCommandNode<CommandSourceStack> getOrderiumCmd(String alias) {
        return Commands.literal(alias)
                .requires(predicate -> predicate.getExecutor() != null && predicate.getExecutor().hasPermission("orderium.admin"))
                .executes(ctx -> {
                    return 1;
                })
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            Orderium.getInst().reloadConfig();

                            return 1;
                        })
                )
                .then(Commands.literal("test")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 1;
                            final ConfigManager config = Orderium.getInst().getConfigs();
                            SignGUI.newSession(p, s -> {}, config.getLines(), config.getSignBlockId(), config.getSearchLine());
                            return 1;
                        })
                )
                .then(Commands.literal("test2")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 1;
                            NewOrderDialog.newSession(p, p.getInventory().getItemInMainHand());
                            return 1;
                        })
                )
                .then(Commands.literal("test3")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 1;
                            for (final Order order : Orderium.getInst().getDbManager().getOrders()) {
                                p.sendRichMessage("id=" + order.id() + " item=" + order.item().getType() + " money-per=" + order.moneyPer() + " amount=" + order.amount());
                            }
                            return 1;
                        })
                )
                .build();
    }

    @Override
    public void bootstrap(@NonNull BootstrapContext ctx) {
        ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> {
            e.registrar().register(getOrderCmd("orders"));
            e.registrar().register(getOrderCmd("order"));
            e.registrar().register(getOrderiumCmd("orderium"));
        });
    }

    @Override
    public @NonNull JavaPlugin createPlugin(@NonNull PluginProviderContext ctx) {
        return new Orderium();
    }

}
