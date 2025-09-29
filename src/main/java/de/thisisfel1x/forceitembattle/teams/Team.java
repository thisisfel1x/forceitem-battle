package de.thisisfel1x.forceitembattle.teams;

import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Team {

    private final String teamName;
    private final TextColor teamColor;
    private final List<GamePlayer> teamMembers;
    private final int maxTeamSize = 2;

    public Team(String teamName, TextColor teamColor) {
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.teamMembers = new ArrayList<>();
    }

    public Audience getTeamAudience() {
        List<Player> onlinePlayers = this.teamMembers.stream()
                .map(GamePlayer::getPlayer)
                .filter(Objects::nonNull)
                .toList();
        return Audience.audience(onlinePlayers);
    }

    public void addPlayer(GamePlayer gamePlayer) {
        if (this.isFull()) {
            gamePlayer.getPlayer().sendMessage(Component.text("This team is already full!"));
            return;
        }

        this.teamMembers.add(gamePlayer);
        gamePlayer.setTeam(this);

        Component joinMessage = Component.text(gamePlayer.getPlayer().getName(), teamColor)
                .append(Component.text(" ist deinem Team beigetreten!", NamedTextColor.GRAY));
        this.broadcastTeamMessage(joinMessage);
    }

    public void removePlayer(GamePlayer gamePlayer) {
        if (this.teamMembers.remove(gamePlayer)) {
            gamePlayer.setTeam(null);

            Component leaveMessage = Component.text(gamePlayer.getPlayer().getName(), teamColor)
                    .append(Component.text(" hat dein Team verlassen.", NamedTextColor.GRAY));
            this.broadcastTeamMessage(leaveMessage);
        }
    }

    public void broadcastTeamMessage(Component message) {
        this.getTeamAudience().sendMessage(message);
    }

    public boolean isFull() {
        return this.teamMembers.size() >= this.maxTeamSize;
    }

    public List<GamePlayer> getTeamMembers() {
        return teamMembers;
    }

    public String getTeamName() {
        return teamName;
    }

    public TextColor getTeamColor() {
        return teamColor;
    }

    public int getCurrentTeamSize() {
        return this.teamMembers.size();
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    @Override
    public String toString() {
        return "Team{" +
                "teamName='" + teamName + '\'' +
                ", teamColor=" + teamColor +
                ", teamMembers=" + teamMembers +
                ", maxTeamSize=" + maxTeamSize +
                '}';
    }
}
