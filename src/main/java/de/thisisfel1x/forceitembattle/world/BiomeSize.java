package de.thisisfel1x.forceitembattle.world;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Size of the biomes in newly generated chunks ({@code world.biome-size} in the config)
 *
 * <p>Minecraft picks the biome of a position from six noise values. Three of them get a higher frequency (see
 * {@link OverworldNoise}): temperature and humidity, which decide nothing but the biome, and continentalness, so
 * oceans and continents shrink along with the biomes. Erosion and weirdness stay untouched: mountains and rivers
 * are generated like in vanilla. Every step raises the first octave of the noises by one and doubles their
 * frequency, so a biome is half as wide
 */
public enum BiomeSize {

    /** Vanilla */
    NORMAL(0),
    /** Biomes half as wide (a quarter of the area) */
    SMALL(1),
    /** Biomes a quarter as wide (a sixteenth of the area) */
    TINY(2);

    private final int octaveShift;

    BiomeSize(int octaveShift) {
        this.octaveShift = octaveShift;
    }

    public boolean isVanilla() {
        return this.octaveShift == 0;
    }

    public int firstOctave(OverworldNoise noise) {
        return noise.vanillaFirstOctave() + this.octaveShift;
    }

    public static Optional<BiomeSize> parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(size -> size.name().equals(normalized)).findFirst();
    }
}
