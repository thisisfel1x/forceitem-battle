package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public QuitListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

        GameStateEnum currentGameState = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();

        event.quitMessage(this.forceItemBattle.getPrefix()
                .append(Component.text(player.getName() + " hat das Spiel verlassen", NamedTextColor.GRAY)));

        if (currentGameState == GameStateEnum.IDLE) {
            // Cleanup GamePlayer
            if (gamePlayer.isInTeam()) {
                gamePlayer.getTeam().removePlayer(gamePlayer);
            }

            // Finally, remove GamePlayer from HashMap
            this.forceItemBattle.getTeamManager().getGamePlayers().remove(player.getUniqueId());
        } else {
            // We are ingame. To allow rejoining, we don't want to remove non-spectator gameplayers
            if (gamePlayer.isSpectator()) {
                this.forceItemBattle.getTeamManager().getGamePlayers().remove(player.getUniqueId());
            } else {
                // TODO do nothing for now
            }
        }
    }

}
