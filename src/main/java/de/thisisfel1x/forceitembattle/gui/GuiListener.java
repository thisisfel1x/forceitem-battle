package de.thisisfel1x.forceitembattle.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof SimpleGui gui)) return;

        // Nothing may be moved while a GUI is open (also prevents shift-clicking items into it)
        event.setCancelled(true);

        if (topInventory.equals(event.getClickedInventory())) {
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (SimpleGui.isGui(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
