package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.settings.impl.BackpackSetting;
import de.thisisfel1x.forceitembattle.settings.impl.NoDamageSetting;
import de.thisisfel1x.forceitembattle.settings.impl.NoHungerSetting;
import de.thisisfel1x.forceitembattle.settings.impl.StarterToolsSetting;

import java.util.function.Supplier;

/**
 * Registry of all game settings. Every constant creates its setting - {@link SettingsManager} registers them in
 * declaration order, which is also the order in the settings GUI
 * <p>
 * New setting: implement it in {@code settings.impl}, add a constant here and its texts to the language files
 * ({@code Message})
 */
public enum SettingType {

    BACKPACK(BackpackSetting::new),
    NO_HUNGER(NoHungerSetting::new),
    NO_DAMAGE(NoDamageSetting::new),
    STARTER_TOOLS(StarterToolsSetting::new);

    private final Supplier<AbstractSetting<?>> factory;

    SettingType(Supplier<AbstractSetting<?>> factory) {
        this.factory = factory;
    }

    AbstractSetting<?> create() {
        return this.factory.get();
    }
}
