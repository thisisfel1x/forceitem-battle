package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Keeps spectators from influencing the world. Damage, block changes, drops and inventory
 * clicks are handled in their respective listeners
 */
public class SpectatorListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public SpectatorListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && this.forceItemBattle.getTeamManager().isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && this.forceItemBattle.getTeamManager().isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (this.forceItemBattle.getTeamManager().isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

}
