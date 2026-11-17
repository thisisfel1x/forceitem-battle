package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.utils.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

public class TeamSelectorInventory {

    private final ForceItemBattle forceItemBattle;

    public TeamSelectorInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openGui(Player player) {
        List<ForceItemBattleTeam> teams = this.forceItemBattle.getTeamManager().getTeams();
        SimpleGui gui = new SimpleGui(Component.text("Teamauswahl"), (teams.size() + 8) / 9);

        for (ForceItemBattleTeam forceItemBattleTeam : teams) {
            gui.addItem(this.getItemStackForTeam(forceItemBattleTeam), event -> {
                if (!(event.getWhoClicked() instanceof Player clickedPlayer)) return;

                GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(clickedPlayer.getUniqueId());
                if (gamePlayer == null || gamePlayer.isSpectator()) return;

                ForceItemBattleTeam previousTeam = gamePlayer.getTeam();
                if (previousTeam == forceItemBattleTeam) {
                    clickedPlayer.closeInventory();
                    return;
                }

                // Check first: a full team must not cost the player the old team
                if (forceItemBattleTeam.isFull()) {
                    clickedPlayer.sendMessage(this.forceItemBattle.getPrefix()
                            .append(Component.text("Dieses Team ist bereits voll", NamedTextColor.RED)));
                    return;
                }

                if (previousTeam != null) {
                    previousTeam.removePlayer(gamePlayer);
                }

                forceItemBattleTeam.addPlayer(gamePlayer);
                clickedPlayer.closeInventory();
            });
        }

        gui.open(player);
    }

    private ItemStack getItemStackForTeam(ForceItemBattleTeam forceItemBattleTeam) {
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < forceItemBattleTeam.getMaxTeamSize(); i++) {
            Component line = Component.text("- ", NamedTextColor.DARK_GRAY);
            if (i < forceItemBattleTeam.getTeamMembers().size()) {
                line = line.append(Component.text(forceItemBattleTeam.getTeamMembers().get(i).getName(),
                        forceItemBattleTeam.getTeamColor()));
            }
            lore.add(Items.noItalic(line));
        }

        Component name = Component.text(forceItemBattleTeam.getTeamName(), forceItemBattleTeam.getTeamColor())
                .append(Component.text(" (" + forceItemBattleTeam.getCurrentTeamSize() + "/" + forceItemBattleTeam.getMaxTeamSize() + ")"));

        ItemStack itemStack = Items.named(Material.LEATHER_BOOTS, name);
        itemStack.editMeta(LeatherArmorMeta.class, meta -> {
            meta.setColor(Color.fromRGB(forceItemBattleTeam.getTeamColor().red(),
                    forceItemBattleTeam.getTeamColor().green(), forceItemBattleTeam.getTeamColor().blue()));
            meta.addItemFlags(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
            meta.lore(lore);
        });
        return itemStack;
    }

}
