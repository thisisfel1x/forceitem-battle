package de.thisisfel1x.forceitembattle.utils;

import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

public class ItemRegistry {

    private static final List<ItemStack> FINDABLE_ITEMS = new ArrayList<>();

    /**
     * Materials that are banned by their exact name. Use this for single items - a substring
     * rule here would accidentally hit unrelated materials.
     */
    private static final Set<String> BANNED_EXACT = Set.of(
            // Only obtainable in creative mode
            "LIGHT",
            "BUDDING_AMETHYST",
            "REINFORCED_DEEPSLATE",
            "FARMLAND",
            "FROGSPAWN",
            "JIGSAW",
            "MACE"
    );

    /**
     * Materials are banned if their name starts with one of these.
     */
    private static final List<String> BANNED_PREFIXES = List.of(
            // End dimension blocks (END_STONE, END_ROD, END_PORTAL_FRAME, ...) - but not ENDER_PEARL & co.
            "END_"
    );

    /**
     * Materials are banned if their name ends with one of these.
     */
    private static final List<String> BANNED_SUFFIXES = List.of(
            // All ores (need silk touch), but not e.g. SPORE_BLOSSOM
            "_ORE",
            "_FROGLIGHT",
            "_SKULL",
            "_HEAD"
    );

    /**
     * Materials are banned if their name contains one of these. Keep these specific enough
     * that they only match the intended family of items.
     */
    private static final List<String> BANNED_FRAGMENTS = List.of(
            "COMMAND_BLOCK",
            "BEDROCK",
            "BARRIER",
            "STRUCTURE",
            "KNOWLEDGE_BOOK",
            "NETHERITE",
            "BEACON",
            "SUSPICIOUS_SAND",
            "SUSPICIOUS_GRAVEL",
            "TURTLE",
            "SCULK",
            "ELYTRA",
            "SLIME",
            "SHULKER_",
            "GOAT_HORN",
            "MUSIC_DISC_",
            "TIPPED_",
            "CHORUS",
            "OMINOUS_BOTTLE",
            "LINGERING_POTION",
            "ANCIENT_DEBRIS",
            "HEAVY_CORE",
            "BREEZE_ROD",
            "ARMADILLO",
            "DISC_FRAGMENT",
            "ECHO_SHARD",
            "DRAGON_BREATH",
            "PATTERN",
            "DEAD",
            "CORAL",
            "PETRIFIED",
            "NETHER_STAR",
            "SNIFFER_EGG",
            "DIRT_PATH",
            "WITHER_ROSE",
            "FERMENTED",
            "RECOVERY_",
            "SPECTRAL_",
            "HONEY_",
            "TEST_",
            "SPONGE",
            "PALE",
            "TORCHFLOWER",
            "CREAKING",
            "RESIN",
            "PHANTOM_MEMBRANE",
            "GHAST_TEAR",
            "POTTERY_SHERD",
            "SMITHING_TEMPLATE",
            "TRIAL_KEY",
            "SPAWNER",
            "TRIDENT",
            "PITCHER_POD",
            "WIND_CHARGE",
            "TRIM",
            "WAXED",
            "EXPOSED",
            "WEATHERED",
            "VAULT",
            "FRAME",
            "DRAGON_EGG",
            "BLUE_ICE",
            "DEBUG_STICK"
    );

    public static void initialize(Collection<String> additionalBanned, Logger logger) {
        if (!FINDABLE_ITEMS.isEmpty()) return;

        Set<String> configBanned = new HashSet<>();
        for (String entry : additionalBanned) {
            String name = entry.trim().toUpperCase(Locale.ROOT);
            if (name.isEmpty()) continue;

            if (Material.getMaterial(name) == null) {
                logger.warning("items.additional-banned: Unbekanntes Material '" + entry + "' wird ignoriert.");
                continue;
            }
            configBanned.add(name);
        }

        List<Material> findableMaterials = Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy())
                .filter(Material::isItem)
                .filter(m -> !m.isAir())
                .filter(m -> !isBanned(m))
                .filter(m -> !configBanned.contains(m.name()))
                .filter(m -> !MaterialTags.COMMAND_BLOCKS.isTagged(m))
                .filter(m -> !MaterialTags.EXPOSED_COPPER_BLOCKS.isTagged(m))
                .filter(m -> !MaterialTags.INFESTED_BLOCKS.isTagged(m))
                .filter(m -> !MaterialTags.MUSIC_DISCS.isTagged(m))
                .filter(m -> !MaterialTags.OXIDIZED_COPPER_BLOCKS.isTagged(m))
                .filter(m -> !MaterialTags.SKULLS.isTagged(m))
                .filter(m -> !MaterialTags.SPAWN_EGGS.isTagged(m))
                .filter(m -> !MaterialTags.WAXED_COPPER_BLOCKS.isTagged(m))
                .filter(m -> !MaterialTags.WEATHERED_COPPER_BLOCKS.isTagged(m))
                .filter(m -> !MaterialTags.PURPUR.isTagged(m))
                .filter(m -> !Tag.COPPER_GOLEM_STATUES.isTagged(m))
                .filter(m -> !Tag.SHULKER_BOXES.isTagged(m))
                .filter(m -> !Tag.ITEMS_TRIM_MATERIALS.isTagged(m))
                .filter(m -> !Tag.CANDLES.isTagged(m))
                .toList();

        for (Material material : findableMaterials) {
            FINDABLE_ITEMS.add(new ItemStack(material));
        }

        logger.info(FINDABLE_ITEMS.size() + " Items im Pool.");
    }

    /**
     * @return all materials that can be chosen as an item to find
     */
    public static List<Material> getFindableMaterials() {
        return FINDABLE_ITEMS.stream().map(ItemStack::getType).toList();
    }

    public static List<ItemStack> getNewShuffledItemList() {
        List<ItemStack> shuffledList = new ArrayList<>(FINDABLE_ITEMS);
        Collections.shuffle(shuffledList);
        return shuffledList;
    }

    private static boolean isBanned(Material material) {
        String name = material.name();

        return BANNED_EXACT.contains(name)
                || BANNED_PREFIXES.stream().anyMatch(name::startsWith)
                || BANNED_SUFFIXES.stream().anyMatch(name::endsWith)
                || BANNED_FRAGMENTS.stream().anyMatch(name::contains);
    }
}
