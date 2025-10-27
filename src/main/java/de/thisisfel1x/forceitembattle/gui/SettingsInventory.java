package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.settings.AbstractSetting;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class SettingsInventory {

    private final ForceItemBattle forceItemBattle;

    public SettingsInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openGui(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text("Settings"))
                .rows(3)
                .create();

        for (AbstractSetting<?> setting : this.forceItemBattle.getSettingsManager().getSettings()) {
            gui.addItem(ItemBuilder.from(setting.getIcon()).asGuiItem(event -> {
                if (!(event.getWhoClicked() instanceof Player)) return;

                setting.handleIconClick(player);
            }));
        }
    }
}
