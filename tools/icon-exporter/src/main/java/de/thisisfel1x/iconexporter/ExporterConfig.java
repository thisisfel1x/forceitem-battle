package de.thisisfel1x.iconexporter;

import java.nio.file.Path;

/**
 * Settings passed in by Gradle as system properties (see build.gradle).
 *
 * @param iconDir          output directory for the rendered icons ({@code <item id>.png})
 * @param manifestFile     JSON file describing the export (Minecraft version, pack format, items)
 * @param previewDir       output directory for the preview and debug screenshots
 * @param scale            GUI scale used for rendering, icons are {@code 16 * scale} pixels big
 * @param previewIconSize  icon size used inside the preview resource pack
 */
record ExporterConfig(Path iconDir, Path manifestFile, Path previewDir, int scale, int previewIconSize) {

    static ExporterConfig fromSystemProperties() {
        return new ExporterConfig(
                Path.of(require("fib.icons.dir")),
                Path.of(require("fib.icons.manifest")),
                Path.of(require("fib.icons.preview")),
                Integer.getInteger("fib.icons.scale", 4),
                Integer.getInteger("fib.icons.previewSize", 32)
        );
    }

    private static String require(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("System property " + property + " is missing - start the exporter with "
                    + "./gradlew -p tools/icon-exporter runClientGameTest");
        }
        return value;
    }
}
