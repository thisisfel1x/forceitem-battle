package de.thisisfel1x.forceitembattle.teams;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            "#2196F3",
            "#E91E63",
            "#9C27B0",
            "#00E676",
            "#FF9100",
            "#CDDC39",
            "#607D8B"
    );

    public TeamManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;

        int configuredAmount = forceItemBattle.getConfig().getInt("teams.amount", 14);
        int amount = Math.max(1, Math.min(this.hexColorPalette.size(), configuredAmount));
        if (amount != configuredAmount) {
            forceItemBattle.getLogger().warning("teams.amount must be between 1 and " + this.hexColorPalette.size()
                    + ", using " + amount);
        }

        int teamSize = Math.max(1, forceItemBattle.getConfig().getInt("teams.size", 2));

        this.setupTeams(amount, teamSize);
    }

    private void setupTeams(int amount, int teamSize) {
        for (int i = 0; i < amount; i++) {
            String hexCode = hexColorPalette.get(i);

            TextColor teamColor = TextColor.fromHexString(hexCode);

            if (teamColor != null) {
                ForceItemBattleTeam forceItemBattleTeam = new ForceItemBattleTeam(i + 1, "Team #" + (i + 1), teamColor, teamSize);
                forceItemBattleTeam.setupForGame();

                this.forceItemBattleTeams.add(forceItemBattleTeam);
            }
        }

        this.forceItemBattleTeams.forEach(team ->
                this.forceItemBattle.getForceItemBattleScoreboardManager().updateTeam(team));
    }

    /**
     * Puts every player without a team into the team with the fewest members, so players are
     * spread across empty teams first and teams are only filled up when no empty team is left.
     * Players are only kicked if every single team is full
     */
    public void assignAllPlayersWithoutTeam() {
        List<GamePlayer> teamlessPlayers = new ArrayList<>(this.gamePlayers.values().stream()
                .filter(gamePlayer -> !gamePlayer.isSpectator() && !gamePlayer.isInTeam())
                .toList());

        if (teamlessPlayers.isEmpty()) {
            return;
        }

        Collections.shuffle(teamlessPlayers);

        List<ForceItemBattleTeam> shuffledTeams = new ArrayList<>(this.forceItemBattleTeams);
        Collections.shuffle(shuffledTeams);

        List<GamePlayer> playersWithoutSpace = new ArrayList<>();

        for (GamePlayer playerToAssign : teamlessPlayers) {
            // min() returns the first of equally small teams, the shuffled order keeps it random
            Optional<ForceItemBattleTeam> targetTeam = shuffledTeams.stream()
                    .filter(team -> !team.isFull())
                    .min(Comparator.comparingInt(ForceItemBattleTeam::getCurrentTeamSize));

            if (targetTeam.isEmpty() || !targetTeam.get().addPlayer(playerToAssign)) {
                playersWithoutSpace.add(playerToAssign);
                continue;
            }

            playerToAssign.sendMessage(Message.TEAM_ASSIGNED,
                    Placeholder.component("team", targetTeam.get().getDisplayName()));
        }

        // Remove first, kick afterwards: nothing else may iterate over stale team-less players
        for (GamePlayer gamePlayer : playersWithoutSpace) {
            this.gamePlayers.remove(gamePlayer.getUniqueId());
            gamePlayer.ifOnline(player -> player.kick(
                    this.forceItemBattle.getLanguageManager().render(player, Message.TEAM_NO_FREE_TEAM)));
        }
    }

    public List<ForceItemBattleTeam> getTeams() {
        return forceItemBattleTeams;
    }

    public Map<UUID, GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public @Nullable GamePlayer getGamePlayer(UUID uuid) {
        return gamePlayers.get(uuid);
    }

    /**
     * @return {@code true} if the player is unknown to the game or a spectator, i.e. must not
     * influence the running game in any way
     */
    public boolean isSpectator(Player player) {
        GamePlayer gamePlayer = this.gamePlayers.get(player.getUniqueId());
        return gamePlayer == null || gamePlayer.isSpectator();
    }

    /**
     * Hides all online spectators from the given (non-spectator) player
     */
    public void hideSpectatorsFrom(Player viewer) {
        for (GamePlayer gamePlayer : this.gamePlayers.values()) {
            if (gamePlayer.isSpectator()) {
                gamePlayer.ifOnline(spectator -> viewer.hidePlayer(this.forceItemBattle, spectator));
            }
        }
    }

    /**
     * @return the maximum number of active players across all teams
     */
    public int getMaxPlayers() {
        return this.forceItemBattleTeams.stream().mapToInt(ForceItemBattleTeam::getMaxTeamSize).sum();
    }

    public Optional<ForceItemBattleTeam> findAvailableTeam() {
        return forceItemBattleTeams.stream().filter(forceItemBattleTeam -> !forceItemBattleTeam.isFull()).findFirst();
    }

    @Override
    public String toString() {
        return "TeamManager{" +
                "teams=" + forceItemBattleTeams +
                ", gamePlayers=" + gamePlayers.size() +
                '}';
    }
}
