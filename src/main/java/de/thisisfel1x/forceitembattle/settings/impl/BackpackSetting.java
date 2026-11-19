package de.thisisfel1x.forceitembattle.settings.impl;

import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.settings.BooleanSetting;
import org.bukkit.Material;

/**
 * Checked by {@code /backpack}
 */
public class BackpackSetting extends BooleanSetting {

    public BackpackSetting() {
        super(Message.SETTINGS_BACKPACK_NAME, Message.SETTINGS_BACKPACK_DESCRIPTION, Material.CHEST, true);
    }
}
