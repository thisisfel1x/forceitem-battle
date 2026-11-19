package de.thisisfel1x.forceitembattle.settings.impl;

import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.settings.BooleanSetting;
import org.bukkit.Material;

/**
 * Checked by {@code EntityExhaustionListener}: without exhaustion saturation and food bar never drop
 */
public class NoHungerSetting extends BooleanSetting {

    public NoHungerSetting() {
        super(Message.SETTINGS_NO_HUNGER_NAME, Message.SETTINGS_NO_HUNGER_DESCRIPTION, Material.COOKED_BEEF, true);
    }
}
