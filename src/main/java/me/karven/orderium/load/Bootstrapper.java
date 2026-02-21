package me.karven.orderium.load;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.val;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.MainGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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
                    new MainGUI((Player) ctx.getSource().getExecutor(), 0);

                    return 1;
                })
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            final String search = StringArgumentType.getString(ctx, "search");
                            new MainGUI((Player) ctx.getSource().getExecutor(), 0, search);
                            return 1;
                        })
                )
                .build();
    }

    private static LiteralCommandNode<CommandSourceStack> getOrderiumCmd(String alias) {
        val builder = Commands.literal(alias);
        builder
                .requires(predicate -> (predicate.getSender().hasPermission("orderium.admin")))
                .then(Commands.literal("reload")
                        .requires(predicate -> predicate.getSender().hasPermission("orderium.admin.reload"))
                        .executes(ctx -> {
                            Orderium.getInst().getConfigs().reload(() -> {

                                ctx.getSource().getSender().sendRichMessage("<green>Orderium reloaded");
                            });
                            return 1;
                        })
                )
                .then(Commands.literal("blacklist")
                        .requires(predicate ->
                                predicate.getExecutor() != null &&
                                predicate.getExecutor().hasPermission("orderium.admin.blacklist") &&
                                predicate.getExecutor() instanceof Player)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 0;

                            AdminToolGUI.openBlacklist(p);

                            return 1;
                        })
                )
                .then(Commands.literal("custom_items")
                        .requires(predicate ->
                                predicate.getExecutor() != null &&
                                        predicate.getExecutor().hasPermission("orderium.admin.custom-items") &&
                                        predicate.getExecutor() instanceof Player)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player p)) return 0;

                            AdminToolGUI.openCustomItems(p);

                            return 1;
                        })
                );

        return builder.build();
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
