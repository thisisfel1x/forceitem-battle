package de.thisisfel1x.forceitembattle.teams;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.format.TextColor;

import java.util.*;

public class TeamManager {

    private final ForceItemBattle forceItemBattle;

    private final List<ForceItemBattleTeam> forceItemBattleTeams = new ArrayList<>();
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
                ForceItemBattleTeam forceItemBattleTeam = new ForceItemBattleTeam("Team #" + (i + 1), teamColor);
                this.forceItemBattleTeams.add(forceItemBattleTeam);
            }
        }

        this.forceItemBattleTeams.forEach(team ->
                this.forceItemBattle.getForceItemBattleScoreboardManager().updateTeam(team));
    }

    public List<ForceItemBattleTeam> getTeams() {
        return forceItemBattleTeams;
    }

    public Map<UUID, GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        return gamePlayers.get(uuid);
    }

    public Optional<ForceItemBattleTeam> findAvailableTeam() {
        return forceItemBattleTeams.stream().filter(forceItemBattleTeam -> !forceItemBattleTeam.isFull()).findFirst();
    }

    @Override
    public String toString() {
        return "TeamManager{" +
                "forceItemBattle=" + forceItemBattle +
                ", teams=" + forceItemBattleTeams +
                ", gamePlayers=" + gamePlayers +
                ", hexColorPalette=" + hexColorPalette +
                '}';
    }
}
