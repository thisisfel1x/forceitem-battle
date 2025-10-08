package de.thisisfel1x.forceitembattle.listeners.entity;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public EntityDamageListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();
        if (gameStateEnum != GameStateEnum.INGAME) {
            event.setCancelled(true);
            return;
        }

        Player player = (Player) event.getEntity();
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

        if (gamePlayer.isSpectator()) {
            event.setCancelled(true);
            return;
        }

    }

}
