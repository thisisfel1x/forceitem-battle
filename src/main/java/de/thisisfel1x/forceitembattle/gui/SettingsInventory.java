package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.settings.AbstractSetting;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class SettingsInventory {

    private final ForceItemBattle forceItemBattle;

    public SettingsInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openGui(Player player) {
        SimpleGui gui = new SimpleGui(Component.text("Settings"), 3);

        for (AbstractSetting<?> setting : this.forceItemBattle.getSettingsManager().getSettings()) {
            gui.addItem(setting.getIcon(), event -> {
                if (!(event.getWhoClicked() instanceof Player clicker)) return;
                if (!clicker.hasPermission("forceitembattle.settings")) return;

                setting.handleIconClick(clicker);
                gui.updateItem(event.getSlot(), setting.getIcon());
            });
        }

        gui.open(player);
    }
}
