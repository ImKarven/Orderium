package me.karven.orderium.data;

import me.karven.orderium.obj.orderitem.VanillaItem;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class VanillaItems {
    private static final MethodHandle AS_BUKKIT_COPY = findAsBukkitCopy();

    public static Collection<VanillaItem> load() {
        MinecraftServer server = MinecraftServer.getServer();
        RegistryAccess registryAccess = server.registryAccess();
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, false, registryAccess);
        Registry<CreativeModeTab> tabs = BuiltInRegistries.CREATIVE_MODE_TAB;
        Collection<net.minecraft.world.item.ItemStack> minecraftItems = new HashSet<>();
        Set<VanillaItem> items = new HashSet<>();

        for (CreativeModeTab tab : tabs) {
            tab.buildContents(params);
            minecraftItems.addAll(tab.getSearchTabDisplayItems());
        }

        for (net.minecraft.world.item.ItemStack mcItem : minecraftItems) {
            items.add(new VanillaItem(asBukkitCopy(mcItem), true));
        }
        return items;
    }

    private static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack mcItem) {
        try {
            return (ItemStack) AS_BUKKIT_COPY.invokeExact(mcItem);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to convert " + mcItem + " to a bukkit item", e);
        }
    }

    private static MethodHandle findAsBukkitCopy() {
        for (Method method : CraftItemStack.class.getMethods()) {
            if (!method.getName().equals("asBukkitCopy") || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(net.minecraft.world.item.ItemStack.class)) continue;
            try {
                return MethodHandles.publicLookup().unreflect(method).asType(MethodType.methodType(ItemStack.class, net.minecraft.world.item.ItemStack.class));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Could not find CraftItemStack#asBukkitCopy");
    }
}
