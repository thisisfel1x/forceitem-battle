package de.thisisfel1x.forceitembattle.listeners.entity;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.settings.SettingType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;

/**
 * Exhaustion drains saturation and then the food bar. It is only allowed for active players during the game and
 * only if the setting "No hunger" is off - otherwise the food bar stays full
 */
public class EntityExhaustionListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public EntityExhaustionListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onEntityExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (this.forceItemBattle.getSettingsManager().isEnabled(SettingType.NO_HUNGER)
                || this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) {
            event.setCancelled(true);
            return;
        }

        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isSpectator()) {
            event.setCancelled(true);
        }
    }

}
