package de.thisisfel1x.forceitembattle.player;

import de.thisisfel1x.forceitembattle.teams.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {

    private final Player player;
    private final String name;
    private final UUID uniqueId;

    private Team team;

    public GamePlayer(Player player) {

        this.player = player;
        this.name = player.getName();
        this.uniqueId = player.getUniqueId();

        this.team = null;
    }

    public void sendMessage(Component message) {
        this.player.sendMessage(message);
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public boolean isInTeam() {
        return team != null;
    }
}
