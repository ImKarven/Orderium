package me.karven.orderium.gui;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import io.papermc.paper.math.Position;
import lombok.val;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.obj.SignInfo;
import me.karven.orderium.utils.Log;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockType;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class SignGUI implements PacketListener {
    private static final HashMap<Player, SignInfo> sessionsList = new HashMap<>();
    private static MiniMessage mm;
    private static Orderium plugin;

    public static void newSession(Player p, Consumer<String> action, List<String> lines, BlockType blockType, int line) {
        if (blockType == null) {
            Log.warn("Failed to show sign GUI because of invalid sign block");
            action.accept("");
            return;
        }
        if (sessionsList.containsKey(p)) return;

        final int x = (int) Math.floor(p.getX());
        int y = (int) Math.ceil(p.getY());
        final int z = (int) Math.floor(p.getZ());
        if (p.getPitch() < 0) {
            y -= 4;
        } else y += 5;

        // PAPER PACKET SENDING IMPLEMENTATION
        Sign signState = (Sign) blockType.createBlockData().createBlockState();
        val frontSide = signState.getSide(Side.FRONT);
        for (int i = 0; i < 4; i++) frontSide.line(i, mm.deserialize(lines.get(i)));
        val loc = new Location(p.getWorld(), x, y, z);
        p.sendBlockChange(loc, signState.getBlockData());
        p.sendBlockUpdate(loc, signState);
        p.openVirtualSign(Position.block(loc), Side.FRONT);
        // END

        sessionsList.put(p, new SignInfo(action, blockType, line, new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
    }

    public SignGUI(Orderium plugin) {
        mm = plugin.mm;
        SignGUI.plugin = plugin;
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;
        final Player player = e.getPlayer();
        final SignInfo info = sessionsList.get(player);
        if (info == null) return;
        final WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(e);
        final Vector3i pos = info.pos();
        if (!wrapper.getBlockPosition().equals(pos)) return;
        final String[] lines = wrapper.getTextLines();
        info.action().accept(lines[info.line() - 1]);

        sessionsList.remove(player);

        val world = player.getWorld();
        val loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());

        // PAPER
        Bukkit.getRegionScheduler().run(plugin, loc, t -> {
            player.sendBlockChange(loc, world.getBlockData(loc));
            if (world.getBlockState(loc) instanceof TileState tile) player.sendBlockUpdate(loc, tile);
        });
        // END

        e.setCancelled(true);
    }
}
