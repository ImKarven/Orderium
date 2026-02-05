package me.karven.orderium.obj;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import lombok.Getter;
import lombok.Setter;
import me.karven.orderium.load.Orderium;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
public class SlotInfo implements Cloneable {
    private int slot;
    private List<String> lore;
    private String displayName;
    private Material type;

    public SlotInfo(int slot, List<String> lore, String displayName, Material type) {
        this.slot = slot;
        this.lore = lore;
        this.displayName = displayName;
        this.type = type;
    }

    public void addDefault(ConfigFile config, String section) {
        config.addDefault(section + ".slot", slot);
        if (!lore.isEmpty()) config.addDefault(section + ".lore", lore);
        config.addDefault(section + ".display-name", displayName);
        config.addDefault(section + ".type", type.toString());
    }

    public void deserialize(ConfigSection section) {
        if (section == null) {
            Orderium.getInst().getLogger().severe("Button deserialization failed because section is null at " + displayName);
            return;
        }
        slot = section.getInteger("slot");
        lore = section.getStringList("lore");
        displayName = section.getString("display-name");
        type = Material.valueOf(section.getString("type", "AIR").toUpperCase());
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
