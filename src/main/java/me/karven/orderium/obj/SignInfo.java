package me.karven.orderium.obj;

import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.block.BlockType;

import java.util.function.Consumer;

public record SignInfo(Consumer<String> action, BlockType signBlock, int line, Vector3i pos) {
}
