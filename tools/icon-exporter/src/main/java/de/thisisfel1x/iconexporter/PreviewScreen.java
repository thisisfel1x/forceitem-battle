package de.thisisfel1x.iconexporter;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Side by side comparison of the same icons as sprite object component and as font glyph
 */
final class PreviewScreen extends Screen {

    record Row(Component spriteIcon, Component fontIcon, Component name) {
    }

    private static final int BACKGROUND = 0xFF2B2B2B;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int LINE_HEIGHT = 11;

    private final List<Row> rows;
    private final int iconSize;

    PreviewScreen(List<Row> rows, int iconSize) {
        super(Component.literal("ForceItemBattle icon preview"));
        this.rows = List.copyOf(rows);
        this.iconSize = iconSize;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), BACKGROUND);

        int spriteColumn = 8;
        int fontColumn = graphics.guiWidth() / 2 + 8;
        int y = 8;

        graphics.text(this.font, Component.literal("Sprite object component (gui atlas)"), spriteColumn, y, TEXT, true);
        graphics.text(this.font, Component.literal("Bitmap font glyph (height 9, ascent 8)"), fontColumn, y, TEXT, true);
        y += LINE_HEIGHT;
        graphics.text(this.font, Component.literal("Icon texture: " + this.iconSize + "x" + this.iconSize + " px"), spriteColumn, y, MUTED, true);
        y += LINE_HEIGHT + 6;

        for (Row row : this.rows) {
            graphics.text(this.font, Component.empty().append(row.spriteIcon()).append(" ").append(row.name()), spriteColumn, y, TEXT, true);
            graphics.text(this.font, Component.empty().append(row.fontIcon()).append(" ").append(row.name()), fontColumn, y, TEXT, true);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
