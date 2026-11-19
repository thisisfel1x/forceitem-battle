package de.thisisfel1x.forceitembattle.world;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes the data pack that overrides the temperature and vegetation noise of the overworld for a
 * {@link BiomeSize}. Generated on every start, so it always matches the config
 */
public final class BiomeSizeDatapack {

    /** Pack format range of the supported versions, filled in by the build (gradle.properties) */
    private static final String FORMAT_RESOURCE = "datapack.properties";

    private BiomeSizeDatapack() {
    }

    /**
     * (Re-)creates the pack in the directory - an existing directory is replaced
     */
    public static void write(Path directory, BiomeSize biomeSize) throws IOException {
        delete(directory);

        Properties formats = loadFormats();
        String packMeta = """
                {
                  "pack": {
                    "description": "ForceItemBattle: biome size %s (generated from config.yml on every start)",
                    "min_format": %s,
                    "max_format": %s
                  }
                }
                """.formatted(biomeSize.name(), formats.getProperty("min_format"), formats.getProperty("max_format"));

        Path noiseDirectory = directory.resolve("data/minecraft/worldgen/noise");
        Files.createDirectories(noiseDirectory);
        Files.writeString(directory.resolve("pack.mcmeta"), packMeta, StandardCharsets.UTF_8);
        Files.writeString(noiseDirectory.resolve("temperature.json"),
                noiseJson(biomeSize.temperatureFirstOctave(), BiomeSize.TEMPERATURE_AMPLITUDES), StandardCharsets.UTF_8);
        Files.writeString(noiseDirectory.resolve("vegetation.json"),
                noiseJson(biomeSize.vegetationFirstOctave(), BiomeSize.VEGETATION_AMPLITUDES), StandardCharsets.UTF_8);
    }

    public static void delete(Path directory) throws IOException {
        if (!Files.exists(directory)) return;

        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    /**
     * The first octave is written twice: Minecraft 26.x renamed {@code firstOctave} (1.21.x) to {@code base_octave}.
     * Both versions ignore the field they do not know, so one file works for every supported version
     */
    static String noiseJson(int firstOctave, double[] amplitudes) {
        String amplitudeList = Arrays.stream(amplitudes)
                .mapToObj(amplitude -> String.format(Locale.ROOT, "%.1f", amplitude))
                .collect(Collectors.joining(", "));
        return """
                {
                  "firstOctave": %d,
                  "base_octave": %d,
                  "amplitudes": [%s]
                }
                """.formatted(firstOctave, firstOctave, amplitudeList);
    }

    private static Properties loadFormats() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = BiomeSizeDatapack.class.getClassLoader().getResourceAsStream(FORMAT_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException(FORMAT_RESOURCE + " is missing in the plugin jar");
            }
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        for (String key : new String[]{"min_format", "max_format"}) {
            if (!properties.getProperty(key, "").matches("\\d+")) {
                throw new IOException(FORMAT_RESOURCE + ": " + key + " is not a pack format - built without Gradle?");
            }
        }
        return properties;
    }
}
