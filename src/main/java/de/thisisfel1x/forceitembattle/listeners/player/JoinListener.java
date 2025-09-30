package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public JoinListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.joinMessage(Component.text(">> " + player.getName()));

        GamePlayer gamePlayer = new GamePlayer(player);
        this.forceItemBattle.getTeamManager().getGamePlayers().put(player.getUniqueId(), gamePlayer);

        gamePlayer.setLobbyInventory();
    }

}
