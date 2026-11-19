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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * End-to-end test against a real Paper server with the plugin, started by {@code tools/smoke-test/e2e_test.py}
 * ({@code -Dfib.test=e2e}):
 * <ol>
 *     <li>joins the server with its client language ({@code fib.e2e.language}) and accepts the resource pack
 *     (served by the script over local HTTP)</li>
 *     <li>waits until the pack is loaded, takes a screenshot of the lobby (scoreboard title glyphs), joins the first
 *     team with the team selector and checks that the lobby texts (team boss bar, team selector item) are in its
 *     language - both clients share the boss bar of the team</li>
 *     <li>enables the setting "Starter tools" in the settings GUI and reports ready (marker file) - the script
 *     starts a game from the server console once both clients are ready</li>
 *     <li>waits for the boss bar of the game, checks that it contains an item icon sprite that exists in the loaded
 *     atlases (not the "missing texture") and that the starter tools are in the inventory, takes screenshots and
 *     disconnects</li>
 * </ol>
 *
 * <p>A game needs two players, so the script runs a second client ({@code -Dfib.e2e.role=secondary}, German): it
 * joins as well, checks its lobby texts, reports ready, waits for the boss bar and leaves once the primary client
 * has finished its checks (marker file)
 */
public final class ServerEndToEndGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("fib-e2e-test");

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int SECONDS = 20;

    /** Any icon of the pack - present as soon as the pack is loaded */
    private static final Identifier PROBE_SPRITE = Identifier.fromNamespaceAndPath("fib", "icon/diamond");
    /** Written by the primary client when it is done, the secondary client disconnects then */
    private static final String DONE_MARKER = "primary-done";

    /** Expected lobby texts of the plugin per client language: boss bar, team selector item */
    private static final Map<String, List<String>> LOBBY_TEXTS = Map.of(
            "en_us", List.of("Waiting for the game to start...", "Choose your team"),
            "de_de", List.of("Warte auf Spielstart...", "Wähle dein Team"));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!ClientTests.isSelected("e2e")) return;

        String address = System.getProperty("fib.e2e.server", "");
        if (address.isBlank()) {
            throw new IllegalStateException("System property fib.e2e.server is missing - use tools/smoke-test/e2e_test.py");
        }
        Path output = Path.of(System.getProperty("fib.e2e.output", "e2e-test"));
        boolean primary = !"secondary".equals(System.getProperty("fib.e2e.role", "primary"));
        String role = primary ? "primary" : "secondary";
        if (primary) {
            deleteIfExists(output.resolve(DONE_MARKER));
        }

        String language = System.getProperty("fib.e2e.language", "en_us");
        List<String> expectedLobbyTexts = LOBBY_TEXTS.get(language);
        if (expectedLobbyTexts == null) {
            throw new IllegalStateException("No expected lobby texts for language " + language);
        }

        ClientTests.applyWindow(context, WIDTH, HEIGHT, 2);
        context.runOnClient(minecraft -> {
            // A local run opens a real window - keep it quiet
            minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            // Only the language sent to the server matters (client information) - no resource reload needed
            minecraft.options.languageCode = language;
        });

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

        // Both clients join Team #1: they share one boss bar object, which still has to show each client's language
        joinFirstTeam(context);

        // Rendered by the server in the language of this client: the boss bar is shared by the team (translatable
        // component), the item name is rendered per player
        LOGGER.info("Checking the lobby texts ({})", language);
        waitFor(context, minecraft -> {
            List<String> bossBars = bossBarNames(minecraft).stream().map(Component::getString).toList();
            String teamSelector = minecraft.player.getInventory().getItem(0).getHoverName().getString();
            return bossBars.contains(expectedLobbyTexts.get(0)) && teamSelector.equals(expectedLobbyTexts.get(1));
        }, 30 * SECONDS, minecraft -> "Lobby texts are not in " + language + ": boss bars "
                + bossBarNames(minecraft).stream().map(Component::getString).toList() + ", team selector '"
                + minecraft.player.getInventory().getItem(0).getHoverName().getString() + "', expected " + expectedLobbyTexts);

        if (primary) {
            enableStarterTools(context);
        }
        writeMarker(output.resolve(role + "-ready"));

        LOGGER.info("Lobby checked, waiting for the game to start (boss bar with an item icon)");
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
            for (Item tool : new Item[]{Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE}) {
                if (!hasStarterTool(minecraft, tool)) {
                    result.add("No unbreakable " + tool + " with Efficiency I in the inventory (setting \"Starter tools\")");
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

    /**
     * Opens the team selector (hotbar slot 1) and joins the first team - the team boss bar appears
     */
    private static void joinFirstTeam(ClientGameTestContext context) {
        LOGGER.info("Joining the first team");
        context.runOnClient(minecraft -> {
            minecraft.player.getInventory().setSelectedSlot(0);
            minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
        });
        waitFor(context, minecraft -> guiSlot(minecraft, Items.LEATHER_BOOTS) != null, 30 * SECONDS,
                minecraft -> "The team selector did not open");

        context.runOnClient(minecraft -> {
            Slot slot = guiSlot(minecraft, Items.LEATHER_BOOTS);
            minecraft.gameMode.handleContainerInput(minecraft.player.containerMenu.containerId, slot.index, 0,
                    ContainerInput.PICKUP, minecraft.player);
        });
        // Joining closes the selector and shows the team boss bar
        waitFor(context, minecraft -> minecraft.player.containerMenu == minecraft.player.inventoryMenu
                        && !bossBarNames(minecraft).isEmpty(), 30 * SECONDS,
                minecraft -> "Joining the first team did not show a boss bar");
    }

    /**
     * Opens the settings GUI with the lobby settings item (hotbar slot 2) and switches "Starter tools" on
     */
    private static void enableStarterTools(ClientGameTestContext context) {
        LOGGER.info("Enabling the setting \"Starter tools\"");
        context.runOnClient(minecraft -> {
            minecraft.player.getInventory().setSelectedSlot(1);
            minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
        });
        waitFor(context, minecraft -> starterToolsSlot(minecraft) != null, 30 * SECONDS,
                minecraft -> "The settings GUI with the starter tools icon did not open");

        context.runOnClient(minecraft -> {
            AbstractContainerMenu menu = minecraft.player.containerMenu;
            Slot slot = starterToolsSlot(minecraft);
            minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, minecraft.player);
        });
        // An enabled setting glows
        waitFor(context, minecraft -> {
            Slot slot = starterToolsSlot(minecraft);
            return slot != null && Boolean.TRUE.equals(slot.getItem().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        }, 30 * SECONDS, minecraft -> "Clicking the starter tools icon did not enable the setting");

        context.runOnClient(minecraft -> {
            minecraft.player.closeContainer();
            minecraft.player.getInventory().setSelectedSlot(0);
        });
        context.waitTicks(5);
    }

    /** The starter tools icon in the opened settings GUI, or {@code null} */
    private static Slot starterToolsSlot(Minecraft minecraft) {
        return guiSlot(minecraft, Items.DIAMOND_PICKAXE);
    }

    /** The first slot with the item in the opened plugin GUI (not in the player inventory part), or {@code null} */
    private static Slot guiSlot(Minecraft minecraft, Item item) {
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (menu == minecraft.player.inventoryMenu) return null;
        return menu.slots.stream()
                .filter(slot -> slot.container != minecraft.player.getInventory())
                .filter(slot -> slot.getItem().is(item))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasStarterTool(Minecraft minecraft, Item tool) {
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(tool) || !stack.has(DataComponents.UNBREAKABLE)) continue;

            ItemEnchantments enchantments = stack.getEnchantments();
            boolean efficiencyOne = enchantments.keySet().stream()
                    .anyMatch(enchantment -> enchantment.is(Enchantments.EFFICIENCY) && enchantments.getLevel(enchantment) == 1);
            if (efficiencyOne) return true;
        }
        return false;
    }

    /**
     * {@link ClientGameTestContext#waitFor(Predicate, int)} with a readable message if the condition is not met
     */
    private static void waitFor(ClientGameTestContext context, Predicate<Minecraft> condition, int ticks,
                                Function<Minecraft, String> failure) {
        try {
            context.waitFor(condition, ticks);
        } catch (AssertionError | RuntimeException exception) {
            String message = context.computeOnClient(failure::apply);
            throw new AssertionError(message, exception);
        }
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

    /** The boss bars are not exposed by the overlay - read its (unobfuscated) field */
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
