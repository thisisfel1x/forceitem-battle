package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.world.BiomeSize;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForceItemBattleBootstrapTest {

    private static final ComponentLogger LOGGER = ComponentLogger.logger("test");

    @TempDir
    Path directory;

    @Test
    void firstStartUsesTheDefaultOfTheBundledConfig() {
        assertEquals(BiomeSize.SMALL, ForceItemBattleBootstrap.readBiomeSize(this.directory.resolve("config.yml"), LOGGER));
    }

    @Test
    void configOfAnOlderVersionKeepsTheVanillaSize() throws IOException {
        Path config = this.directory.resolve("config.yml");
        Files.writeString(config, "teams:\n  amount: 14\n");

        assertEquals(BiomeSize.NORMAL, ForceItemBattleBootstrap.readBiomeSize(config, LOGGER));
    }

    @Test
    void readsTheConfiguredSize() throws IOException {
        Path config = this.directory.resolve("config.yml");
        Files.writeString(config, "world:\n  biome-size: tiny\n");

        assertEquals(BiomeSize.TINY, ForceItemBattleBootstrap.readBiomeSize(config, LOGGER));
    }

    @Test
    void invalidValuesKeepTheVanillaSize() throws IOException {
        Path config = this.directory.resolve("config.yml");
        Files.writeString(config, "world:\n  biome-size: huge\n");
        assertEquals(BiomeSize.NORMAL, ForceItemBattleBootstrap.readBiomeSize(config, LOGGER));

        Files.writeString(config, "world: [\n");
        assertEquals(BiomeSize.NORMAL, ForceItemBattleBootstrap.readBiomeSize(config, LOGGER));
    }
}
