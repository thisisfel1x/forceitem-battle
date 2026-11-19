package de.thisisfel1x.forceitembattle.world;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Size of the biomes in newly generated chunks ({@code world.biome-size} in the config)
 *
 * <p>Minecraft picks the biome of a position from six noise values. Only two of them are changed: temperature and
 * humidity ("vegetation") - they decide nothing but the biome. Continentalness, erosion and weirdness, which also
 * shape the terrain, stay untouched: mountains, oceans and rivers are generated like in vanilla, there are just
 * more different biomes on them. Every step raises the first octave of both noises by one and doubles their
 * frequency, so a biome is half as wide
 */
public enum BiomeSize {

    /** Vanilla */
    NORMAL(0),
    /** Biomes half as wide (a quarter of the area) */
    SMALL(1),
    /** Biomes a quarter as wide (a sixteenth of the area) */
    TINY(2);

    // Vanilla noise parameters (net.minecraft.data.worldgen.NoiseData) - unchanged since 1.18
    static final int VANILLA_TEMPERATURE_FIRST_OCTAVE = -10;
    static final double[] TEMPERATURE_AMPLITUDES = {1.5, 0.0, 1.0, 0.0, 0.0, 0.0};
    static final int VANILLA_VEGETATION_FIRST_OCTAVE = -8;
    static final double[] VEGETATION_AMPLITUDES = {1.0, 1.0, 0.0, 0.0, 0.0, 0.0};

    private final int octaveShift;

    BiomeSize(int octaveShift) {
        this.octaveShift = octaveShift;
    }

    public boolean isVanilla() {
        return this.octaveShift == 0;
    }

    public int temperatureFirstOctave() {
        return VANILLA_TEMPERATURE_FIRST_OCTAVE + this.octaveShift;
    }

    public int vegetationFirstOctave() {
        return VANILLA_VEGETATION_FIRST_OCTAVE + this.octaveShift;
    }

    public static Optional<BiomeSize> parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(size -> size.name().equals(normalized)).findFirst();
    }
}
