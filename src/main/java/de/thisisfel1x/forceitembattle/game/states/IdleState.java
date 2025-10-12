package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.utils.LocationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class IdleState extends GameState {

    private int taskId;
    private final ForceItemBattle forceItemBattle;

    private List<Location> lobbySpawns;
    private int nextSpawnIndex = 0;

    private int animationTick = 0;

    public IdleState(GameManager gameManager) {
        super(gameManager);

        this.forceItemBattle = ForceItemBattle.getInstance();

        Location worldSpawnPoint = new Location(Bukkit.getWorld("world"), 0, 80, 0);
        int maxPlayers = this.forceItemBattle.getTeamManager().getTeams().size() * 2; // todo hardcoded team size
        double radius = 10;

        this.lobbySpawns = LocationUtil.calculateCircleSpawns(worldSpawnPoint, maxPlayers, radius);
        this.nextSpawnIndex = 0;
    }

    @Override
    public void onEnter() {

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            int dotCount = (this.animationTick % 4);
            String dots = ".".repeat(dotCount);
            Component actionBarMessage = Component.text("Warten auf weitere Spieler" + dots,
                    NamedTextColor.WHITE);
            this.forceItemBattle.getServer().sendActionBar(actionBarMessage);
            this.animationTick++;
        }, 0L, 20L);

    }

    @Override
    public void onLeave() {
        this.forceItemBattle.getServer().getScheduler().cancelTask(this.taskId);
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.IDLE;
    }

    public void teleportPlayerToNextSpawn(Player player) {
        if (lobbySpawns.isEmpty()) {
            player.sendMessage(Component.text("Fehler: Keine Lobby-Spawnpunkte definiert!", NamedTextColor.RED));
            return;
        }

        // Get the next spawn location from the list
        Location spawnLocation = lobbySpawns.get(nextSpawnIndex);
        player.teleport(spawnLocation);

        // Increment the index and loop back to the start if we reach the end
        nextSpawnIndex = (nextSpawnIndex + 1) % lobbySpawns.size();
    }
}
