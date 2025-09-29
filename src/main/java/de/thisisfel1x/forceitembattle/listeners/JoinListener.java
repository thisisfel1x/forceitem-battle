package de.thisisfel1x.forceitembattle.listeners;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.Team;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class JoinListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public JoinListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    private Gui gui;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.joinMessage(Component.text(">> " + player.getName()));

        GamePlayer gamePlayer = new GamePlayer(player);
        this.forceItemBattle.getTeamManager().getGamePlayers().put(player.getUniqueId(), gamePlayer);

        Bukkit.getScheduler().runTaskLater(this.forceItemBattle, () ->  {

            this.gui = Gui.gui().title(Component.text("Teamauswahl")).rows(1).create();

            for (Team team : this.forceItemBattle.getTeamManager().getTeams()) {
                Component[] lore = new Component[2];
                for (int i = 0; i < team.getMaxTeamSize(); i++) {
                    try {
                        lore[i] = Component.text("- ", NamedTextColor.DARK_GRAY).append(Component.text(team.getTeamMembers().get(i).getName(), team.getTeamColor())).decoration(TextDecoration.ITALIC, false);
                    } catch (IndexOutOfBoundsException ignored) {
                        lore[i] = Component.text("- ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
                    }

                }

                GuiItem teamItem = ItemBuilder.from(Material.LEATHER_BOOTS)
                        .name(Component.text(team.getTeamName(), team.getTeamColor())
                                .append(Component.text(" (" + team.getCurrentTeamSize() + "/" + team.getMaxTeamSize() + ")")).decoration(TextDecoration.ITALIC, false))
                        .color(Color.fromRGB(team.getTeamColor().red(), team.getTeamColor().green(), team.getTeamColor().blue()))
                        .lore(lore)
                        .asGuiItem(event0 -> {
                            team.addPlayer(gamePlayer);
                            event0.getWhoClicked().closeInventory();

                            for (Team team1 : this.forceItemBattle.getTeamManager().getTeams()) {
                                System.out.println(team1);
                            }
                });

                gui.addItem(teamItem);
            }

            gui.open(player);

        }, 40L);
    }

}
