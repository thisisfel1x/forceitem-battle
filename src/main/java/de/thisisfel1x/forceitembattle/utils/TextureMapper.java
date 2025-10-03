package de.thisisfel1x.forceitembattle.utils;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bukkit.Material;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextureMapper {

    public static void generate() throws IOException {
        /*if (args.length == 0) {
            System.err.println("Fehler: Bitte gib den Pfad zum 'textures'-Ordner deines Resource Packs an.");
            System.err.println("Beispiel: java -jar TextureMapper.jar \"C:/Users/User/Downloads/1.21/assets/minecraft/textures\"");
            return;
        }*/

        Path texturesPath = Paths.get("C:\\Users\\weber\\AppData\\Roaming\\LabyMod\\versions\\assets\\minecraft\\textures");
        /*if (!Files.isDirectory(texturesPath)) {
            System.err.println("Fehler: Der angegebene Pfad ist kein gültiger Ordner: " + texturesPath);
            return;
        }*/

        System.out.println("1. Sammle alle Server-seitigen Material-Keys...");
        Set<String> serverKeys = generateServerKeys();
        System.out.println("   -> " + serverKeys.size() + " Keys gefunden.");

        System.out.println("2. Scanne alle Textur-Dateien im angegebenen Ordner...");
        List<String> texturePaths = scanTextureFiles(texturesPath);
        System.out.println("   -> " + texturePaths.size() + " Texturen gefunden.");

        System.out.println("3. Führe Mapping von Keys zu Texturen durch (dies kann einen Moment dauern)...");
        Map<String, String> mapping = mapKeysToTextures(serverKeys, texturePaths);
        System.out.println("   -> Mapping abgeschlossen.");

        System.out.println("4. Schreibe die Konfigurationsdatei 'texture_map.yml'...");
        writeOutputConfig(mapping, Paths.get("texture_map.yml"));
        System.out.println("   -> Fertig! Die Datei 'texture_map.yml' wurde im aktuellen Ordner erstellt.");
    }

    private static Set<String> generateServerKeys() {
        return Arrays.stream(Material.values())
                .filter(m -> m.isItem() && !m.isLegacy() && !m.name().startsWith("LEGACY_"))
                .map(m -> m.getKey().toString())
                .collect(Collectors.toSet());
    }

    private static List<String> scanTextureFiles(Path texturesPath) throws IOException {
        try (Stream<Path> pathStream = Files.walk(texturesPath)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".png"))
                    .map(p -> texturesPath.relativize(p).toString().replace("\\", "/").replace(".png", ""))
                    .collect(Collectors.toList());
        }
    }

    private static Map<String, String> mapKeysToTextures(Set<String> serverKeys, List<String> texturePaths) {
        Map<String, String> finalMap = new TreeMap<>();
        LevenshteinDistance levenshtein = new LevenshteinDistance();

        for (String key : serverKeys) {
            String keyName = key.substring(key.indexOf(':') + 1);

            Optional<String> perfectMatch = texturePaths.stream()
                    .filter(p -> p.equals("item/" + keyName) || p.equals("block/" + keyName))
                    .findFirst();

            if (perfectMatch.isPresent()) {
                finalMap.put(key, perfectMatch.get());
                continue;
            }

            List<String> entityCandidates = texturePaths.stream()
                    .filter(p -> p.startsWith("entity/" + keyName))
                    .toList();

            if (!entityCandidates.isEmpty()) {
                String bestEntityMatch = entityCandidates.stream()
                        .min(Comparator
                                .comparingInt((String p) -> p.contains("/normal") ? 0 : 1) // Expliziter Typ (String p)
                                .thenComparingInt(String::length))
                        .get();
                finalMap.put(key, bestEntityMatch);
                continue;
            }

            List<String> candidates = texturePaths.stream()
                    .filter(p -> p.contains(keyName))
                    .toList();

            if (candidates.isEmpty()) {
                System.out.println("   WARNUNG: Keine Textur-Kandidaten für " + key + " gefunden.");
                continue;
            }

            String bestMatch = candidates.stream()
                    .min(Comparator
                            .comparingInt((String p) -> p.endsWith("_top") ? 0 : 1)
                            .thenComparingInt((String p) -> p.endsWith("_front") ? 0 : 1)
                            .thenComparingInt(String::length)
                            .thenComparingInt((String p) -> levenshtein.apply(keyName, p.substring(p.lastIndexOf('/') + 1)))
                    ).orElse(candidates.getFirst());

            finalMap.put(key, bestMatch);
        }
        return finalMap;
    }

    private static void writeOutputConfig(Map<String, String> mapping, Path outputPath) throws IOException {
        // 1. DumperOptions konfigurieren, um das Ausgabeformat zu erzwingen.
        DumperOptions options = new DumperOptions();

        // Dies ist die wichtigste Einstellung: Erzwingt den sauberen, zeilenbasierten "Block"-Stil.
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2); // Standard-Einrückung von 2 Leerzeichen.
        options.setPrettyFlow(true); // Verbessert die Lesbarkeit, falls Flow-Style doch mal vorkommt.
        options.setCanonical(false); // Verhindert unnötige Anführungszeichen und Typ-Tags.

        // 2. Eine neue Yaml-Instanz mit den benutzerdefinierten Optionen erstellen.
        Yaml yaml = new Yaml(options);

        // 3. Die Map wie gewohnt in die Datei schreiben.
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            yaml.dump(mapping, writer);
        }
    }
}