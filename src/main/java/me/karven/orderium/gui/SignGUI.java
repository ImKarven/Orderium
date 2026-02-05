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
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import me.karven.orderium.data.ConfigManager;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SignInfo;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class SignGUI implements PacketListener {
    private static final HashMap<Player, SignInfo> sessionsList = new HashMap<>();
    private static ConfigManager cache;

    public static void init(Orderium plugin) {
        cache = plugin.getConfigs();
    }

    public static void newSession(Player p, Consumer<String> action, List<String> lines, int signBlockId, int line) {
        final int x = (int) Math.floor(p.getX());
        int y = (int) Math.ceil(p.getY());
        final int z = (int) Math.floor(p.getZ());
        if (p.getPitch() < 0) {
            y -= 4;
        } else y += 5;

        final Vector3i pos = new Vector3i(x, y, z);
        int blockId = cache.getSignBlockId();

        final WrapperPlayServerBlockChange blockUpdatePacket = new WrapperPlayServerBlockChange(pos, blockId);
        final List<NBTString> nbtLines = lines.stream().map(NBTString::new).toList();

        final NBTCompound nbt = new NBTCompound();
        final NBTCompound frontText = new NBTCompound();
        frontText.setTag("messages", new NBTList<>(NBTType.STRING, nbtLines));
        nbt.setTag("front_text", frontText);

        final WrapperPlayServerBlockEntityData changeTextPacket = new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, nbt);
        final WrapperPlayServerOpenSignEditor openSignPacket = new WrapperPlayServerOpenSignEditor(pos, true);

        PacketEvents.getAPI().getPlayerManager().sendPacket(p, blockUpdatePacket);
        PacketEvents.getAPI().getPlayerManager().sendPacket(p, changeTextPacket);
        PacketEvents.getAPI().getPlayerManager().sendPacket(p, openSignPacket);
        sessionsList.put(p, new SignInfo(action, signBlockId, line, pos));
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;
        final Player player = e.getPlayer();
        if (!sessionsList.containsKey(player)) return;
        final WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(e);
        final String[] lines = wrapper.getTextLines();
        final SignInfo info = sessionsList.get(player);

        info.action().accept(lines[info.line() - 1]);
        final Vector3i pos = info.pos();
        player.getWorld().refreshChunk(pos.getX() >> 4, pos.getZ() >> 4); // Bitwise operator to divide these 2 coords to 16 to convert it to chunk coords

        sessionsList.remove(player);

//        // Try to send the packet to revert the original block but I'm so lost to revert it if it's a block entity so I just resend the chunk for good.
//        final Vector3i pos = wrapper.getBlockPosition();
//        final World world = player.getWorld();
//        final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
//
//        final BlockData bukkitBlockData = block.getBlockData();
//        final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(bukkitBlockData);
//        final WrapperPlayServerBlockChange blockChangePacket = new WrapperPlayServerBlockChange(pos, state);
//        PacketEvents.getAPI().getPlayerManager().sendPacket(player, blockChangePacket);
//
//        if (!(block.getState() instanceof TileState tileState)) return;
//        final CraftWorld craftWorld = (CraftWorld) world;
//        final CraftBlockEntityState<?> craftState = ()
//        final NBTCompound nbt = new NBTCompound();
    }

    @Override
    public void onPacketSend(@NonNull PacketSendEvent e) {
        if (e.getPacketType() != PacketType.Play.Server.BLOCK_CHANGE) return;
        final Player player = e.getPlayer();
        if (!sessionsList.containsKey(player)) return;
        final WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(e);
        final SignInfo info = sessionsList.get(player);
        if (wrapper.getBlockPosition().equals(info.pos())) {
            e.setCancelled(true);
        }
    }

}
