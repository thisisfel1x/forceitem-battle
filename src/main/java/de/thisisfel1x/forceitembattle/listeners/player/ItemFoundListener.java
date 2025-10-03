package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemFoundListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public ItemFoundListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack craftedItem = event.getRecipe().getResult();

        checkIfItemIsFound(player, craftedItem);
    }

    @EventHandler
    public void onPickUp(EntityPickupItemEvent event) {
        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) return;
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack craftedItem = event.getItem().getItemStack();

        checkIfItemIsFound(player, craftedItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Rezept für:")) {
            event.setCancelled(true);
            return;
        }
        ItemStack craftedItem = event.getCurrentItem();

        checkIfItemIsFound(player, craftedItem);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.BARRIER) return;

        // Get the GamePlayer and their team
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || !gamePlayer.isInTeam() || gamePlayer.isSpectator()) return;

        // If all checks pass, we can proceed with the skip logic.
        event.setCancelled(true); // Prevents the barrier from doing anything else
        ForceItemBattleTeam team = gamePlayer.getTeam();

        // Consume one barrier item from the player's hand
        itemInHand.setAmount(itemInHand.getAmount() - 1);

        // Get the item that was skipped for the message
        ItemStack skippedItem = team.getCurrentItem();
        // Add Item to Inv
        player.getInventory().addItem(skippedItem);

        TextColor skippedItemColor = NamedTextColor.nearestTo(team.getTeamColor()); // Use team color for flair

        // Advance the team's progress, marking it as a joker-skip (true)
        team.advanceToNextItem(true);

        // Get the new item to find
        ItemStack nextItem = team.getCurrentItem();

        // Create and send a feedback message to the entire team
        Component jokerMessage = this.forceItemBattle.getPrefix()
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" hat einen Joker für ", NamedTextColor.GRAY))
                .append(Component.translatable(skippedItem.translationKey()).color(skippedItemColor))
                .append(Component.text(" eingesetzt!", NamedTextColor.GRAY));

        Component nextItemMessage = this.forceItemBattle.getPrefix()
                .append(Component.text("Nächstes Item: ", NamedTextColor.GRAY))
                .append(Component.translatable(nextItem.translationKey()).color(NamedTextColor.YELLOW));

        team.broadcastTeamMessage(jokerMessage);
        team.broadcastTeamMessage(nextItemMessage);

        // Play sound
        team.getTeamAudience().playSound(Sound.sound()
                .type(SoundEventKeys.ENTITY_VILLAGER_NO)
                .build());

        // Update boss bar
        team.updateBossBar();
    }

    private void checkIfItemIsFound(Player player, ItemStack itemFound) {
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || !gamePlayer.isInTeam() || gamePlayer.isSpectator()) return;

        ForceItemBattleTeam team = gamePlayer.getTeam();
        ItemStack currentTargetItem = team.getCurrentItem();

        if (itemFound.getType() == currentTargetItem.getType()) {
            team.advanceToNextItem(false);

            // Feedback an das Team
            ItemStack nextItem = team.getCurrentItem();
            team.broadcastTeamMessage(Component.text("Item gefunden! ", NamedTextColor.GREEN)
                    .append(Component.text("Nächstes Item: ", NamedTextColor.GRAY))
                    .append(Component.translatable(nextItem.getType().translationKey(), NamedTextColor.YELLOW)));

            // Play sound
            team.getTeamAudience().playSound(Sound.sound()
                    .type(SoundEventKeys.ENTITY_PLAYER_LEVELUP)
                    .build());

            // Update boss bar
            team.updateBossBar();
        }
    }

}
