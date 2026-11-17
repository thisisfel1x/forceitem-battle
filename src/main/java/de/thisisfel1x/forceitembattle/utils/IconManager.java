package de.thisisfel1x.forceitembattle.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.thisisfel1x.forceitembattle.ForceItemBattle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Maps item names to the glyphs of the custom resource pack font.
 */
public class IconManager {

    private static final String MAPPING_FILE = "item_unicode_mapping.json";

    private final ForceItemBattle forceItemBattle;
    private final Map<String, TextComponent> iconMap = new HashMap<>();

    public IconManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void loadIcons() {
        Type stringMapType = new TypeToken<Map<String, String>>() {
        }.getType();

        try (InputStream inputStream = this.forceItemBattle.getResource(MAPPING_FILE)) {
            if (inputStream == null) {
                this.forceItemBattle.getLogger().warning(MAPPING_FILE + " fehlt im Plugin - Items werden ohne Icon angezeigt.");
                return;
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Map<String, String> rawMap = new Gson().fromJson(reader, stringMapType);

                this.iconMap.clear();
                if (rawMap != null) {
                    rawMap.forEach((key, value) -> this.iconMap.put(key, Component.text(value)));
                }
            }

            this.forceItemBattle.getLogger().info(this.iconMap.size() + " Item-Icons geladen.");
        } catch (IOException | JsonParseException exception) {
            this.forceItemBattle.getLogger().log(Level.WARNING,
                    MAPPING_FILE + " konnte nicht geladen werden - Items werden ohne Icon angezeigt.", exception);
        }
    }

    /**
     * @return the icon for the given key or an empty component if there is none
     */
    public TextComponent getIconSafe(String itemName) {
        TextComponent icon = this.iconMap.get(itemName);
        return (icon != null) ? icon : Component.empty();
    }

    public boolean hasIcon(String itemName) {
        return this.iconMap.containsKey(itemName);
    }

    public boolean isLoaded() {
        return !this.iconMap.isEmpty();
    }
}
