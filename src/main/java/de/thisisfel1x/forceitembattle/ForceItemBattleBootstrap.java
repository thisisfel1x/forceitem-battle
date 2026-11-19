package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.world.BiomeSize;
import de.thisisfel1x.forceitembattle.world.BiomeSizeDatapack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs before the server loads its data packs and worlds - the only point where world generation can still be
 * changed. Registers a data pack with smaller biomes if {@code world.biome-size} asks for it
 */
@SuppressWarnings("UnstableApiUsage")
public final class ForceItemBattleBootstrap implements PluginBootstrap {

    static final String BIOME_SIZE_PATH = "world.biome-size";
    private static final String DATAPACK_ID = "biome_size";

    @Override
    public void bootstrap(BootstrapContext context) {
        ComponentLogger logger = context.getLogger();
        Path dataDirectory = context.getDataDirectory();
        BiomeSize biomeSize = readBiomeSize(dataDirectory.resolve("config.yml"), logger);
        Path packDirectory = dataDirectory.resolve("datapacks").resolve(DATAPACK_ID);
        // Discovery runs again on every data pack reload (/reload, /datapack ...) - log only once
        AtomicBoolean logged = new AtomicBoolean();

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                if (biomeSize.isVanilla()) {
                    BiomeSizeDatapack.delete(packDirectory);
                    return;
                }

                BiomeSizeDatapack.write(packDirectory, biomeSize);
                event.registrar().discoverPack(packDirectory, DATAPACK_ID, configurer -> configurer
                        .title(Component.text("ForceItemBattle biome size"))
                        .autoEnableOnServerStart(true));
                if (logged.compareAndSet(false, true)) {
                    logger.info("Biome size {}: newly generated chunks get smaller biomes (data pack {})",
                            biomeSize, packDirectory);
                }
            } catch (IOException exception) {
                logger.error("Could not register the biome size data pack, biomes keep their normal size", exception);
            }
        });
    }

    /**
     * Reads the biome size from the config. On the very first start there is no config yet: the default of the
     * bundled config applies. An existing config without the option (created by an older version) keeps the
     * vanilla size, so an existing world does not suddenly get differently sized biomes in new chunks
     */
    static BiomeSize readBiomeSize(Path configFile, ComponentLogger logger) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            if (Files.exists(configFile)) {
                config.load(configFile.toFile());
            } else {
                try (InputStream inputStream = ForceItemBattleBootstrap.class.getClassLoader().getResourceAsStream("config.yml")) {
                    if (inputStream == null) return BiomeSize.NORMAL;
                    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        config.load(reader);
                    }
                }
            }
        } catch (IOException | InvalidConfigurationException exception) {
            logger.warn("Could not read {} ({}), biomes keep their normal size", configFile, exception.getMessage());
            return BiomeSize.NORMAL;
        }

        String value = config.getString(BIOME_SIZE_PATH, BiomeSize.NORMAL.name());
        return BiomeSize.parse(value).orElseGet(() -> {
            logger.warn("{}: unknown value '{}' (NORMAL, SMALL, TINY), biomes keep their normal size", BIOME_SIZE_PATH, value);
            return BiomeSize.NORMAL;
        });
    }
}
