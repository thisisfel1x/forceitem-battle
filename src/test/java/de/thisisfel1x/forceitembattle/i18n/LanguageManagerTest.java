package de.thisisfel1x.forceitembattle.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageManagerTest {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    /** Every placeholder some message uses - rendering with all of them must leave no tag behind */
    private static final TagResolver ALL_PLACEHOLDERS = TagResolver.resolver(
            Placeholder.unparsed("player", "Steve"),
            Placeholder.unparsed("item", "Diamond"),
            Placeholder.unparsed("team", "Team #1"),
            Placeholder.unparsed("max", "64"),
            Placeholder.unparsed("dots", "..."),
            Placeholder.unparsed("time", "12:34"),
            Placeholder.unparsed("rank", "1"),
            Placeholder.unparsed("online", "3"),
            Placeholder.unparsed("id", "7"),
            Placeholder.styling("team_color"),
            LanguageManager.count("seconds", 2),
            Placeholder.unparsed("items", "2"));

    @TempDir
    Path languageFolder;

    private static InputStream bundled(String language) {
        return LanguageManagerTest.class.getClassLoader().getResourceAsStream("lang/" + language + ".yml");
    }

    private LanguageManager create(String defaultLanguage, boolean perPlayer) {
        return new LanguageManager(this.languageFolder, LanguageManagerTest::bundled, defaultLanguage, perPlayer, LOGGER);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @ParameterizedTest
    @EnumSource(Message.class)
    void everyMessageExistsInEveryBundledLanguage(Message message) throws IOException {
        for (String language : LanguageManager.BUNDLED_LANGUAGES) {
            try (InputStream inputStream = bundled(language)) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                org.bukkit.configuration.file.YamlConfiguration configuration = new org.bukkit.configuration.file.YamlConfiguration();
                try {
                    configuration.loadFromString(content);
                } catch (org.bukkit.configuration.InvalidConfigurationException exception) {
                    throw new AssertionError(language + ".yml is not valid YAML", exception);
                }
                assertTrue(configuration.isString(message.key()), language + ".yml has no message " + message.key());
            }
        }
    }

    @Test
    void bundledLanguagesHaveNoUnknownKeys() throws Exception {
        List<String> known = new ArrayList<>();
        for (Message message : Message.values()) known.add(message.key());

        for (String language : LanguageManager.BUNDLED_LANGUAGES) {
            org.bukkit.configuration.file.YamlConfiguration configuration = new org.bukkit.configuration.file.YamlConfiguration();
            try (InputStream inputStream = bundled(language)) {
                configuration.loadFromString(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
            for (String key : configuration.getKeys(true)) {
                if (configuration.isConfigurationSection(key)) continue;
                assertTrue(known.contains(key), language + ".yml contains " + key + ", which is no Message");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Message.class)
    void everyMessageRendersWithoutLeftoverTags(Message message) {
        LanguageManager languageManager = this.create("en", true);
        for (String language : LanguageManager.BUNDLED_LANGUAGES) {
            String text = plain(languageManager.render(Locale.forLanguageTag(language), message, ALL_PLACEHOLDERS));
            if (message == Message.COMMAND_START_USAGE) {
                // Escaped on purpose: /start <duration> <jokers>
                text = text.replaceAll("<[^<>]+>", "");
            }
            assertFalse(text.contains("<") || text.contains(">"), language + " " + message.key() + " renders as: " + text);
        }
    }

    @Test
    void copiesBundledLanguagesIntoTheFolder() {
        this.create("en", true);
        assertTrue(Files.exists(this.languageFolder.resolve("en.yml")));
        assertTrue(Files.exists(this.languageFolder.resolve("de.yml")));
    }

    @Test
    void usesTheClientLanguageOfThePlayer() {
        LanguageManager languageManager = this.create("en", true);

        assertEquals("de", languageManager.languageFor(Locale.GERMANY));
        assertEquals("de", languageManager.languageFor(Locale.forLanguageTag("de-AT")));
        assertEquals("en", languageManager.languageFor(Locale.US));
        assertEquals("en", languageManager.languageFor(Locale.JAPAN), "no file -> default language");
        assertEquals("en", languageManager.languageFor(null), "console -> default language");
        assertEquals("Waiting for the game to start...", plain(languageManager.render(Locale.US, Message.LOBBY_BOSSBAR)));
        assertEquals("Warte auf Spielstart...", plain(languageManager.render(Locale.GERMANY, Message.LOBBY_BOSSBAR)));
    }

    @Test
    void perPlayerOffUsesTheDefaultLanguageForEverybody() {
        LanguageManager languageManager = this.create("de", false);

        assertEquals("de", languageManager.languageFor(Locale.US));
        assertEquals("Warte auf Spielstart...", plain(languageManager.render(Locale.US, Message.LOBBY_BOSSBAR)));
    }

    @Test
    void detectsAdditionalLanguagesAndFillsMissingKeys() throws IOException {
        Files.writeString(this.languageFolder.resolve("fr.yml"), "lobby:\n  bossbar: \"En attente du début...\"\n");
        Files.writeString(this.languageFolder.resolve("pt.yml"), "lobby:\n  bossbar: \"pt\"\n");
        Files.writeString(this.languageFolder.resolve("pt_br.yml"), "lobby:\n  bossbar: \"pt_br\"\n");
        Files.writeString(this.languageFolder.resolve("not a language.yml"), "lobby:\n  bossbar: \"x\"\n");
        Files.writeString(this.languageFolder.resolve("broken.yml"), "lobby: [\n");

        LanguageManager languageManager = this.create("de", true);

        assertEquals(List.of("de", "en", "fr", "pt", "pt_br"), List.copyOf(languageManager.getLanguages()));
        assertEquals("fr", languageManager.languageFor(Locale.FRANCE));
        assertEquals("pt_br", languageManager.languageFor(Locale.forLanguageTag("pt-BR")), "exact match first");
        assertEquals("pt", languageManager.languageFor(Locale.forLanguageTag("pt-PT")));

        assertEquals("En attente du début...", plain(languageManager.render(Locale.FRANCE, Message.LOBBY_BOSSBAR)));
        // Missing in fr.yml -> default language (de)
        assertEquals("Einstellungen", plain(languageManager.render(Locale.FRANCE, Message.SETTINGS_GUI_TITLE)));
    }

    @Test
    void editedFilesKeepBundledMessagesTheyDoNotContain() throws IOException {
        Files.writeString(this.languageFolder.resolve("en.yml"), "lobby:\n  bossbar: \"Custom\"\n");

        LanguageManager languageManager = this.create("en", true);

        assertEquals("Custom", plain(languageManager.render(Locale.US, Message.LOBBY_BOSSBAR)));
        assertEquals("Settings", plain(languageManager.render(Locale.US, Message.SETTINGS_GUI_TITLE)));
    }

    @Test
    void unknownDefaultLanguageFallsBackToEnglish() {
        LanguageManager languageManager = this.create("xx", false);

        assertEquals("en", languageManager.getDefaultLanguage());
    }

    @Test
    void pluralChoosesTheTextByTheNumber() {
        LanguageManager languageManager = this.create("en", true);

        assertEquals("The game starts in 1 second",
                plain(languageManager.render(Locale.US, Message.STARTING_ACTIONBAR, LanguageManager.count("seconds", 1))));
        assertEquals("The game starts in 3 seconds",
                plain(languageManager.render(Locale.US, Message.STARTING_ACTIONBAR, LanguageManager.count("seconds", 3))));
        assertEquals("Das Spiel startet in 1 Sekunde",
                plain(languageManager.render(Locale.GERMANY, Message.STARTING_ACTIONBAR, LanguageManager.count("seconds", 1))));
    }

    @Test
    void prefixIsAvailableInEveryMessage() {
        LanguageManager languageManager = this.create("en", true);

        assertEquals("ForceItemBattle ● This team is full", plain(languageManager.render(Locale.US, Message.TEAM_FULL)));
    }

    @Test
    void translatorRendersSharedComponentsPerLocale() {
        LanguageManager languageManager = this.create("en", true);

        Component german = languageManager.translator().translate(
                (net.kyori.adventure.text.TranslatableComponent) Message.BACKPACK_TITLE.translatable(), Locale.GERMANY);
        assertEquals("Backpack", plain(german));
        Component english = languageManager.translator().translate(
                (net.kyori.adventure.text.TranslatableComponent) Message.LOBBY_BOSSBAR.translatable(), Locale.UK);
        assertEquals("Waiting for the game to start...", plain(english));
    }

    @Test
    void descriptionsAreSplitIntoLoreLines() {
        LanguageManager languageManager = this.create("en", true);

        List<String> lines = languageManager.renderLines(Locale.US, Message.SETTINGS_STARTER_TOOLS_DESCRIPTION).stream()
                .map(LanguageManagerTest::plain)
                .toList();
        assertEquals(2, lines.size(), lines.toString());
    }
}
