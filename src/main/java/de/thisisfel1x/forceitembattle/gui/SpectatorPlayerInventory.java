package de.thisisfel1x.forceitembattle.gui;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.utils.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class SpectatorPlayerInventory {

    private final ForceItemBattle forceItemBattle;

    public SpectatorPlayerInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    public void openSpectatorInventory(Player player) {
        // Only players that are online right now can be watched
        List<GamePlayer> watchablePlayers = this.forceItemBattle.getTeamManager().getGamePlayers().values().stream()
                .filter(gamePlayer -> !gamePlayer.isSpectator() && gamePlayer.isInTeam() && gamePlayer.isOnline())
                .toList();

        SimpleGui gui = new SimpleGui(Component.text("Alle Spieler"), (watchablePlayers.size() + 8) / 9);

        for (GamePlayer gamePlayer : watchablePlayers) {
            Player target = gamePlayer.getPlayer();
            if (target == null) continue;

            ItemStack skull = Items.named(Material.PLAYER_HEAD, target.displayName());
            skull.editMeta(SkullMeta.class, meta -> {
                meta.setOwningPlayer(target);
                meta.lore(List.of(Items.noItalic(
                        Component.text(gamePlayer.getTeam().getTeamName(), gamePlayer.getTeam().getTeamColor()))));
            });

            gui.addItem(skull, event -> {
                player.closeInventory();

                // The target may have left since the menu was opened
                Player currentTarget = gamePlayer.getPlayer();
                if (currentTarget == null) {
                    player.sendMessage(this.forceItemBattle.getPrefix()
                            .append(Component.text("Dieser Spieler ist nicht mehr online.", NamedTextColor.RED)));
                    return;
                }
                player.teleport(currentTarget.getLocation());
            });
        }

        gui.open(player);
    }

}
