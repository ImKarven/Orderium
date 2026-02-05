package me.karven.orderium.obj;

import com.github.retrooper.packetevents.util.Vector3i;

import java.util.function.Consumer;

public record SignInfo(Consumer<String> action, int signBlockId, int line, Vector3i pos) {
}
