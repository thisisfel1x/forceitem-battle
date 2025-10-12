package de.thisisfel1x.forceitembattle.listeners.entity;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;

public class EntityExhaustionListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public EntityExhaustionListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onEntityExhaustion(EntityExhaustionEvent event) {
        if(!(event.getEntity() instanceof Player player)) return;

        event.setCancelled(true);
    }

}
