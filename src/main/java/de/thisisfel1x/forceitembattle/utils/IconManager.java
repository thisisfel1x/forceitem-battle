package de.thisisfel1x.forceitembattle.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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

public class IconManager {

    private final Map<String, TextComponent> iconMap = new HashMap<>();

    public void loadIcons() {
        Gson gson = new Gson();
        Type stringMapType = new TypeToken<Map<String, String>>() {
        }.getType();

        try (InputStream inputStream = ForceItemBattle.getInstance().getResource("item_unicode_mapping.json")) {

            if (inputStream == null) {
                return;
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

                Map<String, String> rawMap = gson.fromJson(reader, stringMapType);

                this.iconMap.clear();
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    TextComponent iconComponent = Component.text(entry.getValue());
                    this.iconMap.put(entry.getKey(), iconComponent);
                }


            }

        } catch (IOException | JsonSyntaxException ignored) {
        }
    }

    public TextComponent getIcon(String itemName) {
        TextComponent icon = this.iconMap.get(itemName);
        if (icon == null) {
            System.err.println("Warnung: Icon für '" + itemName + "' nicht gefunden.");
        }
        return icon;
    }

    public TextComponent getIconSafe(String itemName) {
        TextComponent icon = this.iconMap.get(itemName);
        return (icon != null) ? icon : Component.empty();
    }

    public boolean isLoaded() {
        return !this.iconMap.isEmpty();
    }
}
