package me.karven.orderium.obj;

import io.papermc.paper.math.Position;
import org.bukkit.block.BlockType;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public record SignInfo(Consumer<String> action, BlockType signBlock, int line, Position position) { }
