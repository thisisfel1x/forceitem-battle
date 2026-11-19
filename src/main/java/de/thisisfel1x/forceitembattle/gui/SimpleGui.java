package de.thisisfel1x.forceitembattle.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal read-only chest GUI. Every click inside a view of this GUI is cancelled by
 * {@link GuiListener}; slots may carry an optional click action
 */
public class SimpleGui implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public SimpleGui(Component title, int rows) {
        int clampedRows = Math.max(1, Math.min(6, rows));
        this.inventory = Bukkit.createInventory(this, clampedRows * 9, title);
    }

    public void setItem(int slot, @Nullable ItemStack item) {
        this.setItem(slot, item, null);
    }

    public void setItem(int slot, @Nullable ItemStack item, @Nullable Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= this.inventory.getSize()) return;

        this.inventory.setItem(slot, item);
        if (action != null) {
            this.actions.put(slot, action);
        } else {
            this.actions.remove(slot);
        }
    }

    /**
     * Puts the item into the first empty slot
     *
     * @return the used slot or -1 if the GUI is full
     */
    public int addItem(ItemStack item, @Nullable Consumer<InventoryClickEvent> action) {
        int slot = this.inventory.firstEmpty();
        if (slot == -1) return -1;

        this.setItem(slot, item, action);
        return slot;
    }

    /**
     * Replaces the item of a slot while keeping its click action
     */
    public void updateItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= this.inventory.getSize()) return;
        this.inventory.setItem(slot, item);
    }

    public void fill(ItemStack item) {
        for (int slot = 0; slot < this.inventory.getSize(); slot++) {
            this.setItem(slot, item);
        }
    }

    void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = this.actions.get(event.getSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    public void open(Player player) {
        player.openInventory(this.inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public static boolean isGui(@Nullable Inventory inventory) {
        return inventory != null && inventory.getHolder(false) instanceof SimpleGui;
    }
}
