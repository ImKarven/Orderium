package me.karven.orderium.utils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.Getter;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SortTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class NMSUtils {
    @Getter
    private static final Set<ItemStack> itemsList = new HashSet<>();
    private static final Set<ItemStack> AZ = new TreeSet<>(
            Comparator.comparing(item -> item.getType().toString())
    ),
    ZA = new TreeSet<>(
            Comparator.comparing(item -> item.getType().toString(), Comparator.reverseOrder())
    );
    @Getter
    private static final BlockEntityType<?> signEntity = BlockEntityType.SIGN;
    private static Orderium plugin;
    public static void init(Orderium pl) {
        plugin = pl;
        generateItemsList();
    }

    private static void generateItemsList() {
        final MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false, server.registryAccess());
        final Registry<CreativeModeTab> registry = BuiltInRegistries.CREATIVE_MODE_TAB;
        Set<net.minecraft.world.item.ItemStack> minecraftItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (CreativeModeTab tab : registry) {
            tab.buildContents(params);
        }
        for (CreativeModeTab tab : registry) {
            minecraftItems.addAll(tab.getSearchTabDisplayItems());
        }
        final List<ItemStack> bukkitItems = minecraftItems.stream().map(net.minecraft.world.item.ItemStack::asBukkitCopy).toList();
        itemsList.addAll(bukkitItems);
        AZ.addAll(bukkitItems);
        ZA.addAll(bukkitItems);

        plugin.getLogger().info("Loaded " + itemsList.size() + " items.");
    }

    public static Set<ItemStack> getItems(SortTypes sortType) {
        if (sortType == SortTypes.Z_A) {
            return ZA;
        }
        return AZ;
    }

    public static int getBlockStateId(String identifier) {
        final Optional<Block> optionalBlock = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(identifier));
        if (optionalBlock.isEmpty()) {
            Orderium.getInst().getLogger().severe("Failed to parse block with identifier " + identifier);
            return -1;
        }
        final Block block = optionalBlock.get();
        final BlockState state = block.defaultBlockState();
        return Block.getId(state);
    }

}
