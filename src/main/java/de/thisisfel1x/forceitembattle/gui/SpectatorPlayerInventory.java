package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class SpectatorPlayerInventory {

    private final ForceItemBattle forceItemBattle;

    public SpectatorPlayerInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openSpectatorInventory(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text("Alle Spieler"))
                .rows(Math.round(this.forceItemBattle.getTeamManager().getGamePlayers().values()
                        .stream().filter(gamePlayer -> !gamePlayer.isSpectator()
                                && gamePlayer.isActive()).count() / 9f) + 1)
                .create();

        for (GamePlayer gamePlayer : this.forceItemBattle.getTeamManager().getGamePlayers().values()) {
            if (gamePlayer.isSpectator()) continue;

            gui.addItem(ItemBuilder.skull()
                    .owner(gamePlayer.getPlayer())
                    .name(gamePlayer.getPlayer().displayName().decoration(TextDecoration.ITALIC, false))
                    .lore(Component.text(gamePlayer.getTeam().getTeamName(), gamePlayer.getTeam().getTeamColor())
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        if(!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) {
                            return;
                        }
                        player.closeInventory();
                        player.teleport(gamePlayer.getPlayer().getLocation());
                    }));
        }

        gui.open(player);
    }

}
