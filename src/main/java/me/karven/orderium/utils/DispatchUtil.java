package me.karven.orderium.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

import static me.karven.orderium.load.Orderium.plugin;

public class DispatchUtil {

    public static ScheduledTask async(Runnable toRun) {
        return async(task -> toRun.run());
    }

    public static ScheduledTask async(Consumer<ScheduledTask> toRun) {
        return Bukkit.getAsyncScheduler().runNow(plugin, toRun);
    }

    public static ScheduledTask entity(Entity entity, Runnable toRun) {
        return entity(entity, task -> toRun.run());
    }

    public static ScheduledTask entity(Entity entity, Consumer<ScheduledTask> toRun) {
        return entity.getScheduler().run(plugin, toRun, null);
    }
}
