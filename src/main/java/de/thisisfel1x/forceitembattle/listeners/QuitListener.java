package de.thisisfel1x.forceitembattle.listeners;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.Component;
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

        event.quitMessage(Component.text("<< " + player.getName()));

        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer != null) {
            if (gamePlayer.isInTeam()) {
                gamePlayer.getTeam().removePlayer(gamePlayer);
            }
            this.forceItemBattle.getTeamManager().getGamePlayers().remove(player.getUniqueId());
        }
    }

}
