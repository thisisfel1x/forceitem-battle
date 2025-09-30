package de.thisisfel1x.forceitembattle.utils;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ForceItemBattleScoreboardManager {

    private final ForceItemBattle forceItemBattle;
    private final Scoreboard mainScoreboard;

    public ForceItemBattleScoreboardManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        createSpectatorTeam();
    }

    public void updateTeam(ForceItemBattleTeam logicForceItemBattleTeam) {
        String teamNumber = logicForceItemBattleTeam.getTeamName().replaceAll("[^0-9]", "");
        String teamId = String.format("%02d_fib_%s", Integer.parseInt(teamNumber), logicForceItemBattleTeam.getTeamName().replace(" ", "").toLowerCase());

        Team scoreboardTeam = mainScoreboard.getTeam(teamId);
        if (scoreboardTeam == null) {
            scoreboardTeam = mainScoreboard.registerNewTeam(teamId);
        }

        //scoreboardTeam.color(NamedTextColor.nearestTo(logicForceItemBattleTeam.getTeamColor()));
        scoreboardTeam.color(NamedTextColor.WHITE);

        Component prefix = Component.text("[#" + teamNumber + "] ", logicForceItemBattleTeam.getTeamColor());

        scoreboardTeam.prefix(prefix);
    }


    private void createSpectatorTeam() {
        String spectatorTeamId = "99_fib_spectator";
        Team specTeam = mainScoreboard.getTeam(spectatorTeamId);
        if (specTeam == null) {
            specTeam = mainScoreboard.registerNewTeam(spectatorTeamId);
        }

        specTeam.color(NamedTextColor.GRAY);
        specTeam.prefix(Component.empty().decoration(TextDecoration.ITALIC, true));
    }

    public void setPlayerSpectator(GamePlayer gamePlayer) {
        this.removePlayerFromTeam(gamePlayer);

        Team specTeam = mainScoreboard.getTeam("99_fib_spectator");
        if (specTeam != null) {
            specTeam.addEntry(gamePlayer.getName());
        }
    }

    public void addPlayerToTeam(GamePlayer gamePlayer) {
        if (!gamePlayer.isInTeam()) return;

        ForceItemBattleTeam logicForceItemBattleTeam = gamePlayer.getTeam();

        String teamNumber = logicForceItemBattleTeam.getTeamName().replaceAll("[^0-9]", "");
        String teamId = String.format("%02d_fib_%s", Integer.parseInt(teamNumber), logicForceItemBattleTeam.getTeamName().replace(" ", "").toLowerCase());

        Team scoreboardTeam = mainScoreboard.getTeam(teamId);
        if (scoreboardTeam != null) {
            scoreboardTeam.addEntry(gamePlayer.getName());
        }
    }

    public void removePlayerFromTeam(GamePlayer gamePlayer) {
        Team currentTeam = mainScoreboard.getEntryTeam(gamePlayer.getName());
        if (currentTeam != null) {
            currentTeam.removeEntry(gamePlayer.getName());
        }
    }

    public void cleanup() {
        mainScoreboard.getTeams().stream()
                .filter(team -> team.getName().contains("_fib_"))
                .forEach(Team::unregister);
    }
}
