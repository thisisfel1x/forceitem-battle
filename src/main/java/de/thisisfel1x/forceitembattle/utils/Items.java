package de.thisisfel1x.forceitembattle.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Small helpers for building display items.
 */
public final class Items {

    private Items() {
    }

    public static ItemStack named(Material material, Component name) {
        return named(material, name, false);
    }

    public static ItemStack named(Material material, Component name, boolean glow) {
        ItemStack itemStack = new ItemStack(material);
        itemStack.editMeta(meta -> {
            meta.displayName(noItalic(name));
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return itemStack;
    }

    public static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> noItalic(List<Component> components) {
        return components.stream().map(Items::noItalic).toList();
    }
}
