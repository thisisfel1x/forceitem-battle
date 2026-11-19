package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.settings.AbstractSetting;
import org.bukkit.entity.Player;

public class SettingsInventory {

    private final ForceItemBattle forceItemBattle;

    public SettingsInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openGui(Player player) {
        SimpleGui gui = new SimpleGui(this.forceItemBattle.getLanguageManager().render(player, Message.SETTINGS_GUI_TITLE), 3);

        for (AbstractSetting<?> setting : this.forceItemBattle.getSettingsManager().getSettings()) {
            gui.addItem(setting.getIcon(player), event -> {
                if (!(event.getWhoClicked() instanceof Player clicker)) return;
                if (!clicker.hasPermission("forceitembattle.settings")) return;

                setting.handleIconClick(clicker);
                gui.updateItem(event.getSlot(), setting.getIcon(clicker));
            });
        }

        gui.open(player);
    }
}
