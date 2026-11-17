package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class InteractListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public InteractListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();
        boolean spectator = this.forceItemBattle.getTeamManager().isSpectator(player);

        // Spectators never interact with the world (buttons, chests, pressure plates, ...)
        if (spectator) {
            event.setCancelled(true);
        }

        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        // The event fires once per hand, only react to the main hand
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.hasItem()) return;

        if (gameStateEnum == GameStateEnum.IDLE) {
            if (event.getMaterial() == Material.AMETHYST_SHARD) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1, 1);
                this.forceItemBattle.getTeamSelectorInventory().openGui(player);
            } else if (event.getMaterial() == Material.PAPER && player.hasPermission("forceitembattle.settings")) {
                this.forceItemBattle.getSettingsInventory().openGui(player);
            }
        } else if (gameStateEnum == GameStateEnum.INGAME) {
            if (spectator && event.getMaterial() == Material.COMPASS) {
                this.forceItemBattle.getSpectatorPlayerInventory().openSpectatorInventory(player);
            }
        }
    }

}
