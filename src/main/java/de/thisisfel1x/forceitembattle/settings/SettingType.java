package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.settings.impl.BackpackSetting;

import java.util.function.Supplier;

/**
 * Registry of all game settings. Every constant creates its setting - {@link SettingsManager} registers them in
 * declaration order, which is also the order in the settings GUI.
 * <p>
 * New setting: implement it in {@code settings.impl} and add a constant here.
 */
public enum SettingType {

    BACKPACK(BackpackSetting::new);

    private final Supplier<AbstractSetting<?>> factory;

    SettingType(Supplier<AbstractSetting<?>> factory) {
        this.factory = factory;
    }

    AbstractSetting<?> create() {
        return this.factory.get();
    }
}
