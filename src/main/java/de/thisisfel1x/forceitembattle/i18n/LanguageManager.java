package de.thisisfel1x.forceitembattle.i18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.translation.Translator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Loads the language files and renders {@link Message}s for a player in its client language
 *
 * <p>Every {@code <code>.yml} in the language folder is a language, e.g. {@code fr.yml} or {@code pt_br.yml}
 * (named after the Minecraft language codes). The bundled languages ({@value #FALLBACK_LANGUAGE}, de) are copied
 * there on first start and can be edited. Keys missing in a file are taken from the bundled version of the
 * language, then from the default language and finally from the bundled English file - a language file only
 * has to contain the messages it changes
 */
public final class LanguageManager {

    public static final List<String> BUNDLED_LANGUAGES = List.of("en", "de");
    public static final String FALLBACK_LANGUAGE = "en";

    private static final Pattern LANGUAGE_CODE = Pattern.compile("[a-z]{2,3}(_[a-z0-9]{2,8})?");
    private static final Pattern LINE_BREAK = Pattern.compile("<newline>|<br>|\\R");

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Logger logger;
    private final boolean perPlayer;
    private final String defaultLanguage;
    /** Language code -> files to look a key up in, most specific first */
    private final Map<String, List<YamlConfiguration>> lookupChains = new LinkedHashMap<>();
    private final Translator translator = new MessageTranslator();

    /**
     * @param languageFolder  folder with the language files (created if missing, bundled languages are copied)
     * @param bundledLanguage opens the bundled file of a language, {@code null} if it is not bundled
     * @param defaultLanguage language for the console and for players whose language has no file
     * @param perPlayer       {@code false}: every player gets the default language
     */
    public LanguageManager(Path languageFolder, Function<String, @Nullable InputStream> bundledLanguage,
                           String defaultLanguage, boolean perPlayer, Logger logger) {
        this.logger = logger;
        this.perPlayer = perPlayer;

        Map<String, YamlConfiguration> bundled = new LinkedHashMap<>();
        for (String code : BUNDLED_LANGUAGES) {
            YamlConfiguration configuration = this.loadBundled(code, bundledLanguage);
            if (configuration != null) {
                bundled.put(code, configuration);
            }
        }
        if (!bundled.containsKey(FALLBACK_LANGUAGE)) {
            throw new IllegalStateException("The bundled language file " + FALLBACK_LANGUAGE + ".yml is missing");
        }

        Map<String, YamlConfiguration> files = this.loadFolder(languageFolder, bundledLanguage);

        Set<String> codes = new TreeSet<>(bundled.keySet());
        codes.addAll(files.keySet());

        String requestedDefault = defaultLanguage.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (!codes.contains(requestedDefault)) {
            logger.warning("Default language '" + defaultLanguage + "' has no file in " + languageFolder
                    + ", using " + FALLBACK_LANGUAGE);
            requestedDefault = FALLBACK_LANGUAGE;
        }
        this.defaultLanguage = requestedDefault;

        for (String code : codes) {
            List<YamlConfiguration> chain = new ArrayList<>();
            addIfPresent(chain, files.get(code));
            addIfPresent(chain, bundled.get(code));
            addIfPresent(chain, files.get(this.defaultLanguage));
            addIfPresent(chain, bundled.get(this.defaultLanguage));
            addIfPresent(chain, bundled.get(FALLBACK_LANGUAGE));
            this.lookupChains.put(code, Collections.unmodifiableList(chain));
        }

        logger.info("Languages: " + String.join(", ", this.lookupChains.keySet()) + " (default: " + this.defaultLanguage
                + (perPlayer ? ", players get their client language" : ", for every player") + ")");
    }

    private static void addIfPresent(List<YamlConfiguration> chain, @Nullable YamlConfiguration configuration) {
        if (configuration != null && !chain.contains(configuration)) {
            chain.add(configuration);
        }
    }

    private @Nullable YamlConfiguration loadBundled(String code, Function<String, @Nullable InputStream> bundledLanguage) {
        try (InputStream inputStream = bundledLanguage.apply(code)) {
            if (inputStream == null) return null;
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                YamlConfiguration configuration = new YamlConfiguration();
                configuration.load(reader);
                return configuration;
            }
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Bundled language file " + code + ".yml is invalid", exception);
        }
    }

    /**
     * Copies the bundled languages into the folder (if not there yet) and loads every language file of it
     */
    private Map<String, YamlConfiguration> loadFolder(Path folder, Function<String, @Nullable InputStream> bundledLanguage) {
        Map<String, YamlConfiguration> files = new LinkedHashMap<>();
        try {
            Files.createDirectories(folder);
            for (String code : BUNDLED_LANGUAGES) {
                Path target = folder.resolve(code + ".yml");
                if (Files.exists(target)) continue;
                try (InputStream inputStream = bundledLanguage.apply(code)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, target);
                    }
                }
            }
        } catch (IOException exception) {
            this.logger.log(Level.WARNING, "Could not create the language folder " + folder, exception);
            return files;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.yml")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String code = fileName.substring(0, fileName.length() - ".yml".length()).toLowerCase(Locale.ROOT);
                if (!LANGUAGE_CODE.matcher(code).matches()) {
                    this.logger.warning("Ignoring language file " + fileName + ": the name has to be a language "
                            + "code like en.yml, de.yml or pt_br.yml");
                    continue;
                }

                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    YamlConfiguration configuration = new YamlConfiguration();
                    configuration.load(reader);
                    files.put(code, configuration);
                } catch (IOException | InvalidConfigurationException exception) {
                    this.logger.warning("Ignoring language file " + fileName + ", it is not valid YAML: "
                            + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            this.logger.log(Level.WARNING, "Could not read the language folder " + folder, exception);
        }
        return files;
    }

    /**
     * @return the language code used for the locale: the exact language ({@code pt_br}), the language without
     * country ({@code pt}) or the default language
     */
    public String languageFor(@Nullable Locale locale) {
        if (this.perPlayer && locale != null) {
            String full = locale.toString().toLowerCase(Locale.ROOT);
            if (this.lookupChains.containsKey(full)) return full;

            String language = locale.getLanguage().toLowerCase(Locale.ROOT);
            if (this.lookupChains.containsKey(language)) return language;
        }
        return this.defaultLanguage;
    }

    public Set<String> getLanguages() {
        return Collections.unmodifiableSet(this.lookupChains.keySet());
    }

    public String getDefaultLanguage() {
        return this.defaultLanguage;
    }

    /**
     * @return the raw MiniMessage string of the message in the given language
     */
    public String raw(String language, Message message) {
        List<YamlConfiguration> chain = this.lookupChains.getOrDefault(language, this.lookupChains.get(this.defaultLanguage));
        for (YamlConfiguration configuration : chain) {
            String value = configuration.getString(message.key());
            if (value != null) return value;
        }
        return message.key();
    }

    public Component render(@Nullable Locale locale, Message message, TagResolver... placeholders) {
        String language = this.languageFor(locale);
        return this.miniMessage.deserialize(this.raw(language, message), this.resolver(language, placeholders));
    }

    private TagResolver resolver(String language, TagResolver... placeholders) {
        Component prefix = this.miniMessage.deserialize(this.raw(language, Message.PREFIX));
        return TagResolver.builder()
                .resolvers(placeholders)
                .tag("prefix", Tag.selfClosingInserting(prefix))
                .build();
    }

    /**
     * Renders the message in the language of the player - the console gets the default language
     */
    public Component render(CommandSender receiver, Message message, TagResolver... placeholders) {
        return this.render(localeOf(receiver), message, placeholders);
    }

    /**
     * Renders every line of the message on its own (split at {@code <newline>}, {@code <br>} and line breaks),
     * e.g. for item lore. Formatting does not carry over to the next line
     */
    public List<Component> renderLines(CommandSender receiver, Message message, TagResolver... placeholders) {
        return this.renderLines(localeOf(receiver), message, placeholders);
    }

    public List<Component> renderLines(@Nullable Locale locale, Message message, TagResolver... placeholders) {
        String language = this.languageFor(locale);
        TagResolver resolver = this.resolver(language, placeholders);
        return LINE_BREAK.splitAsStream(this.raw(language, message))
                .map(line -> this.miniMessage.deserialize(line, resolver))
                .toList();
    }

    public void send(CommandSender receiver, Message message, TagResolver... placeholders) {
        receiver.sendMessage(this.render(receiver, message, placeholders));
    }

    public void send(Iterable<? extends CommandSender> receivers, Message message, TagResolver... placeholders) {
        for (CommandSender receiver : receivers) {
            this.send(receiver, message, placeholders);
        }
    }

    /**
     * Sends the message to every online player (each in its language) and to the console
     */
    public void broadcast(Message message, TagResolver... placeholders) {
        this.send(Bukkit.getOnlinePlayers(), message, placeholders);
        this.send(Bukkit.getConsoleSender(), message, placeholders);
    }

    private static @Nullable Locale localeOf(CommandSender receiver) {
        return receiver instanceof Player player ? player.locale() : null;
    }

    /**
     * Renders {@link Message#translatable()} components in the language of the receiving client. Register it at
     * {@code GlobalTranslator.translator()}
     */
    public Translator translator() {
        return this.translator;
    }

    /**
     * {@code <name>} with the number and {@code <plural:'1#item|1<items'>} choosing a text by it
     * (a {@link java.text.ChoiceFormat} pattern)
     */
    public static TagResolver count(String name, int number) {
        return TagResolver.resolver(
                Placeholder.unparsed(name, Integer.toString(number)),
                Formatter.choice("plural", number));
    }

    private final class MessageTranslator implements Translator {

        private final Key name = Key.key("forceitembattle", "messages");

        @Override
        public Key name() {
            return this.name;
        }

        @Override
        public @Nullable MessageFormat translate(String key, Locale locale) {
            return null;
        }

        @Override
        public @Nullable Component translate(TranslatableComponent component, Locale locale) {
            Message message = Message.byTranslationKey(component.key());
            if (message == null) return null;

            // Keep style and children of the translatable component (e.g. a color set by the caller)
            Component rendered = LanguageManager.this.render(locale, message).applyFallbackStyle(component.style());
            for (Component child : component.children()) {
                rendered = rendered.append(child);
            }
            return rendered;
        }
    }
}
