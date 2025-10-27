package de.thisisfel1x.forceitembattle.settings.impl;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.settings.BooleanSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;

public class BackpackSetting extends BooleanSetting {

    public BackpackSetting() {
        super(
                "BACKPACK",
                Component.text("Backpack"),
                List.of(
                        Component.text("Wenn aktiv kann der Backpack verwendet werden", NamedTextColor.GRAY)
                ),
                Material.CHEST,
                true
        );
    }

    @Override
    public void applyEffect(ForceItemBattle plugin) {

    }

    @Override
    public void removeEffect(ForceItemBattle plugin) {

    }
}
