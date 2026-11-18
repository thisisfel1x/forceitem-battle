package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.ForceItemBattle;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class SettingsManager {

    private final ForceItemBattle forceItemBattle;
    // EnumMap iterates in declaration order of SettingType - the order of the settings GUI
    private final Map<SettingType, AbstractSetting<?>> settings = new EnumMap<>(SettingType.class);

    public SettingsManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;

        for (SettingType type : SettingType.values()) {
            this.settings.put(type, type.create());
        }
    }

    public Collection<AbstractSetting<?>> getSettings() {
        return Collections.unmodifiableCollection(this.settings.values());
    }

    public AbstractSetting<?> getSetting(SettingType type) {
        return this.settings.get(type);
    }

    /**
     * @return the value of a boolean setting, {@code false} if the setting is not a boolean setting
     */
    public boolean isEnabled(SettingType type) {
        return this.settings.get(type) instanceof BooleanSetting booleanSetting
                && Boolean.TRUE.equals(booleanSetting.getValue());
    }

    public void applyAllGameSettings() {
        for (AbstractSetting<?> setting : this.settings.values()) {
            setting.applyEffect(this.forceItemBattle);
        }
    }

    public void removeAllGameSettings() {
        for (AbstractSetting<?> setting : this.settings.values()) {
            setting.removeEffect(this.forceItemBattle);
        }
    }
}
