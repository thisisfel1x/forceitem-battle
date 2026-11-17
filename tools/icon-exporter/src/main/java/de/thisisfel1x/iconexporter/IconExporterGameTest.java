package de.thisisfel1x.iconexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Renders every item of the running Minecraft version with the game's own GUI item renderer and
 * stores it as a transparent PNG (one file per item id). Runs as a Fabric client gametest, so it
 * works headless in CI (xvfb) and needs no installed Minecraft.
 *
 * <p>Transparency: every sheet is rendered twice, on black and on white. For a pixel that was
 * alpha-blended onto the background, {@code black = a*c} and {@code white = a*c + (1-a)*255},
 * so {@code a = 1 - (white - black) / 255} and {@code c = black / a}.
 *
 * <p>Afterwards a preview resource pack is loaded and screenshots compare the two ways of showing
 * an icon in text: sprite object components (gui atlas) and bitmap font glyphs.
 */
public final class IconExporterGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("fib-icon-exporter");

    /** Size of one sheet cell in GUI units: the 16 unit item plus a margin of 1 on every side. */
    private static final int CELL = 18;
    private static final int SHEET_COLUMNS = 32;
    private static final int SHEET_ROWS = 16;

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private static final int PREVIEW_WIDTH = 1920;
    private static final int PREVIEW_HEIGHT = 1080;
    private static final String PREVIEW_PACK = "fib-icon-preview";

    /** Items shown in the preview: 3D models, tinted and flat items and some of the 26.x additions. */
    private static final List<String> PREVIEW_ITEMS = List.of(
            "red_bed", "straw_bed", "chest", "oak_stairs", "white_banner", "shield", "grass_block",
            "glass", "diamond", "potion", "leather_chestplate", "compass", "oak_sapling",
            "red_cushion", "poplar_log", "cinnabar_bricks", "sulfur_spike", "plains_village_map"
    );
    private static final List<String> BOSSBAR_ITEMS = List.of("red_bed", "oak_stairs", "diamond");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!ClientTests.isSelected("icons")) return;

        ExporterConfig config = ExporterConfig.fromSystemProperties();
        LOGGER.info("Exporting item icons to {}", config.iconDir());

        // Since 26.x the item components come from the (built-in) datapack and are only bound while a
        // world is loaded - ItemStacks cannot be created on the title screen
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();

            List<String> itemIds = this.exportIcons(context, singleplayer, config);
            this.writeManifest(config, itemIds);
            LOGGER.info("Exported {} item icons", itemIds.size());

            this.renderPreview(context, singleplayer, config, itemIds);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------------------------------

    private List<String> exportIcons(ClientGameTestContext context, TestSingleplayerContext singleplayer, ExporterConfig config) {
        List<Item> items = context.computeOnClient(minecraft -> BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList());

        prepareEmptyIconDir(config.iconDir());
        freezeTextureAnimations(context, singleplayer);

        int scale = config.scale();
        int iconSize = 16 * scale;
        int perSheet = SHEET_COLUMNS * SHEET_ROWS;
        int width = SHEET_COLUMNS * CELL * scale;
        int height = SHEET_ROWS * CELL * scale;

        ClientTests.applyWindow(context, width, height, scale);

        // Screens look up Minecraft.getInstance() in their constructor and must be created on the client thread
        IconSheetScreen screen = context.computeOnClient(minecraft -> new IconSheetScreen(CELL));
        context.setScreen(() -> screen);
        context.waitTicks(2);

        List<String> exported = new ArrayList<>();
        for (int sheet = 0; sheet * perSheet < items.size(); sheet++) {
            List<Item> sheetItems = items.subList(sheet * perSheet, Math.min(items.size(), (sheet + 1) * perSheet));
            List<ItemStack> stacks = context.computeOnClient(minecraft -> sheetItems.stream().map(item -> {
                ItemStack stack = new ItemStack(item);
                // The glint is animated and would differ between the black and the white render
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                return stack;
            }).toList());

            Path blackSheet = this.renderSheet(context, config, screen, stacks, BLACK, "sheet-" + sheet + "-black", width, height);
            Path whiteSheet = this.renderSheet(context, config, screen, stacks, WHITE, "sheet-" + sheet + "-white", width, height);

            try (NativeImage black = readImage(blackSheet); NativeImage white = readImage(whiteSheet)) {
                for (int i = 0; i < sheetItems.size(); i++) {
                    int x = ((i % SHEET_COLUMNS) * CELL + 1) * scale;
                    int y = ((i / SHEET_COLUMNS) * CELL + 1) * scale;
                    String id = BuiltInRegistries.ITEM.getKey(sheetItems.get(i)).getPath();

                    try (NativeImage icon = IconImages.extract(black, white, x, y, iconSize)) {
                        icon.writeToFile(config.iconDir().resolve(id + ".png"));
                    }
                    exported.add(id);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            LOGGER.info("Sheet {} done ({} icons so far)", sheet, exported.size());
        }

        context.setScreen(() -> null);
        return exported;
    }

    /**
     * Animated textures (magma, prismarine, command blocks, ...) must show the same frame in the black and the white
     * render, otherwise the transparency is computed wrong - and the same frame in every export, otherwise each
     * export changes these icons. Freezing the world stops the animations (they only tick while the level runs
     * normally), then every animation is set back to its first frame.
     */
    private static void freezeTextureAnimations(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("tick freeze");
        context.waitFor(minecraft -> minecraft.level != null && !minecraft.level.tickRateManager().runsNormally(), 10 * 20);

        context.runOnClient(minecraft -> {
            try {
                Field states = TextureAtlas.class.getDeclaredField("animatedTexturesStates");
                Field frame = SpriteContents.AnimationState.class.getDeclaredField("frame");
                Field subFrame = SpriteContents.AnimationState.class.getDeclaredField("subFrame");
                Field dirty = SpriteContents.AnimationState.class.getDeclaredField("isDirty");
                Method upload = TextureAtlas.class.getDeclaredMethod("uploadAnimationFrames");
                for (AccessibleObject member : List.of(states, frame, subFrame, dirty, upload)) {
                    member.setAccessible(true);
                }

                List<TextureAtlas> atlases = new ArrayList<>();
                minecraft.getAtlasManager().forEach((id, atlas) -> atlases.add(atlas));
                for (TextureAtlas atlas : atlases) {
                    for (Object state : (List<?>) states.get(atlas)) {
                        frame.setInt(state, 0);
                        subFrame.setInt(state, 0);
                        dirty.setBoolean(state, true);
                    }
                    upload.invoke(atlas);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Internals of this Minecraft version differ - the freeze alone keeps black and white consistent
                LOGGER.warn("Could not reset the texture animations to their first frame, animated icons may differ "
                        + "between exports: {}", exception.toString());
            }
        });
        context.waitTicks(2);
    }

    private Path renderSheet(ClientGameTestContext context, ExporterConfig config, IconSheetScreen screen,
                             List<ItemStack> stacks, int background, String name, int width, int height) {
        context.runOnClient(minecraft -> screen.show(stacks, background));
        context.waitTicks(2);
        return context.takeScreenshot(TestScreenshotOptions.of(name)
                .withSize(width, height)
                .withDestinationDir(config.previewDir().resolve("sheets"))
                .disableCounterPrefix());
    }

    private void writeManifest(ExporterConfig config, List<String> itemIds) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("minecraftVersion", SharedConstants.getCurrentVersion().name());
        manifest.addProperty("resourcePackFormat", currentPackFormat());
        manifest.addProperty("iconSize", 16 * config.scale());

        JsonArray items = new JsonArray();
        itemIds.forEach(items::add);
        manifest.add("items", items);

        writeString(config.manifestFile(), GSON.toJson(manifest) + "\n");
    }

    // ------------------------------------------------------------------------------------------
    // Preview: sprite object component vs. bitmap font glyph
    // ------------------------------------------------------------------------------------------

    private void renderPreview(ClientGameTestContext context, TestSingleplayerContext singleplayer, ExporterConfig config, List<String> exportedIds) {
        List<String> previewIds = PREVIEW_ITEMS.stream().filter(exportedIds::contains).toList();
        if (previewIds.isEmpty()) {
            LOGGER.warn("None of the preview items exists in this version, skipping the preview");
            return;
        }

        Path packDir = context.computeOnClient(minecraft -> minecraft.getResourcePackDirectory().resolve(PREVIEW_PACK));
        this.writePreviewPack(config, packDir, previewIds);
        this.enablePreviewPack(context);

        List<PreviewScreen.Row> rows = context.computeOnClient(minecraft -> previewIds.stream()
                .map(id -> new PreviewScreen.Row(spriteIcon(id), fontIcon(previewIds.indexOf(id)), itemName(id)))
                .toList());

        PreviewScreen previewScreen = context.computeOnClient(minecraft -> new PreviewScreen(rows, config.previewIconSize()));
        context.setScreen(() -> previewScreen);
        for (int guiScale : new int[]{2, 3}) {
            ClientTests.applyWindow(context, PREVIEW_WIDTH, PREVIEW_HEIGHT, guiScale);
            context.waitTicks(3);
            this.takePreviewScreenshot(context, config, "compare-gui" + guiScale);
        }
        context.setScreen(() -> null);

        this.renderBossBars(context, singleplayer, config, previewIds);
    }

    private void writePreviewPack(ExporterConfig config, Path packDir, List<String> previewIds) {
        deleteRecursively(packDir);

        JsonObject pack = new JsonObject();
        pack.addProperty("description", "ForceItemBattle icon preview");
        pack.addProperty("min_format", currentPackFormat());
        pack.addProperty("max_format", currentPackFormat());
        JsonObject mcmeta = new JsonObject();
        mcmeta.add("pack", pack);
        writeString(packDir.resolve("pack.mcmeta"), GSON.toJson(mcmeta));

        JsonArray providers = new JsonArray();
        for (int i = 0; i < previewIds.size(); i++) {
            String id = previewIds.get(i);
            Path target = packDir.resolve("assets/fib/textures/gui/sprites/icon/" + id + ".png");

            try (NativeImage source = readImage(config.iconDir().resolve(id + ".png"));
                 NativeImage scaled = IconImages.downscale(source, config.previewIconSize())) {
                Files.createDirectories(target.getParent());
                scaled.writeToFile(target);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }

            // Same metrics as the previous resource pack used for its item glyphs
            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", "fib:gui/sprites/icon/" + id + ".png");
            provider.addProperty("height", 9);
            provider.addProperty("ascent", 8);
            JsonArray chars = new JsonArray();
            chars.add(String.valueOf(glyph(i)));
            provider.add("chars", chars);
            providers.add(provider);
        }

        JsonObject font = new JsonObject();
        font.add("providers", providers);
        writeString(packDir.resolve("assets/fib/font/preview.json"), GSON.toJson(font));
    }

    private void enablePreviewPack(ClientGameTestContext context) {
        CompletableFuture<Void> reload = context.computeOnClient(minecraft -> {
            PackRepository repository = minecraft.getResourcePackRepository();
            repository.reload();
            if (!repository.addPack("file/" + PREVIEW_PACK)) {
                throw new IllegalStateException("Preview resource pack was not found: " + repository.getAvailableIds());
            }
            return minecraft.reloadResourcePacks();
        });
        // The loading overlay fades out after the reload, screenshots before that are covered by it
        context.waitFor(minecraft -> reload.isDone() && minecraft.gui.overlay() == null, 120 * 20);
        reload.join();
        context.waitTicks(5);
    }

    private void renderBossBars(ClientGameTestContext context, TestSingleplayerContext singleplayer, ExporterConfig config, List<String> previewIds) {
        List<Component> names = context.computeOnClient(minecraft -> {
            List<Component> result = new ArrayList<>();
            for (String id : BOSSBAR_ITEMS) {
                int index = previewIds.indexOf(id);
                if (index < 0) continue;
                result.add(Component.empty().append(spriteIcon(id)).append(" ").append(itemName(id))
                        .append(Component.literal("  [Sprite]").withColor(0xAAAAAA)));
                result.add(Component.empty().append(fontIcon(index)).append(" ").append(itemName(id))
                        .append(Component.literal("  [Font]").withColor(0xAAAAAA)));
            }
            return result;
        });

        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            for (Component name : names) {
                ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), name,
                        BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
                bossBar.addPlayer(player);
            }
        });
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);

        for (int guiScale : new int[]{2, 3}) {
            ClientTests.applyWindow(context, PREVIEW_WIDTH, PREVIEW_HEIGHT, guiScale);
            context.waitTicks(3);
            this.takePreviewScreenshot(context, config, "bossbar-gui" + guiScale);
        }
    }

    private void takePreviewScreenshot(ClientGameTestContext context, ExporterConfig config, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name)
                .withSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .withDestinationDir(config.previewDir())
                .disableCounterPrefix());
    }

    private static Component spriteIcon(String id) {
        return Component.object(new AtlasSprite(
                Identifier.withDefaultNamespace("gui"),
                Identifier.fromNamespaceAndPath("fib", "icon/" + id)));
    }

    private static Component fontIcon(int index) {
        FontDescription font = new FontDescription.Resource(Identifier.fromNamespaceAndPath("fib", "preview"));
        return Component.literal(String.valueOf(glyph(index))).withStyle(Style.EMPTY.withFont(font));
    }

    private static Component itemName(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
        return new ItemStack(item).getHoverName();
    }

    private static char glyph(int index) {
        return (char) (0xE000 + index);
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private static int currentPackFormat() {
        return SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).major();
    }

    private static NativeImage readImage(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return NativeImage.read(inputStream);
        }
    }

    private static void prepareEmptyIconDir(Path iconDir) {
        try {
            Files.createDirectories(iconDir);
            // Icons of items that no longer exist must not survive a version bump
            try (Stream<Path> files = Files.list(iconDir)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".png")).toList()) {
                    Files.delete(file);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void writeString(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) return;
        try (Stream<Path> files = Files.walk(path)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
