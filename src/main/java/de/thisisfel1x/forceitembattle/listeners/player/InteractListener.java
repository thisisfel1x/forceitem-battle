package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public InteractListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (!event.hasItem()) return;

        Player player = event.getPlayer();
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();

        if (gameStateEnum == GameStateEnum.IDLE) {
            if (event.getMaterial() == Material.AMETHYST_SHARD) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1, 1);
                this.forceItemBattle.getTeamSelectorInventory().openGui(player);
            }
        } else if (gameStateEnum == GameStateEnum.INGAME) {
            if (!gamePlayer.isSpectator()) return;
            if (event.getMaterial() == Material.COMPASS) {
                this.forceItemBattle.getSpectatorPlayerInventory().openSpectatorInventory(player);
            }
        }


    }

}
