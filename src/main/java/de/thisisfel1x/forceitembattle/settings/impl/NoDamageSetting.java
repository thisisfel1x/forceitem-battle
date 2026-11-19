package de.thisisfel1x.forceitembattle.settings.impl;

import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.settings.BooleanSetting;
import org.bukkit.Material;

/**
 * Checked by {@code EntityDamageListener}
 */
public class NoDamageSetting extends BooleanSetting {

    public NoDamageSetting() {
        super(Message.SETTINGS_NO_DAMAGE_NAME, Message.SETTINGS_NO_DAMAGE_DESCRIPTION, Material.GOLDEN_APPLE, false);
    }
}
