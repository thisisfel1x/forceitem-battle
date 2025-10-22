package de.thisisfel1x.forceitembattle.utils;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.team.ScoreboardTeam;
import net.megavex.scoreboardlibrary.api.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ForceItemBattleScoreboardManager {

    private final ForceItemBattle forceItemBattle;
    private final TeamManager teamManager;
    private final Map<UUID, Sidebar> playerSidebars = new HashMap<>();

    public ForceItemBattleScoreboardManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        ScoreboardLibrary scoreboardLibrary = forceItemBattle.getScoreboardLibrary();
        this.teamManager = scoreboardLibrary.createTeamManager();

        createSpectatorTeam();

        Bukkit.getScheduler().runTaskTimerAsynchronously(forceItemBattle, this::updateSidebar, 0L, 20L);
    }

    public void handlePlayerJoin(Player player) {
        Sidebar sidebar = forceItemBattle.getScoreboardLibrary().createSidebar();
        sidebar.addPlayer(player);
        playerSidebars.put(player.getUniqueId(), sidebar);
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
        GameStateEnum currentState = forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();
        ComponentSidebarLayout layout;

        switch (currentState) {
            case IDLE, STARTING -> {
                layout = buildLobbyLayout();
                for (ScoreboardTeam team : teamManager.teams()) {
                    team.defaultDisplay().suffix(Component.empty());
                }
            }
            case INGAME -> {
                layout = buildEmptyLayout();
                for (ForceItemBattleTeam logicTeam : forceItemBattle.getTeamManager().getTeams()) {
                    String teamId = String.format("%02d_fib_%s", logicTeam.getTeamId(), logicTeam.getTeamName().toLowerCase());
                    ScoreboardTeam scoreboardTeam = teamManager.team(teamId);
                    if (scoreboardTeam != null) {
                        ItemStack currentItem = logicTeam.getCurrentItem();
                        Component suffix = Component.text(" ● ", NamedTextColor.DARK_GRAY)
                                .append(Component.translatable(currentItem.translationKey(), NamedTextColor.GOLD));
                        scoreboardTeam.defaultDisplay().suffix(suffix);
                    }
                }
            }
            default -> {
                layout = buildEmptyLayout();
                for (ScoreboardTeam team : teamManager.teams()) {
                    team.defaultDisplay().suffix(Component.empty());
                }
            }
        }

        for (Sidebar sidebar : playerSidebars.values()) {
            layout.apply(sidebar);
        }
    }

    private ComponentSidebarLayout buildLobbyLayout() {
        SidebarComponent title = SidebarComponent.staticLine(
                forceItemBattle.miniMessage().deserialize("<dark_gray>» <aqua>teamcrimx<bold>DE</bold> <dark_gray>«"));

        SidebarComponent lines = SidebarComponent.builder()
                .addBlankLine()
                .addStaticLine(Component.text("Spieler:", NamedTextColor.WHITE))
                .addDynamicLine(() -> {
                    int online = Bukkit.getOnlinePlayers().size();
                    int max = forceItemBattle.getTeamManager().getTeams().size() * 2;
                    return forceItemBattle.miniMessage().deserialize("<dark_gray>● <green>" + online + "<gray>/<white>" + max);
                })
                .addBlankLine()
                .build();

        return new ComponentSidebarLayout(title, lines);
    }

    private ComponentSidebarLayout buildIngameLayout() {
        SidebarComponent title = SidebarComponent.staticLine(
                forceItemBattle.miniMessage().deserialize("<dark_gray>» <aqua>teamcrimx<bold>DE</bold> <dark_gray>«"));

        var linesBuilder = SidebarComponent.builder();

        List<ForceItemBattleTeam> sortedTeams = forceItemBattle.getTeamManager().getTeams().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .sorted(Comparator.comparingInt(team -> ((ForceItemBattleTeam) team).getFoundItems().size()).reversed())
                .toList();

        linesBuilder.addBlankLine();
        for (ForceItemBattleTeam team : sortedTeams) {
            linesBuilder.addDynamicLine(() -> Component.text("#" + team.getTeamId() + " ", team.getTeamColor())
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(team.getFoundItems().size() + " "
                                    + (team.getFoundItems().size() == 1 ? "Item" : "Items"), NamedTextColor.WHITE))));
        }
        linesBuilder.addBlankLine();

        return new ComponentSidebarLayout(title, linesBuilder.build());
    }

    private ComponentSidebarLayout buildEmptyLayout() {
        return new ComponentSidebarLayout(SidebarComponent.staticLine(Component.empty()),
                SidebarComponent.staticLine(Component.empty()));
    }

    public void updateTeam(ForceItemBattleTeam logicForceItemBattleTeam) {
        String teamId = String.format("%02d_fib_%s", logicForceItemBattleTeam.getTeamId(), logicForceItemBattleTeam.getTeamName().toLowerCase());
        ScoreboardTeam team = teamManager.createIfAbsent(teamId);

        String teamNumber = logicForceItemBattleTeam.getTeamName().replaceAll("[^0-9]", "");
        Component prefix = Component.text("[#" + teamNumber + "] ", logicForceItemBattleTeam.getTeamColor());

        team.defaultDisplay().prefix(prefix);
    }

    private void createSpectatorTeam() {
        ScoreboardTeam specTeam = teamManager.createIfAbsent("99_fib_spectator");
        specTeam.defaultDisplay().playerColor(NamedTextColor.GRAY);
    }

    public void setPlayerSpectator(GamePlayer gamePlayer) {
        this.removePlayerFromTeam(gamePlayer);
        ScoreboardTeam specTeam = teamManager.team("99_fib_spectator");
        if (specTeam != null) {
            specTeam.defaultDisplay().addEntry(gamePlayer.getName());
        }
    }

    public void addPlayerToTeam(GamePlayer gamePlayer) {
        if (!gamePlayer.isInTeam()) return;
        ForceItemBattleTeam logicTeam = gamePlayer.getTeam();
        String teamId = String.format("%02d_fib_%s", logicTeam.getTeamId(), logicTeam.getTeamName().toLowerCase());
        ScoreboardTeam team = teamManager.team(teamId);
        if (team != null) {
            team.defaultDisplay().addEntry(gamePlayer.getName());
        }
    }

    public void removePlayerFromTeam(GamePlayer gamePlayer) {
        teamManager.teams().forEach(team -> team.defaultDisplay().removeEntry(gamePlayer.getName()));
    }

    public void cleanup() {
        if (teamManager != null) {
            teamManager.close();
        }
        playerSidebars.values().forEach(Sidebar::close);
        playerSidebars.clear();
    }
}