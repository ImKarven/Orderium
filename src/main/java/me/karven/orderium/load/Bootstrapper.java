package me.karven.orderium.load;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.MainGUI;
import me.karven.orderium.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static me.karven.orderium.data.ConfigCache.cache;
import static me.karven.orderium.load.Orderium.plugin;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class Bootstrapper implements PluginBootstrap {

    private static @NotNull Predicate<CommandSourceStack> permission(@NotNull String permission) {
        return predicate ->
                predicate.getExecutor() != null &&
                predicate.getExecutor().hasPermission("orderium." + permission);
    }

    private static @NotNull Predicate<CommandSourceStack> playerAndPermission(@NotNull String permission) {
        return predicate ->
                predicate.getExecutor() instanceof final Player player &&
                player.hasPermission("orderium." + permission);
    }

    private static LiteralCommandNode<CommandSourceStack> getOrderCmd(String alias) {

        return Commands.literal(alias)
                .requires(predicate -> predicate.getExecutor() instanceof Player)
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                    MainGUI mainGUI = new MainGUI(player, 0);
                    PlayerUtils.openGUI(player, mainGUI.getGUI(), true);
                    return 1;
                })
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                            final String search = StringArgumentType.getString(ctx, "search");
                            MainGUI mainGUI = new MainGUI(player, 0, search);
                            PlayerUtils.openGUI(player, mainGUI.getGUI(), true);
                            return 1;
                        })
                )
                .build();
    }

    private static LiteralCommandNode<CommandSourceStack> getOrderiumCmd() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("orderium");

        builder
                .requires(permission("admin"))
                .then(Commands.literal("reload")
                        .requires(permission("admin.reload"))
                        .executes(ctx -> {
                            assert ctx.getSource().getExecutor() != null;
                            cache.reload(() -> ctx.getSource().getExecutor().sendRichMessage("<green>Orderium reloaded"));
                            return 1;
                        })
                )
                .then(Commands.literal("blacklist")
                        .requires(playerAndPermission("admin.blacklist"))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof final Player p)) return 2;

                            PlayerUtils.openGUI(p, AdminToolGUI.getBlacklistGUI(), true);

                            return 1;
                        })
                )
                .then(Commands.literal("custom_items")
                        .requires(playerAndPermission("admin.custom-items"))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof final Player p)) return 2;

                            PlayerUtils.openGUI(p, AdminToolGUI.getCustomItemsGUI(), true);

                            return 1;
                        })
                )
        ;
        return builder.build();
    }

    @Override
    public void bootstrap(@NotNull BootstrapContext ctx) {
        ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> {
            for (final String orderAlias : cache.orderCommandAliases) {
                e.registrar().register(getOrderCmd(orderAlias));
            }
            e.registrar().register(getOrderiumCmd());
        });
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext ctx) {
        return plugin;
    }
}
