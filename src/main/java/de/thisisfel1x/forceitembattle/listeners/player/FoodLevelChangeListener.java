package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodLevelChangeListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public FoodLevelChangeListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
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
