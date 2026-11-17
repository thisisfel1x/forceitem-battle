package de.thisisfel1x.iconexporter;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Draws a grid of items on a solid background. Every cell is {@code cell} GUI units big and the
 * item sits at an offset of 1 unit, so neighbouring items never touch.
 */
final class IconSheetScreen extends Screen {

    private final int cell;
    private List<ItemStack> stacks = List.of();
    private int background = 0xFF000000;

    IconSheetScreen(int cell) {
        super(Component.literal("ForceItemBattle icon sheet"));
        this.cell = cell;
    }

    /**
     * Must be called on the client thread.
     */
    void show(List<ItemStack> stacks, int background) {
        this.stacks = List.copyOf(stacks);
        this.background = background;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // No menu background / blur - the sheet paints its own solid background
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), this.background);

        int columns = Math.max(1, graphics.guiWidth() / this.cell);
        for (int i = 0; i < this.stacks.size(); i++) {
            int x = (i % columns) * this.cell + 1;
            int y = (i / columns) * this.cell + 1;
            graphics.fakeItem(this.stacks.get(i), x, y);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
