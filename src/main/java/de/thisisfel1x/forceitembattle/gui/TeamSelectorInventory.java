package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.Team;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TeamSelectorInventory {

    private final ForceItemBattle forceItemBattle;

    public TeamSelectorInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openGui(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text("Teamauswahl"))
                .rows(1)
                .create();

        for (Team team : this.forceItemBattle.getTeamManager().getTeams()) {
            GuiItem teamItem = ItemBuilder.from(this.getItemStackForTeam(team)).asGuiItem(event -> {
                if (event.getWhoClicked() instanceof Player clickedPlayer) {
                    GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(clickedPlayer.getUniqueId());
                    if (gamePlayer.isInTeam()) {
                        gamePlayer.getTeam().removePlayer(gamePlayer);
                    }

                    team.addPlayer(gamePlayer);
                    clickedPlayer.closeInventory();
                }
            });
            gui.addItem(teamItem);
        }

        gui.open(player);
    }

    private ItemStack getItemStackForTeam(Team team) {
        Component[] lore = new Component[2];
        for (int i = 0; i < team.getMaxTeamSize(); i++) {
            try {
                lore[i] = Component.text("- ", NamedTextColor.DARK_GRAY).append(Component.text(team.getTeamMembers().get(i).getName(), team.getTeamColor())).decoration(TextDecoration.ITALIC, false);
            } catch (IndexOutOfBoundsException ignored) {
                lore[i] = Component.text("- ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
            }
        }

        return ItemBuilder.from(Material.LEATHER_BOOTS)
                .name(Component.text(team.getTeamName(), team.getTeamColor())
                        .append(Component.text(" (" + team.getCurrentTeamSize() + "/" + team.getMaxTeamSize() + ")")).decoration(TextDecoration.ITALIC, false))
                .color(Color.fromRGB(team.getTeamColor().red(), team.getTeamColor().green(), team.getTeamColor().blue()))
                .lore(lore)
                .build();
    }

}
