package de.thisisfel1x.forceitembattle.utils;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves which resource pack is sent to joining players
 *
 * <p>With an empty {@code resource-pack.url} the pack of the GitHub release of exactly this plugin
 * version is used. URL and SHA-1 of that pack are generated at build time (see
 * {@code gradle/resourcepack.gradle}) and shipped inside the jar, so they always match
 */
public class ResourcePackService {

    private static final String BUNDLED_INFO = "resourcepack/pack.properties";

    private final boolean required;
    private final @Nullable ResourcePackInfo packInfo;

    public ResourcePackService(ForceItemBattle forceItemBattle) {
        FileConfiguration config = forceItemBattle.getConfig();
        Logger logger = forceItemBattle.getLogger();

        this.required = config.getBoolean("resource-pack.required", true);

        if (!config.getBoolean("resource-pack.enabled", true)) {
            this.packInfo = null;
            logger.info("Resource pack is disabled, items are shown without icons");
            return;
        }

        String configuredUrl = config.getString("resource-pack.url", "").trim();
        String configuredHash = config.getString("resource-pack.sha1", "").trim().toLowerCase(Locale.ROOT);

        String url;
        String hash;
        if (configuredUrl.isEmpty()) {
            Properties bundled = loadBundledInfo(forceItemBattle);
            url = bundled.getProperty("url", "").trim();
            hash = configuredHash.isEmpty() ? bundled.getProperty("sha1", "").trim() : configuredHash;
        } else {
            url = configuredUrl;
            hash = configuredHash;
            if (hash.isEmpty()) {
                logger.warning("resource-pack.sha1 is not set: clients cannot cache the pack "
                        + "and may not notice updates under the same URL");
            }
        }

        if (!hash.isEmpty() && !hash.matches("[0-9a-f]{40}")) {
            logger.warning("resource-pack.sha1 is not a valid SHA-1 hash (40 hex characters) and is ignored");
            hash = "";
        }

        this.packInfo = createPackInfo(url, hash, logger);
        if (this.packInfo != null) {
            logger.info("Resource pack: " + url);
        }
    }

    private static @Nullable ResourcePackInfo createPackInfo(String url, String hash, Logger logger) {
        if (url.isEmpty()) {
            logger.warning("No resource pack URL available (resource-pack.url is empty and the plugin was built "
                    + "without pack information), items are shown without icons");
            return null;
        }

        try {
            URI uri = URI.create(url);
            // Stable per pack version: the client keeps a cached copy and replaces it when the pack changes
            UUID id = UUID.nameUUIDFromBytes((url + '#' + hash).getBytes(StandardCharsets.UTF_8));
            return ResourcePackInfo.resourcePackInfo(id, uri, hash);
        } catch (IllegalArgumentException exception) {
            logger.warning("resource-pack.url is not a valid URL: " + url);
            return null;
        }
    }

    private static Properties loadBundledInfo(ForceItemBattle forceItemBattle) {
        Properties properties = new Properties();
        try (InputStream inputStream = forceItemBattle.getResource(BUNDLED_INFO)) {
            if (inputStream == null) return properties;

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException exception) {
            forceItemBattle.getLogger().log(Level.WARNING, "Could not read " + BUNDLED_INFO, exception);
        }
        return properties;
    }

    public void sendTo(Player player) {
        if (this.packInfo == null) return;

        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(this.packInfo)
                .replace(true)
                .required(this.required)
                .prompt(ForceItemBattle.getInstance().getLanguageManager().render(player, Message.RESOURCE_PACK_PROMPT))
                .build());
    }

    /**
     * @return {@code true} if players receive a resource pack (and item icons can be shown)
     */
    public boolean isEnabled() {
        return this.packInfo != null;
    }
}
