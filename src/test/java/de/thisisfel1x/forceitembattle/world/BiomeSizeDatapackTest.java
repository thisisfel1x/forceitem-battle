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
        assertEquals(-10, BiomeSize.NORMAL.temperatureFirstOctave());
        assertEquals(-8, BiomeSize.NORMAL.vegetationFirstOctave());
        assertTrue(BiomeSize.NORMAL.isVanilla());
    }

    @Test
    void smallerBiomesRaiseTheFirstOctave() throws IOException {
        BiomeSizeDatapack.write(this.directory, BiomeSize.TINY);

        JsonObject temperature = this.read("data/minecraft/worldgen/noise/temperature.json");
        assertEquals(-8, temperature.get("firstOctave").getAsInt());
        assertEquals(-8, temperature.get("base_octave").getAsInt(), "name since Minecraft 26.1");
        assertEquals(6, temperature.getAsJsonArray("amplitudes").size());
        assertEquals(1.5, temperature.getAsJsonArray("amplitudes").get(0).getAsDouble());

        JsonObject vegetation = this.read("data/minecraft/worldgen/noise/vegetation.json");
        assertEquals(-6, vegetation.get("firstOctave").getAsInt());
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
