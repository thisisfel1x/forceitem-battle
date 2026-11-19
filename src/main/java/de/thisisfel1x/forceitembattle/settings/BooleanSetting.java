package de.thisisfel1x.forceitembattle.settings;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.LanguageManager;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.utils.Items;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A setting that is switched on and off by clicking its icon. Settings without an effect of their own (checked
 * by listeners via {@link SettingsManager#isEnabled(SettingType)}) only need a constructor
 */
public abstract class BooleanSetting extends AbstractSetting<Boolean> {

    public BooleanSetting(Message name, Message description, Material iconMaterial, boolean defaultValue) {
        super(name, description, iconMaterial, defaultValue);
    }

    @Override
    public ItemStack getIcon(Player viewer) {
        LanguageManager languageManager = ForceItemBattle.getInstance().getLanguageManager();

        // Item lore has no line breaks - every line of the description is a lore line of its own
        List<Component> lore = new ArrayList<>(languageManager.renderLines(viewer, this.description));
        lore.add(Component.empty());
        lore.add(languageManager.render(viewer, this.value
                ? Message.SETTINGS_STATUS_ENABLED
                : Message.SETTINGS_STATUS_DISABLED));

        ItemStack icon = Items.named(this.iconMaterial, languageManager.render(viewer, this.name), this.value);
        icon.editMeta(meta -> {
            meta.lore(Items.noItalic(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return icon;
    }

    @Override
    public void handleIconClick(Player player) {
        this.setValue(!this.value);

        if (this.value) {
            player.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1f, 1.2f));
        } else {
            player.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, Sound.Source.PLAYER, 1f, 0.8f));
        }
    }

    @Override
    public void applyEffect(ForceItemBattle plugin) {
    }

    @Override
    public void removeEffect(ForceItemBattle plugin) {
    }
}
