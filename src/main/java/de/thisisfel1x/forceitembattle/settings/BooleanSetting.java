package de.thisisfel1x.forceitembattle.settings;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class BooleanSetting extends AbstractSetting<Boolean> {

    public BooleanSetting(String key, Component name, List<Component> description, Material iconMaterial, Boolean defaultValue) {
        super(key, name, description, iconMaterial, defaultValue);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(this.iconMaterial);
        ItemMeta meta = icon.getItemMeta();

        meta.displayName(this.name
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>(this.description);
        lore.add(Component.empty());

        if (this.value) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            lore.add(Component.text("» Status: ", NamedTextColor.GRAY)
                    .append(Component.text("AKTIVIERT", NamedTextColor.GREEN)));
        } else {
            lore.add(Component.text("» Status: ", NamedTextColor.GRAY)
                    .append(Component.text("DEAKTIVIERT", NamedTextColor.RED)));
        }

        meta.lore(lore);
        icon.setItemMeta(meta);
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
}