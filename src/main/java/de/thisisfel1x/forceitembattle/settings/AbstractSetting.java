package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class AbstractSetting<T> {

    protected final String key;
    protected final Component name;
    protected final List<Component> description;
    protected final Material iconMaterial;
    protected final T defaultValue;
    protected T value;

    public AbstractSetting(String key, Component name, List<Component> description, Material iconMaterial, T defaultValue) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.iconMaterial = iconMaterial;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Component getName() {
        return name;
    }

    public abstract ItemStack getIcon();

    public abstract void handleIconClick(Player player);

    public abstract void applyEffect(ForceItemBattle plugin);

    public abstract void removeEffect(ForceItemBattle plugin);
}