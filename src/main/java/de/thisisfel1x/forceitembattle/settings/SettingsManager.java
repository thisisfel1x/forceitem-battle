package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.settings.impl.BackpackSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsManager {

    private final ForceItemBattle forceItemBattle;
    private final List<AbstractSetting<?>> settings = new ArrayList<>();
    private final Map<String, AbstractSetting<?>> settingsMap = new HashMap<>();

    public SettingsManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        registerAllSettings();
    }

    private void registerAllSettings() {
        addSetting(new BackpackSetting());
    }

    private void addSetting(AbstractSetting<?> setting) {
        this.settings.add(setting);
        this.settingsMap.put(setting.getKey(), setting);
    }


    public List<AbstractSetting<?>> getSettings() {
        return this.settings;
    }


    public AbstractSetting<?> getSetting(String name) {
        return this.settingsMap.get(name);
    }


    public void applyAllGameSettings() {
        for (AbstractSetting<?> setting : settings) {
            setting.applyEffect(forceItemBattle);
        }
    }


    public void removeAllGameSettings() {
        for (AbstractSetting<?> setting : settings) {
            setting.removeEffect(forceItemBattle);
        }
    }
}