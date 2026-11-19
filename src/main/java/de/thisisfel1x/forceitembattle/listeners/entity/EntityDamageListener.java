package de.thisisfel1x.forceitembattle.listeners.entity;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.settings.SettingType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public EntityDamageListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Spectators must not hurt anything (players, mobs, armor stands, ...)
        if (event instanceof EntityDamageByEntityEvent byEntityEvent && this.isSpectatorAttack(byEntityEvent)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player player)) return;

        // Players are invulnerable outside of the running game, spectators always
        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();
        if (gameStateEnum != GameStateEnum.INGAME || this.forceItemBattle.getTeamManager().isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        // Setting "No damage": the void and /kill still kill - otherwise a player would fall forever
        if (this.forceItemBattle.getSettingsManager().isEnabled(SettingType.NO_DAMAGE)
                && event.getCause() != EntityDamageEvent.DamageCause.VOID
                && event.getCause() != EntityDamageEvent.DamageCause.KILL) {
            event.setCancelled(true);
        }
    }

    private boolean isSpectatorAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            return this.forceItemBattle.getTeamManager().isSpectator(damager);
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return this.forceItemBattle.getTeamManager().isSpectator(shooter);
        }
        return false;
    }

}
