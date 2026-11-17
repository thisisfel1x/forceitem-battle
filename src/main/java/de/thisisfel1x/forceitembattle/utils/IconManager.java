package de.thisisfel1x.forceitembattle.utils;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Item icons for text (boss bar, chat). Every icon is a sprite of the resource pack in the gui
 * atlas ({@code fib:icon/<item id>}) and is shown with a sprite object component - there is no
 * mapping to maintain, the item id is the sprite name.
 *
 * <p>Which icons exist is generated at build time from {@code resourcepack/icons/}, an unknown
 * sprite would be rendered as the pink/black "missing texture" otherwise.
 */
public class IconManager {

    private static final String ICON_LIST = "resourcepack/icons.txt";
    private static final Key GUI_ATLAS = Key.key(Key.MINECRAFT_NAMESPACE, "gui");
    private static final String SPRITE_NAMESPACE = "fib";

    private final ForceItemBattle forceItemBattle;
    private final Set<String> icons = new HashSet<>();

    public IconManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    /**
     * @param resourcePackEnabled without the resource pack the sprites do not exist on the client
     */
    public void loadIcons(boolean resourcePackEnabled) {
        this.icons.clear();
        if (!resourcePackEnabled) return;

        try (InputStream inputStream = this.forceItemBattle.getResource(ICON_LIST)) {
            if (inputStream == null) {
                this.forceItemBattle.getLogger().warning(ICON_LIST + " is missing in the plugin, items are shown without icons");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .forEach(this.icons::add);
            }

            this.forceItemBattle.getLogger().info(this.icons.size() + " item icons in the resource pack");
        } catch (IOException exception) {
            this.forceItemBattle.getLogger().log(Level.WARNING,
                    "Could not load " + ICON_LIST + ", items are shown without icons", exception);
        }
    }

    /**
     * @return the icon followed by a space, or an empty component if there is no icon
     */
    public Component getIconWithSpacing(Material material) {
        if (!this.hasIcon(material)) return Component.empty();

        String id = material.getKey().getKey();
        return Component.object(ObjectContents.sprite(GUI_ATLAS, Key.key(SPRITE_NAMESPACE, "icon/" + id)))
                .append(Component.space());
    }

    public boolean hasIcon(Material material) {
        return this.icons.contains(material.getKey().getKey());
    }

    public boolean isLoaded() {
        return !this.icons.isEmpty();
    }
}
