package me.karven.orderium;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.karven.orderium.config.Config;
import me.karven.orderium.gui.AdminToolGUI;
import me.karven.orderium.gui.MainGUI;
import me.karven.orderium.utils.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static me.karven.orderium.Orderium.plugin;
import static me.karven.orderium.config.Config.config;

public class OrderiumCommands {
    private static @NotNull Predicate<CommandSourceStack> permission(@NotNull String permission) {
        return predicate -> {
            final Entity executor = predicate.getExecutor();
            final String perm = "orderium." + permission;
            return executor == null ? predicate.getSender().hasPermission(perm) : executor.hasPermission(perm);
        };
    }

    private static @NotNull Predicate<CommandSourceStack> playerAndPermission(@NotNull String permission) {
        return predicate ->
                predicate.getExecutor() instanceof final Player player &&
                        player.hasPermission("orderium." + permission);
    }

    private static LiteralCommandNode<CommandSourceStack> getOrderCmd(String alias) {

        return Commands.literal(alias)
                .requires(playerAndPermission("use"))
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
                            final Entity executor = ctx.getSource().getExecutor();
                            final CommandSender sender = executor == null ? ctx.getSource().getSender() : executor;
                            Config.reloadAsync().thenAccept(ignored -> sender.sendRichMessage("<green>Orderium reloaded"));

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

    public static void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(getOrderiumCmd());

            for (final String alias : config.orderCommandAliases) {
                event.registrar().register(getOrderCmd(alias));
            }
        });
    }
}
