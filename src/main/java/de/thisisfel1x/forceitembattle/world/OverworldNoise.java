package de.thisisfel1x.forceitembattle.world;

/**
 * The overworld noises the biome size data pack overrides, with their vanilla parameters
 * ({@code data/minecraft/worldgen/noise/<name>.json} of the server jar, unchanged since 1.18)
 *
 * <p>Temperature and vegetation (humidity) decide nothing but the biome. Continentalness decides where ocean,
 * coast and inland are and shapes the terrain height along with it - shrinking it makes oceans and continents
 * smaller together, so oceans do not take over the map when the other biomes get smaller
 */
public enum OverworldNoise {

    TEMPERATURE("temperature", -10, 1.2453007926713473, 1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
    VEGETATION("vegetation", -8, 0.9494731054427978, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0),
    CONTINENTALNESS("continentalness", -9, 0.8880832896205223, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);

    private final String fileName;
    private final int vanillaFirstOctave;
    /** Only in the 26.3+ format: up to 26.2 the game computed this factor itself */
    private final double baseAmplitude;
    private final double[] amplitudes;

    OverworldNoise(String fileName, int vanillaFirstOctave, double baseAmplitude, double... amplitudes) {
        this.fileName = fileName;
        this.vanillaFirstOctave = vanillaFirstOctave;
        this.baseAmplitude = baseAmplitude;
        this.amplitudes = amplitudes;
    }

    public String fileName() {
        return this.fileName;
    }

    public int vanillaFirstOctave() {
        return this.vanillaFirstOctave;
    }

    public double baseAmplitude() {
        return this.baseAmplitude;
    }

    public double[] amplitudes() {
        return this.amplitudes.clone();
    }
}
