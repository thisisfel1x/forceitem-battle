package de.thisisfel1x.iconexporter;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Helpers shared by the client tests. All client tests are registered in fabric.mod.json, the system property
 * {@code fib.test} (set by build.gradle) decides which one actually runs
 */
final class ClientTests {

    private ClientTests() {
    }

    /**
     * @param name {@code icons} or {@code e2e}
     */
    static boolean isSelected(String name) {
        return name.equals(System.getProperty("fib.test", "icons"));
    }

    /**
     * Resizes the framebuffer and forces a GUI scale. Items in the GUI are drawn from an atlas whose
     * slots are {@code 16 * guiScale} pixels big, so the GUI scale decides the icon resolution
     */
    static void applyWindow(ClientGameTestContext context, int width, int height, int guiScale) {
        context.runOnClient(minecraft -> {
            Window window = minecraft.getWindow();
            window.setWidth(width);
            window.setHeight(height);
            minecraft.gameRenderer.resize(width, height);
            minecraft.options.guiScale().set(guiScale);
            minecraft.resizeGui();

            if (window.getGuiScale() != guiScale) {
                throw new IllegalStateException("Could not apply GUI scale " + guiScale + " to a " + width + "x" + height
                        + " framebuffer (got " + window.getGuiScale() + ")");
            }
        });
    }
}
