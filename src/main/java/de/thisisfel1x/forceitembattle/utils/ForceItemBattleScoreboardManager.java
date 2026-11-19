package de.thisisfel1x.forceitembattle.utils;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.LanguageManager;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.team.ScoreboardTeam;
import net.megavex.scoreboardlibrary.api.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ForceItemBattleScoreboardManager {

    private static final String SPECTATOR_TEAM_ID = "99_fib_spectator";

    private final ForceItemBattle forceItemBattle;
    private final TeamManager teamManager;
    private final Map<UUID, Sidebar> playerSidebars = new HashMap<>();
    private final boolean showRankingIngame;
    private final BukkitTask updateTask;

    private final Component scoreboardTitle = Component.text("");

    public ForceItemBattleScoreboardManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        ScoreboardLibrary scoreboardLibrary = forceItemBattle.getScoreboardLibrary();
        this.teamManager = scoreboardLibrary.createTeamManager();
        this.showRankingIngame = forceItemBattle.getConfig().getBoolean("scoreboard.show-ranking-ingame", false);

        createSpectatorTeam();

        // Runs on the main thread: sidebars, teams and the game state are not thread safe.
        // The library itself sends the packets asynchronously
        this.updateTask = Bukkit.getScheduler().runTaskTimer(forceItemBattle, this::updateSidebar, 0L, 20L);
    }

    private static String teamKey(ForceItemBattleTeam team) {
        return String.format("%02d_fib_%s", team.getTeamId(), team.getTeamName().toLowerCase(Locale.ROOT));
    }

    public void handlePlayerJoin(Player player) {
        Sidebar sidebar = forceItemBattle.getScoreboardLibrary().createSidebar();
        sidebar.addPlayer(player);

        Sidebar previous = playerSidebars.put(player.getUniqueId(), sidebar);
        if (previous != null) {
            previous.close();
        }
        teamManager.addPlayer(player);
    }

    public void handlePlayerQuit(Player player) {
        Sidebar sidebar = playerSidebars.remove(player.getUniqueId());
        if (sidebar != null) {
            sidebar.close();
        }
        teamManager.removePlayer(player);
    }

    public void updateSidebar() {
        GameManager gameManager = forceItemBattle.getGameManager();
        if (gameManager == null || gameManager.getCurrentGameState() == null) return; // plugin still enabling

        GameStateEnum currentState = gameManager.getCurrentGameState().getGameStateEnum();
        Function<Locale, ComponentSidebarLayout> layoutFactory;

        switch (currentState) {
            case IDLE, STARTING -> {
                layoutFactory = this::buildLobbyLayout;
                clearTeamSuffixes();
            }
            case INGAME -> {
                layoutFactory = this.showRankingIngame ? this::buildIngameLayout : locale -> buildEmptyLayout();
                for (ForceItemBattleTeam logicTeam : forceItemBattle.getTeamManager().getTeams()) {
                    ScoreboardTeam scoreboardTeam = teamManager.team(teamKey(logicTeam));
                    if (scoreboardTeam != null) {
                        ItemStack currentItem = logicTeam.getCurrentItem();
                        Component suffix = Component.text(" ● ", NamedTextColor.DARK_GRAY)
                                .append(Component.translatable(currentItem.translationKey(), NamedTextColor.GOLD));
                        scoreboardTeam.defaultDisplay().suffix(suffix);
                    }
                }
            }
            default -> {
                layoutFactory = locale -> buildEmptyLayout();
                clearTeamSuffixes();
            }
        }

        // Every sidebar in the language of its player - built once per language
        LanguageManager languageManager = forceItemBattle.getLanguageManager();
        Map<String, ComponentSidebarLayout> layoutsByLanguage = new HashMap<>();
        for (Map.Entry<UUID, Sidebar> entry : playerSidebars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Locale locale = player != null ? player.locale() : null;
            layoutsByLanguage.computeIfAbsent(languageManager.languageFor(locale), language -> layoutFactory.apply(locale))
                    .apply(entry.getValue());
        }
    }

    private void clearTeamSuffixes() {
        for (ScoreboardTeam team : teamManager.teams()) {
            team.defaultDisplay().suffix(Component.empty());
        }
    }

    private ComponentSidebarLayout buildLobbyLayout(@Nullable Locale locale) {
        SidebarComponent title = SidebarComponent.staticLine(this.scoreboardTitle);

        Component players = forceItemBattle.getLanguageManager().render(locale, Message.SCOREBOARD_LOBBY_PLAYERS,
                Placeholder.unparsed("online", Integer.toString(Bukkit.getOnlinePlayers().size())),
                Placeholder.unparsed("max", Integer.toString(forceItemBattle.getTeamManager().getMaxPlayers())));

        SidebarComponent lines = SidebarComponent.builder()
                .addBlankLine()
                .addStaticLine(players)
                .addBlankLine()
                .build();

        return new ComponentSidebarLayout(title, lines);
    }

    private ComponentSidebarLayout buildIngameLayout(@Nullable Locale locale) {
        SidebarComponent title = SidebarComponent.staticLine(this.scoreboardTitle);

        var linesBuilder = SidebarComponent.builder();

        List<ForceItemBattleTeam> sortedTeams = forceItemBattle.getTeamManager().getTeams().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .sorted(Comparator.comparingInt((ForceItemBattleTeam team) -> team.getFoundItems().size()).reversed()
                        .thenComparingLong(ForceItemBattleTeam::getLastFoundTimestamp))
                .toList();

        linesBuilder.addBlankLine();
        for (ForceItemBattleTeam team : sortedTeams) {
            linesBuilder.addStaticLine(forceItemBattle.getLanguageManager().render(locale, Message.SCOREBOARD_TEAM_LINE,
                    Placeholder.styling("team_color", team.getTeamColor()),
                    Placeholder.unparsed("id", Integer.toString(team.getTeamId())),
                    LanguageManager.count("items", team.getFoundItems().size())));
        }
        linesBuilder.addBlankLine();

        return new ComponentSidebarLayout(title, linesBuilder.build());
    }

    private ComponentSidebarLayout buildEmptyLayout() {
        return new ComponentSidebarLayout(SidebarComponent.staticLine(Component.empty()),
                SidebarComponent.staticLine(Component.empty()));
    }

    public void updateTeam(ForceItemBattleTeam logicForceItemBattleTeam) {
        ScoreboardTeam team = teamManager.createIfAbsent(teamKey(logicForceItemBattleTeam));

        Component prefix = Component.text("[#" + logicForceItemBattleTeam.getTeamId() + "] ", logicForceItemBattleTeam.getTeamColor());

        team.defaultDisplay().prefix(prefix);
    }

    private void createSpectatorTeam() {
        ScoreboardTeam specTeam = teamManager.createIfAbsent(SPECTATOR_TEAM_ID);
        specTeam.defaultDisplay().playerColor(NamedTextColor.GRAY);
    }

    public void setPlayerSpectator(GamePlayer gamePlayer) {
        this.removePlayerFromTeam(gamePlayer);
        ScoreboardTeam specTeam = teamManager.team(SPECTATOR_TEAM_ID);
        if (specTeam != null) {
            specTeam.defaultDisplay().addEntry(gamePlayer.getName());
        }
    }

    public void addPlayerToTeam(GamePlayer gamePlayer) {
        if (!gamePlayer.isInTeam()) return;

        this.removePlayerFromTeam(gamePlayer);
        ScoreboardTeam team = teamManager.team(teamKey(gamePlayer.getTeam()));
        if (team != null) {
            team.defaultDisplay().addEntry(gamePlayer.getName());
        }
    }

    public void removePlayerFromTeam(GamePlayer gamePlayer) {
        teamManager.teams().forEach(team -> team.defaultDisplay().removeEntry(gamePlayer.getName()));
    }

    public void cleanup() {
        this.updateTask.cancel();

        playerSidebars.values().forEach(Sidebar::close);
        playerSidebars.clear();

        if (teamManager != null) {
            teamManager.close();
        }
    }
}
