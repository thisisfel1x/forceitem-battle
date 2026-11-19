package de.thisisfel1x.forceitembattle.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeSizeDatapackTest {

    @TempDir
    Path directory;

    private JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(this.directory.resolve(path))).getAsJsonObject();
    }

    @Test
    void normalIsVanilla() {
        assertEquals(-10, BiomeSize.NORMAL.firstOctave(OverworldNoise.TEMPERATURE));
        assertEquals(-8, BiomeSize.NORMAL.firstOctave(OverworldNoise.VEGETATION));
        assertEquals(-9, BiomeSize.NORMAL.firstOctave(OverworldNoise.CONTINENTALNESS));
        assertTrue(BiomeSize.NORMAL.isVanilla());
    }

    @Test
    void smallerBiomesRaiseTheFirstOctave() throws IOException {
        BiomeSizeDatapack.write(this.directory, BiomeSize.TINY);

        assertEquals(-8, this.read("data/minecraft/worldgen/noise/temperature.json").get("firstOctave").getAsInt());
        assertEquals(-6, this.read("data/minecraft/worldgen/noise/vegetation.json").get("firstOctave").getAsInt());
        assertEquals(-7, this.read("data/minecraft/worldgen/noise/continentalness.json").get("firstOctave").getAsInt(),
                "oceans shrink along with the biomes");
    }

    @Test
    void writesTheFormatUpTo26_2() throws IOException {
        BiomeSizeDatapack.write(this.directory, BiomeSize.SMALL);

        JsonObject temperature = this.read("data/minecraft/worldgen/noise/temperature.json");
        assertEquals(-9, temperature.get("firstOctave").getAsInt());
        assertEquals(6, temperature.getAsJsonArray("amplitudes").size());
        assertEquals(1.5, temperature.getAsJsonArray("amplitudes").get(0).getAsDouble());
    }

    @Test
    void writesTheFormatSince26_3() throws IOException {
        BiomeSizeDatapack.write(this.directory, BiomeSize.SMALL);

        // Vanilla 26.3 continentalness.json with base_octave -9
        JsonObject continentalness = this.read("data/minecraft/worldgen/noise/continentalness.json");
        assertEquals(-8, continentalness.get("base_octave").getAsInt());
        assertEquals(0.8880832896205223, continentalness.get("base_amplitude").getAsDouble());
        assertEquals(9, continentalness.get("octave_count").getAsInt());
        assertEquals(9, continentalness.getAsJsonArray("amplitude_modifiers").size());
        assertEquals(2.0, continentalness.getAsJsonArray("amplitude_modifiers").get(2).getAsDouble());
    }

    @Test
    void packMetaContainsTheFormatRangeOfTheBuild() throws IOException {
        BiomeSizeDatapack.write(this.directory, BiomeSize.SMALL);

        JsonObject pack = this.read("pack.mcmeta").getAsJsonObject("pack");
        int min = pack.get("min_format").getAsInt();
        int max = pack.get("max_format").getAsInt();
        assertTrue(min > 0 && max >= min, "format range " + min + ".." + max);
    }

    @Test
    void rewritingReplacesTheOldPack() throws IOException {
        Files.createDirectories(this.directory.resolve("data/minecraft/worldgen/noise"));
        Files.writeString(this.directory.resolve("leftover.txt"), "old");

        BiomeSizeDatapack.write(this.directory, BiomeSize.SMALL);

        assertFalse(Files.exists(this.directory.resolve("leftover.txt")));
        assertEquals(-9, this.read("data/minecraft/worldgen/noise/temperature.json").get("firstOctave").getAsInt());
    }

    @Test
    void parsesConfigValues() {
        assertEquals(BiomeSize.SMALL, BiomeSize.parse(" small ").orElseThrow());
        assertTrue(BiomeSize.parse("huge").isEmpty());
    }
}
