package me.karven.orderium.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import me.karven.orderium.utils.PDCUtils;
import org.bukkit.NamespacedKey;

import java.util.Optional;

/// This class is used for listening to container content packets to remove unnecessary nbt data for compatibility with custom items and to make it as vanilla as possible
public class ContainerContentListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.WINDOW_ITEMS) return;
        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
        for (ItemStack item : packet.getItems()) {
            Optional<NBTCompound> nbtOptional = item.getComponent(ComponentTypes.CUSTOM_DATA);
            if (nbtOptional.isEmpty()) continue;
            NBTCompound nbt = nbtOptional.get();
            NBTCompound persistentData = nbt.getCompoundTagOrNull("PublicBukkitValues");
            if (persistentData == null) continue;
            persistentData.removeTag("orderium:if-uuid");
            for (NamespacedKey key : PDCUtils.KEYS) {
                persistentData.removeTag(key.toString());
            }
            if (persistentData.getTags().isEmpty()) nbt.removeTag("PublicBukkitValues");
            if (nbt.getTags().isEmpty()) item.unsetComponent(ComponentTypes.CUSTOM_DATA);
        }
        event.markForReEncode(true);
    }
}
