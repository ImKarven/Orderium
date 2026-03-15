package me.karven.orderium.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

import static me.karven.orderium.load.Orderium.plugin;

public class DispatchUtil {

    public static ScheduledTask async(Runnable toRun) {
        return async(t -> toRun.run());
    }

    public static ScheduledTask async(Consumer<ScheduledTask> toRun) {
        return Bukkit.getAsyncScheduler().runNow(plugin, toRun);
    }
}
