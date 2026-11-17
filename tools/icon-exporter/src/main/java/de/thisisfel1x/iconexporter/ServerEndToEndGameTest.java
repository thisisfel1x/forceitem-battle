package de.thisisfel1x.iconexporter;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * End-to-end test against a real Paper server with the plugin, started by {@code tools/smoke-test/e2e_test.py}
 * ({@code -Dfib.test=e2e}):
 * <ol>
 *     <li>joins the server and accepts the resource pack (served by the script over local HTTP)</li>
 *     <li>waits until the pack is loaded and takes a screenshot of the lobby (scoreboard title glyphs)</li>
 *     <li>the script starts a game from the server console - waits for the boss bar of the plugin</li>
 *     <li>checks that the boss bar contains an item icon sprite and that the sprite exists in the loaded
 *     atlases (not the "missing texture"), takes screenshots and disconnects</li>
 * </ol>
 *
 * <p>A game needs two players, so the script runs a second client ({@code -Dfib.e2e.role=secondary}): it joins
 * as well, waits for the boss bar and leaves once the primary client has finished its checks (marker file).
 */
public final class ServerEndToEndGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("fib-e2e-test");

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int SECONDS = 20;

    /** Any icon of the pack - present as soon as the pack is loaded. */
    private static final Identifier PROBE_SPRITE = Identifier.fromNamespaceAndPath("fib", "icon/diamond");
    /** Written by the primary client when it is done, the secondary client disconnects then. */
    private static final String DONE_MARKER = "primary-done";

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!ClientTests.isSelected("e2e")) return;

        String address = System.getProperty("fib.e2e.server", "");
        if (address.isBlank()) {
            throw new IllegalStateException("System property fib.e2e.server is missing - use tools/smoke-test/e2e_test.py");
        }
        Path output = Path.of(System.getProperty("fib.e2e.output", "e2e-test"));
        boolean primary = !"secondary".equals(System.getProperty("fib.e2e.role", "primary"));
        if (primary) {
            deleteIfExists(output.resolve(DONE_MARKER));
        }

        ClientTests.applyWindow(context, WIDTH, HEIGHT, 2);
        // A local run opens a real window - keep it quiet
        context.runOnClient(minecraft -> minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));

        LOGGER.info("Connecting to {}", address);
        context.runOnClient(minecraft -> {
            ServerData server = new ServerData("ForceItemBattle E2E", address, ServerData.Type.OTHER);
            // Accept the server resource pack without the prompt
            server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            ConnectScreen.startConnecting(minecraft.gui.screen(), minecraft, ServerAddress.parseString(address), server, false, null);
        });

        context.waitFor(minecraft -> {
            if (minecraft.gui.screen() instanceof DisconnectedScreen screen) {
                throw new AssertionError("Disconnected from the server: " + screen.getTitle().getString()
                        + " - see the server log for the reason");
            }
            return minecraft.player != null && minecraft.level != null;
        }, 120 * SECONDS);
        LOGGER.info("Joined the server, waiting for the resource pack");
        context.waitFor(minecraft -> minecraft.gui.overlay() == null && isSpriteLoaded(minecraft, PROBE_SPRITE), 180 * SECONDS);
        context.waitTicks(2 * SECONDS);
        if (primary) {
            screenshot(context, output, "lobby");
        }

        // The lobby already has a boss bar ("Warte auf Spielstart...") - the game shows the item to find with its icon
        LOGGER.info("Resource pack loaded, waiting for the game to start (boss bar with an item icon)");
        context.waitFor(minecraft -> !bossBarSprites(minecraft).isEmpty(), 180 * SECONDS);
        context.waitTicks(2 * SECONDS);

        if (!primary) {
            LOGGER.info("Game started, waiting for the primary client to finish");
            context.waitFor(minecraft -> Files.exists(output.resolve(DONE_MARKER)), 300 * SECONDS);
            disconnect(context);
            LOGGER.info("End-to-end test passed (secondary client)");
            return;
        }

        List<String> problems = context.computeOnClient(minecraft -> {
            List<String> result = new ArrayList<>();
            List<Component> names = bossBarNames(minecraft);
            List<Identifier> sprites = bossBarSprites(minecraft);
            LOGGER.info("Boss bars: {} - sprites: {}", names.stream().map(Component::getString).toList(), sprites);

            if (sprites.isEmpty()) {
                result.add("No boss bar contains an item icon sprite: " + names.stream().map(Component::getString).toList());
            }
            for (Identifier sprite : sprites) {
                if (!isSpriteLoaded(minecraft, sprite)) {
                    result.add("Sprite " + sprite + " of the boss bar is not in the resource pack (missing texture)");
                }
            }
            return result;
        });

        for (int guiScale : new int[]{2, 3}) {
            ClientTests.applyWindow(context, WIDTH, HEIGHT, guiScale);
            context.waitTicks(5);
            screenshot(context, output, "bossbar-gui" + guiScale);
        }

        writeMarker(output.resolve(DONE_MARKER));
        disconnect(context);

        if (!problems.isEmpty()) {
            throw new AssertionError(String.join("\n", problems));
        }
        LOGGER.info("End-to-end test passed (primary client)");
    }

    private static void disconnect(ClientGameTestContext context) {
        context.runOnClient(minecraft -> minecraft.disconnectFromWorld(Component.literal("End-to-end test finished")));
        context.waitFor(minecraft -> minecraft.level == null, 60 * SECONDS);
        // Client gametests have to end on the title screen (disconnecting opens the multiplayer screen)
        context.setScreen(TitleScreen::new);
        context.waitTicks(SECONDS);
    }

    private static List<Identifier> bossBarSprites(Minecraft minecraft) {
        List<Identifier> sprites = new ArrayList<>();
        bossBarNames(minecraft).forEach(name -> collectSprites(name, sprites));
        return sprites;
    }

    private static void writeMarker(Path marker) {
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "done\n");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void screenshot(ClientGameTestContext context, Path output, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name)
                .withSize(WIDTH, HEIGHT)
                .withDestinationDir(output)
                .disableCounterPrefix());
    }

    /**
     * @return {@code true} if one of the loaded atlases contains the sprite (an unknown sprite resolves to the
     * missing texture)
     */
    private static boolean isSpriteLoaded(Minecraft minecraft, Identifier sprite) {
        boolean[] found = {false};
        minecraft.getAtlasManager().forEach((id, atlas) -> {
            TextureAtlasSprite atlasSprite = atlas.getSprite(sprite);
            if (atlasSprite != null && !atlasSprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static void collectSprites(Component component, List<Identifier> sprites) {
        if (component.getContents() instanceof ObjectContents objectContents
                && objectContents.contents() instanceof AtlasSprite atlasSprite) {
            sprites.add(atlasSprite.sprite());
        }
        component.getSiblings().forEach(sibling -> collectSprites(sibling, sprites));
    }

    /** The boss bars are not exposed by the overlay - read its (unobfuscated) field. */
    private static List<Component> bossBarNames(Minecraft minecraft) {
        try {
            Field field = BossHealthOverlay.class.getDeclaredField("events");
            field.setAccessible(true);
            Map<?, ?> events = (Map<?, ?>) field.get(minecraft.gui.hud.getBossOverlay());
            return events.values().stream().map(event -> ((BossEvent) event).getName()).toList();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read the boss bars", exception);
        }
    }
}
