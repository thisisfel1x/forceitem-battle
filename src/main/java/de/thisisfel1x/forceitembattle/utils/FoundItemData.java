package de.thisisfel1x.forceitembattle.utils;

import org.bukkit.inventory.ItemStack;

public record FoundItemData(ItemStack item, long timestamp, boolean usedJoker) {
}
