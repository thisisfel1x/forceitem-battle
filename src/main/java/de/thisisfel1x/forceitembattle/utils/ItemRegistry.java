package de.thisisfel1x.forceitembattle.utils;

import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemRegistry {

    private static final List<ItemStack> FINDABLE_ITEMS = new ArrayList<>();

    public static void initialize() {
        if (!FINDABLE_ITEMS.isEmpty()) return;

        List<Material> items = Arrays.asList(Material.values());

        /*List<Tag<Material>> tags = new ArrayList<>();
        tags.add(Tag.ITEMS_ACACIA_LOGS);
        tags.add(Tag.ITEMS_ANVIL);
        tags.add(Tag.ITEMS_AXES);
        tags.add(Tag.ITEMS_BAMBOO_BLOCKS);
        tags.add(Tag.ITEMS_BANNERS);
        tags.add(Tag.ITEMS_BARS);
        tags.add(Tag.ITEMS_BEDS);
        tags.add(Tag.ITEMS_BIRCH_LOGS);
        tags.add(Tag.ITEMS_BOATS);
        tags.add(Tag.ITEMS_BREWING_FUEL);
        tags.add(Tag.ITEMS_BUTTONS);
        tags.add(Tag.ITEMS_CAT_FOOD);
        tags.add(Tag.ITEMS_CHAINS);
        tags.add(Tag.ITEMS_CHERRY_LOGS);
        tags.add(Tag.ITEMS_CHEST_ARMOR);
        tags.add(Tag.ITEMS_CHEST_BOATS);
        tags.add(Tag.ITEMS_CHICKEN_FOOD);
        tags.add(Tag.ITEMS_COALS);
        tags.add(Tag.ITEMS_COPPER);
        tags.add(Tag.ITEMS_COPPER_CHESTS);
        tags.add(Tag.ITEMS_COW_FOOD);
        tags.add(Tag.ITEMS_DARK_OAK_LOGS);
        tags.add(Tag.ITEMS_DIAMOND_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_DIRT);
        tags.add(Tag.ITEMS_DOORS);
        tags.add(Tag.ITEMS_DYEABLE);
        tags.add(Tag.FENCE_GATES);
        tags.add(Tag.FENCES);
        tags.add(Tag.ITEMS_FISHES);
        tags.add(Tag.ITEMS_FLOWERS);
        tags.add(Tag.ITEMS_GOLD_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_HANGING_SIGNS);
        tags.add(Tag.ITEMS_HEAD_ARMOR);
        tags.add(Tag.ITEMS_HOES);
        tags.add(Tag.ITEMS_IRON_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_JUNGLE_LOGS);
        tags.add(Tag.ITEMS_LANTERNS);
        tags.add(Tag.ITEMS_LEAVES);
        tags.add(Tag.ITEMS_LEG_ARMOR);
        tags.add(Tag.ITEMS_LOGS);
        tags.add(Tag.ITEMS_MANGROVE_LOGS);
        tags.add(Tag.ITEMS_MEAT);
        tags.add(Tag.ITEMS_OAK_LOGS);
        tags.add(Tag.ITEMS_RAILS);
        tags.add(Tag.ITEMS_PLANKS);
        tags.add(Tag.ITEMS_RAILS);
        tags.add(Tag.ITEMS_SAND);
        tags.add(Tag.ITEMS_SHOVELS);
        tags.add(Tag.ITEMS_SAND);
        tags.add(Tag.ITEMS_SLABS);
        tags.add(Tag.ITEMS_STAIRS);
        tags.add(Tag.ITEMS_SPRUCE_LOGS);
        tags.add(Tag.SMALL_FLOWERS);
        tags.add(Tag.FLOWERS);
        tags.add(Tag.ITEMS_SWORDS);
        tags.add(Tag.ITEMS_TRAPDOORS);
        tags.add(Tag.ITEMS_WALLS);
        tags.add(Tag.ITEMS_WARPED_STEMS);
        tags.add(Tag.ITEMS_WART_BLOCKS);
        tags.add(Tag.ITEMS_WOOL);
        tags.add(Tag.ITEMS_WOOL_CARPETS);
        tags.add(Tag.ITEMS_WOODEN_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_STONE_BRICKS);
        tags.add(Tag.ITEMS_STONE_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_IRON_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_GOLD_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_DIAMOND_TOOL_MATERIALS);
        tags.add(Tag.ITEMS_COPPER);
        tags.add(Tag.ITEMS_ENCHANTABLE_BOW);
        tags.add(Tag.ITEMS_ENCHANTABLE_CROSSBOW);
        tags.add(Tag.BADLANDS_TERRACOTTA);*/


        /*List<Material> findableMaterials = tags.stream()
                .flatMap(materialTag -> materialTag.getValues().stream())
                .distinct()
                .filter(Material::isItem)
                .filter(m -> !m.isLegacy())
                .filter(m -> !isBanned(m))
                .toList();*/

        List<Material> findableMaterials = items.stream()
                .filter(Material::isItem)
                .filter(m -> !m.isLegacy())
                .filter(m -> !isBanned(m))
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
                .filter(m -> !Tag.AIR.isTagged(m))
                .filter(m -> !Tag.COPPER_GOLEM_STATUES.isTagged(m))
                .filter(m -> !Tag.SHULKER_BOXES.isTagged(m))
                .filter(m -> !Tag.ITEMS_TRIM_MATERIALS.isTagged(m))
                .filter(m -> !Tag.CANDLES.isTagged(m))
                .toList();

        for (Material material : findableMaterials) {
            FINDABLE_ITEMS.add(new ItemStack(material));
        }
    }

    public static List<ItemStack> getNewShuffledItemList() {
        List<ItemStack> shuffledList = new ArrayList<>(FINDABLE_ITEMS);
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(shuffledList);
        }
        return shuffledList;
    }

    private static boolean isBanned(Material material) {
        String name = material.name();
        return name.contains("COMMAND_BLOCK") ||
                name.contains("BEDROCK") ||
                name.contains("BARRIER") ||
                name.contains("AIR") ||
                name.contains("STRUCTURE") ||
                name.contains("KNOWLEDGE_BOOK") ||
                name.contains("NETHERITE") ||
                name.contains("ORE") ||
                name.contains("BEACON") ||
                name.contains("END_CRYSTAL") ||
                name.contains("SUSPICIOUS_SAND") ||
                name.contains("SUSPICIOUS_GRAVEL") ||
                name.contains("TURTLE") ||
                name.contains("SCULK") ||
                name.contains("ELYTRA") ||
                name.contains("GOAT_HORN") ||
                name.contains("MUSIC_DISC_") ||
                name.contains("TIPPED_") ||
                name.contains("OMINOUS_BOTTLE") ||
                name.contains("LINGERING_POTION") ||
                name.contains("ANCIENT_DEBRIS") ||
                name.contains("HEAVY_CORE") ||
                name.contains("BREEZE_ROD") ||
                name.contains("ARMADILLO") ||
                name.contains("DISC_FRAGMENT") ||
                name.contains("ECHO_SHARD") ||
                name.contains("DRAGON_BREATH") ||
                name.contains("PATTERN") ||
                name.contains("DEAD") ||
                name.contains("CORAL") ||
                name.contains("PETRIFIED") ||
                name.contains("WITHER_ROSE") ||
                name.contains("FERMENTED") ||
                name.contains("SPONGE") ||
                name.contains("PALE") ||
                name.contains("CREAKING") ||
                name.contains("RESIN") ||
                name.contains("PHANTOM_MEMBRANE") ||
                name.contains("GHAST_TEAR") ||
                name.contains("POTTERY_SHERD") ||
                name.contains("SMITHING_TEMPLATE") ||
                name.contains("TRIAL_KEY") ||
                name.contains("SPAWNER") ||
                name.contains("TRIDENT") ||
                name.contains("WIND_CHARGE") ||
                name.contains("MACE") ||
                name.contains("END") ||
                name.contains("TRIM") ||
                name.contains("WAXED") ||
                name.contains("EXPOSED") ||
                name.contains("WEATHERED") ||
                name.contains("VAULT") ||
                name.contains("_SKULL") ||
                name.contains("_HEAD") ||
                name.contains("FRAME") ||
                name.contains("DRAGON_EGG") ||
                name.contains("DEBUG_STICK");
    }
}
