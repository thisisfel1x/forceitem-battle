package de.thisisfel1x.forceitembattle.game;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Central place for everything that advances a team to its next item: found items
 * (event based or by the periodic inventory scan) and jokers
 */
public class ItemTracker {

    /**
     * Upper bound of items a team can complete within one scan, in case several upcoming
     * items are already in the inventory
     */
    private static final int MAX_CHAIN_PER_SCAN = 10;

    private final ForceItemBattle forceItemBattle;

    public ItemTracker(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    /**
     * Called by events (craft, pick up, inventory click) with the item the player just touched
     */
    public void checkIfItemIsFound(Player player, @Nullable ItemStack itemFound) {
        if (itemFound == null) return;

        ForceItemBattleTeam team = this.getActiveTeam(player);
        if (team == null || !team.hasItemsLeft()) return;

        if (itemFound.getType() == team.getCurrentItem().getType()) {
            this.completeCurrentItem(team);
        }
    }

    /**
     * Checks the inventories of all online players for their team's current item. Catches every
     * way of obtaining an item that does not fire one of the handled events (buckets, trades,
     * items that were already in the inventory, ...)
     */
    public void scanInventories() {
        for (ForceItemBattleTeam team : this.forceItemBattle.getTeamManager().getTeams()) {
            for (int i = 0; i < MAX_CHAIN_PER_SCAN && team.hasItemsLeft(); i++) {
                Material target = team.getCurrentItem().getType();

                boolean found = team.getOnlineMembers().stream().anyMatch(player -> hasItem(player, target));
                if (!found) break;

                this.completeCurrentItem(team);
            }
        }
    }

    private static boolean hasItem(Player player, Material material) {
        // getContents() includes armor and off hand
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == material) {
                return true;
            }
        }
        return player.getItemOnCursor().getType() == material;
    }

    private void completeCurrentItem(ForceItemBattleTeam team) {
        team.advanceToNextItem(false);

        ItemStack nextItem = team.getCurrentItem();
        team.broadcastTeamMessage(Message.ITEM_FOUND,
                Placeholder.component("item", Component.translatable(nextItem.getType().translationKey())));

        team.getTeamAudience().playSound(Sound.sound()
                .type(SoundEventKeys.ENTITY_PLAYER_LEVELUP)
                .build());

        this.refreshTeamDisplays(team);
    }

    /**
     * Skips the team's current item with a joker
     *
     * @param jokerStack the joker stack in the player's hand, one joker is consumed
     * @return {@code true} if a joker was used
     */
    public boolean useJoker(Player player, ItemStack jokerStack) {
        ForceItemBattleTeam team = this.getActiveTeam(player);
        if (team == null || !team.hasItemsLeft()) return false;

        jokerStack.setAmount(jokerStack.getAmount() - 1);

        ItemStack skippedItem = team.getCurrentItem();
        player.getInventory().addItem(skippedItem.clone());

        team.advanceToNextItem(true);
        ItemStack nextItem = team.getCurrentItem();

        TextColor skippedItemColor = NamedTextColor.nearestTo(team.getTeamColor());

        team.broadcastTeamMessage(Message.JOKER_USED,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.component("item", Component.translatable(skippedItem.translationKey()).color(skippedItemColor)));
        team.broadcastTeamMessage(Message.ITEM_NEXT,
                Placeholder.component("item", Component.translatable(nextItem.translationKey())));

        team.getTeamAudience().playSound(Sound.sound()
                .type(SoundEventKeys.ENTITY_VILLAGER_NO)
                .build());

        this.refreshTeamDisplays(team);
        return true;
    }

    private void refreshTeamDisplays(ForceItemBattleTeam team) {
        team.updateBossBar();
        // Only this team's item changed, other teams keep their armor stands
        team.updateArmorstands();
    }

    private @Nullable ForceItemBattleTeam getActiveTeam(Player player) {
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isSpectator() || !gamePlayer.isInTeam()) return null;

        return gamePlayer.getTeam();
    }
}
