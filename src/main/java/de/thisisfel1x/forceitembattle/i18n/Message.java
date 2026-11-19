package de.thisisfel1x.forceitembattle.i18n;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every text the plugin shows to players. The texts themselves are MiniMessage strings in the language files
 * ({@code plugins/forceitem-battle/lang/<language>.yml}), the key is their path in the file
 * <p>
 * Placeholders are listed per constant. {@code <prefix>} works in every message
 */
public enum Message {

    PREFIX("prefix"),

    // Commands
    COMMAND_PLAYERS_ONLY("command.players-only"),
    COMMAND_ONLY_INGAME("command.only-ingame"),
    COMMAND_NOT_A_PLAYER("command.not-a-player"),
    COMMAND_NOT_IN_TEAM("command.not-in-team"),
    COMMAND_TOP_OVERWORLD_ONLY("command.top.overworld-only"),
    COMMAND_START_ALREADY_RUNNING("command.start.already-running"),
    COMMAND_START_USAGE("command.start.usage"),
    /** {@code <max>} */
    COMMAND_START_INVALID_DURATION("command.start.invalid-duration"),
    /** {@code <max>} */
    COMMAND_START_INVALID_JOKERS("command.start.invalid-jokers"),
    COMMAND_START_NO_PLAYERS("command.start.no-players"),
    COMMAND_START_STARTING("command.start.starting"),

    // Backpack and recipes
    BACKPACK_DISABLED("backpack.disabled"),
    BACKPACK_TITLE("backpack.title"),
    /** {@code <item>} */
    RECIPE_TITLE("recipe.title"),
    RECIPE_NONE("recipe.none"),
    RECIPE_UNSUPPORTED("recipe.unsupported"),

    // Lobby, joining and leaving
    /** {@code <dots>} - animated, 0 to 3 dots */
    LOBBY_WAITING_FOR_PLAYERS("lobby.waiting-for-players"),
    LOBBY_BOSSBAR("lobby.bossbar"),
    LOBBY_ITEM_TEAM_SELECTOR("lobby.item.team-selector"),
    LOBBY_ITEM_SETTINGS("lobby.item.settings"),
    /** {@code <player>} */
    PLAYER_JOINED("player.joined"),
    /** {@code <player>} */
    PLAYER_LEFT("player.left"),
    /** {@code <team>} */
    PLAYER_WELCOME_BACK("player.welcome-back"),

    // Spectators
    SPECTATOR_ITEM_COMPASS("spectator.item.compass"),
    SPECTATOR_GUI_TITLE("spectator.gui.title"),
    SPECTATOR_PLAYER_OFFLINE("spectator.player-offline"),

    // Teams
    TEAM_GUI_TITLE("team.gui.title"),
    TEAM_ALREADY_MEMBER("team.already-member"),
    TEAM_FULL("team.full"),
    /** {@code <player>} */
    TEAM_MEMBER_JOINED("team.member-joined"),
    /** {@code <player>} */
    TEAM_MEMBER_LEFT("team.member-left"),
    /** {@code <team>} */
    TEAM_ASSIGNED("team.assigned"),
    TEAM_NO_FREE_TEAM("team.no-free-team"),

    // Items and jokers
    JOKER_NAME("joker.name"),
    /** {@code <player>}, {@code <item>} */
    JOKER_USED("joker.used"),
    /** {@code <item>} - the next item */
    ITEM_FOUND("item.found"),
    /** {@code <item>} */
    ITEM_NEXT("item.next"),

    // Settings
    SETTINGS_GUI_TITLE("settings.gui.title"),
    SETTINGS_STATUS_ENABLED("settings.status.enabled"),
    SETTINGS_STATUS_DISABLED("settings.status.disabled"),
    SETTINGS_BACKPACK_NAME("settings.backpack.name"),
    SETTINGS_BACKPACK_DESCRIPTION("settings.backpack.description"),
    SETTINGS_NO_HUNGER_NAME("settings.no-hunger.name"),
    SETTINGS_NO_HUNGER_DESCRIPTION("settings.no-hunger.description"),
    SETTINGS_NO_DAMAGE_NAME("settings.no-damage.name"),
    SETTINGS_NO_DAMAGE_DESCRIPTION("settings.no-damage.description"),
    SETTINGS_STARTER_TOOLS_NAME("settings.starter-tools.name"),
    SETTINGS_STARTER_TOOLS_DESCRIPTION("settings.starter-tools.description"),

    // Game
    STARTING_SUBTITLE("starting.subtitle"),
    /** {@code <seconds>}, {@code <plural:'...'>} */
    STARTING_ACTIONBAR("starting.actionbar"),
    STARTING_GO("starting.go"),
    /** {@code <time>} - mm:ss */
    GAME_TIMER("game.timer"),
    /** {@code <time>} - mm:ss */
    GAME_TIMER_SPECTATOR("game.timer-spectator"),

    // Results
    RESULTS_TITLE("results.title"),
    RESULTS_HIDDEN_TEAM("results.hidden-team"),
    RESULTS_JOKER_SUFFIX("results.joker-suffix"),
    /** {@code <items>}, {@code <plural:'...'>} */
    RESULTS_TEAM_ITEMS("results.team-items"),
    RESULTS_FINAL_HEADER("results.final.header"),
    /** {@code <rank>}, {@code <team>}, {@code <items>}, {@code <plural:'...'>} */
    RESULTS_FINAL_LINE("results.final.line"),
    RESULTS_FINAL_FOOTER("results.final.footer"),
    /** {@code <seconds>}, {@code <plural:'...'>} */
    RESULTS_SHUTDOWN("results.shutdown"),

    // Scoreboard
    /** {@code <online>}, {@code <max>} */
    SCOREBOARD_LOBBY_PLAYERS("scoreboard.lobby-players"),
    /** {@code <team_color>...</team_color>}, {@code <id>}, {@code <items>}, {@code <plural:'...'>} */
    SCOREBOARD_TEAM_LINE("scoreboard.team-line"),

    RESOURCE_PACK_PROMPT("resource-pack.prompt");

    /** Prefix of the translation keys in {@link #translatable()} */
    static final String TRANSLATION_KEY_PREFIX = "forceitembattle.";

    private static final Map<String, Message> BY_TRANSLATION_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Message::translationKey, Function.identity()));

    private final String key;

    Message(String key) {
        this.key = key;
    }

    /**
     * @return the path of the message in the language files
     */
    public String key() {
        return this.key;
    }

    public String translationKey() {
        return TRANSLATION_KEY_PREFIX + this.key;
    }

    /**
     * A component that every client sees in its own language (rendered by the server when it is sent). For
     * objects that are shared by several players, like a boss bar or an inventory. Only for messages without
     * placeholders
     */
    public Component translatable() {
        return Component.translatable(this.translationKey());
    }

    static @Nullable Message byTranslationKey(String translationKey) {
        return BY_TRANSLATION_KEY.get(translationKey);
    }
}
