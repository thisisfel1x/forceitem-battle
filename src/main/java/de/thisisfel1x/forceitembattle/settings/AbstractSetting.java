package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractSetting<T> {

    protected final Message name;
    protected final Message description;
    protected final Material iconMaterial;
    protected final T defaultValue;
    protected T value;

    public AbstractSetting(Message name, Message description, Material iconMaterial, T defaultValue) {
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

    public Message getName() {
        return name;
    }

    /**
     * @param viewer the player the settings GUI is shown to - texts are in its language
     */
    public abstract ItemStack getIcon(Player viewer);

    public abstract void handleIconClick(Player player);

    /**
     * Called when the game starts (after the countdown)
     */
    public abstract void applyEffect(ForceItemBattle plugin);

    /**
     * Called when the game ends
     */
    public abstract void removeEffect(ForceItemBattle plugin);
}
