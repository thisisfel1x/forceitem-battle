package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public InventoryClickListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        GameStateEnum gameStateEnum = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();

        // Outside of the running game nobody moves items; ingame only spectators are locked.
        // Clicks inside plugin GUIs are handled (and cancelled) by the GuiListener
        if (gameStateEnum != GameStateEnum.INGAME || this.forceItemBattle.getTeamManager().isSpectator(player)) {
            event.setCancelled(true);
        }
    }

}
