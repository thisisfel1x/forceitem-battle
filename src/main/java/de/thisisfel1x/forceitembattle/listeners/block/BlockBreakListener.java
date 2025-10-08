package de.thisisfel1x.forceitembattle.listeners.block;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public BlockBreakListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();
        if (gameStateEnum != GameStateEnum.INGAME) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

        if (gamePlayer.isSpectator()) {
            event.setCancelled(true);
            return;
        }
    }

}
