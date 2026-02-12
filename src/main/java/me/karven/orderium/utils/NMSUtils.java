package me.karven.orderium.utils;

import lombok.Getter;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SortTypes;
import net.kyori.adventure.key.Key;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

// Previously used minecraft internal to get the items, I'm lazy to rename it after changing
public class NMSUtils {

    private static final Registry<BlockType> blockRegistry = Registry.BLOCK;

    private static final Set<ItemStack>
            AZ = new TreeSet<>(AlgoUtils.getComparator(SortTypes.A_Z)),
            ZA = new TreeSet<>(AlgoUtils.getComparator(SortTypes.Z_A));
    @Getter
    private static final BlockEntityType<?> signEntity = BlockEntityType.SIGN;
    private static Orderium plugin;
    public static CompletableFuture<Void> init(Orderium pl) {
        plugin = pl;
        return generateItemsList();
    }

    private static CompletableFuture<Void> generateItemsList() {
        final CompletableFuture<Void> res = new CompletableFuture<>();
        plugin.getDbManager().getItems().thenAccept(bukkitItems -> {
            AZ.addAll(bukkitItems);
            ZA.addAll(bukkitItems);

            plugin.getLogger().info("Loaded " + AZ.size() + " items.");
            res.complete(null);
        });
        return res;
    }

    public static Set<ItemStack> getItems(SortTypes sortType) {
        if (sortType == SortTypes.Z_A) {
            return ZA;
        }
        return AZ;
    }
    public static BlockType getBlockType(String identifier) {
        return blockRegistry.get(Key.key(identifier));
    }
}
