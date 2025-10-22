package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public PlayerMoveListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();

        if (gameStateEnum != GameStateEnum.INGAME) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                player.teleport(location.setDirection(event.getTo().getDirection()));
            }
        }
    }

}
