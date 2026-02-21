package me.karven.orderium.obj;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import lombok.Getter;
import lombok.Setter;
import me.karven.orderium.load.Orderium;
import me.karven.orderium.utils.ConvertUtils;
import org.bukkit.inventory.ItemType;

import java.util.List;

@Getter
@Setter
public class SlotInfo implements Cloneable {
    private int slot;
    private List<String> lore;
    private String displayName;
    private ItemType type;

    public SlotInfo(int slot, List<String> lore, String displayName, ItemType type) {
        this.slot = slot;
        this.lore = lore;
        this.displayName = displayName;
        this.type = type;
    }


    public void addDefault(ConfigFile config, String section) {
        if (slot != -1) config.addDefault(section + ".slot", slot);
        if (!lore.isEmpty()) config.addDefault(section + ".lore", lore);
        config.addDefault(section + ".display-name", displayName);
        config.addDefault(section + ".type", type.getKey().toString());
    }

    public void deserialize(ConfigSection section) {
        if (section == null) {
            Orderium.getInst().getLogger().severe("Button deserialization failed because section is null");
            return;
        }
        slot = section.get("slot") == null ? -1 : section.getInteger("slot");
        lore = section.getStringList("lore");
        displayName = section.getString("display-name");
        type = ConvertUtils.getItemType(section.getString("type", "minecraft:stone"));
    }

    @Override
    public SlotInfo clone() {
        try {
            return (SlotInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
