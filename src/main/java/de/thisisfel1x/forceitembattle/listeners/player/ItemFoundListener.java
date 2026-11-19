package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.gui.SimpleGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Event based item detection for instant feedback. Everything these events miss is caught by
 * the periodic inventory scan of the {@link de.thisisfel1x.forceitembattle.game.ItemTracker}
 */
public class ItemFoundListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public ItemFoundListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    private boolean isIngame() {
        return this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() == GameStateEnum.INGAME;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!this.isIngame()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        this.forceItemBattle.getItemTracker().checkIfItemIsFound(player, event.getRecipe().getResult());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickUp(EntityPickupItemEvent event) {
        if (!this.isIngame()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        this.forceItemBattle.getItemTracker().checkIfItemIsFound(player, event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!this.isIngame()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        // Display items of plugin GUIs (recipe view, spectator menu, ...) never count
        if (SimpleGui.isGui(event.getView().getTopInventory())) return;

        this.forceItemBattle.getItemTracker().checkIfItemIsFound(player, event.getCurrentItem());
    }

    // No ignoreCancelled: right clicks into the air are fired as cancelled by default
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!this.isIngame()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // The event fires once per hand - without this check a single click could burn two jokers
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.BARRIER) return;

        // Never place the barrier, no matter whether the joker can be used
        event.setCancelled(true);

        this.forceItemBattle.getItemTracker().useJoker(player, itemInHand);
    }

}
