package de.thisisfel1x.forceitembattle.teams;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.format.TextColor;

import java.util.*;

public class TeamManager {

    private final ForceItemBattle forceItemBattle;

    private final List<Team> teams = new ArrayList<>();
    private final Map<UUID, GamePlayer> gamePlayers = new HashMap<>();

    private final List<String> hexColorPalette = Arrays.asList(
            "#FF5555",
            "#55AAFF",
            "#55FF55",
            "#FFFF55",
            "#FFAA00",
            "#55FFFF",
            "#FF55FF",
            "#AA00AA",
            "#00AAAA",
            "#AAAAAA",
            "#F44336",
            "#2196F3"
    );

    public TeamManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;

        this.setupTeams(8);
    }

    private void setupTeams(int amount) {
        for (int i = 0; i < amount; i++) {
            String hexCode = hexColorPalette.get(i);

            TextColor teamColor = TextColor.fromHexString(hexCode);

            if (teamColor != null) {
                Team team = new Team("Team #" + (i + 1), teamColor);
                this.teams.add(team);
            }
        }
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Map<UUID, GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        return gamePlayers.get(uuid);
    }

    public Optional<Team> findAvailableTeam() {
        return teams.stream().filter(team -> !team.isFull()).findFirst();
    }

    @Override
    public String toString() {
        return "TeamManager{" +
                "forceItemBattle=" + forceItemBattle +
                ", teams=" + teams +
                ", gamePlayers=" + gamePlayers +
                ", hexColorPalette=" + hexColorPalette +
                '}';
    }
}
