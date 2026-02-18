package me.karven.orderium.gui;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SignInfo;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class SignGUI implements PacketListener {
    private static final HashMap<Player, SignInfo> sessionsList = new HashMap<>();
    private static final HashMap<Player, WrapperPlayServerBlockEntityData> dataChanges = new HashMap<>();
    private final Orderium plugin;

    public static void newSession(Player p, Consumer<String> action, List<String> lines, BlockType blockType, int line) {
        if (blockType == null) {
            Orderium.getInst().getLogger().severe("Failed to show sign GUI because of invalid sign block");
            action.accept("");
            return;
        }
        final int x = (int) Math.floor(p.getX());
        int y = (int) Math.ceil(p.getY());
        final int z = (int) Math.floor(p.getZ());
        if (p.getPitch() < 0) {
            y -= 4;
        } else y += 5;

        final Vector3i pos = new Vector3i(x, y, z);
        WrappedBlockState block = SpigotConversionUtil.fromBukkitBlockData(blockType.createBlockData());
        final WrapperPlayServerBlockChange blockUpdatePacket = new WrapperPlayServerBlockChange(pos, block);
        final List<NBTString> nbtLines = lines.stream().map(NBTString::new).toList();

        final NBTCompound nbt = new NBTCompound();
        final NBTCompound frontText = new NBTCompound();
        frontText.setTag("messages", new NBTList<>(NBTType.STRING, nbtLines));
        nbt.setTag("front_text", frontText);

        final WrapperPlayServerBlockEntityData changeTextPacket = new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, nbt);
        final WrapperPlayServerOpenSignEditor openSignPacket = new WrapperPlayServerOpenSignEditor(pos, true);

        send(p, blockUpdatePacket);
        send(p, changeTextPacket);
        send(p, openSignPacket);
        sessionsList.put(p, new SignInfo(action, blockType, line, pos));
    }

    public SignGUI(Orderium plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;
        final Player player = e.getPlayer();
        if (!sessionsList.containsKey(player)) return;
        final WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(e);
        final String[] lines = wrapper.getTextLines();
        final SignInfo info = sessionsList.get(player);

        final Vector3i pos = info.pos();
        final WrappedBlockState blockState = SpigotConversionUtil.fromBukkitBlockData(player.getWorld().getBlockData(pos.getX(), pos.getY(), pos.getZ())); // Might have to use region scheduler here for folia support
        final WrapperPlayServerBlockChange blockChangePacket = new WrapperPlayServerBlockChange(pos, blockState);
        final WrapperPlayServerBlockEntityData blockEntityDataPacket = dataChanges.get(player);

        // Sending the packets immediately doesn't work for some reason
        player.getScheduler().runDelayed(plugin, t -> {
            send(player, blockChangePacket);
            if (blockEntityDataPacket != null) send(player, blockEntityDataPacket);
        }, null, 2);

        info.action().accept(lines[info.line() - 1]);

        sessionsList.remove(player);
        dataChanges.remove(player);
    }

    @Override
    public void onPacketSend(@NonNull PacketSendEvent e) {
        switch (e.getPacketType()) {
            case PacketType.Play.Server.BLOCK_CHANGE -> {
                final Player player = e.getPlayer();
                if (!sessionsList.containsKey(player)) return;
                final WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(e);
                final SignInfo info = sessionsList.get(player);
                if (!wrapper.getBlockPosition().equals(info.pos())) return;
                e.setCancelled(true);
            }

            case PacketType.Play.Server.BLOCK_ENTITY_DATA -> {
                final Player player = e.getPlayer();
                final SignInfo info = sessionsList.get(player);
                if (info == null) return;
                final WrapperPlayServerBlockEntityData wrapper = new WrapperPlayServerBlockEntityData(e);
                if (!wrapper.getPosition().equals(info.pos())) return;
                dataChanges.remove(player);
                dataChanges.put(player, wrapper);
                e.setCancelled(true);
            }

            default -> {}
        }
    }

    private static void send(Player p, PacketWrapper<?> wrapper) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(p, wrapper);
    }
}
