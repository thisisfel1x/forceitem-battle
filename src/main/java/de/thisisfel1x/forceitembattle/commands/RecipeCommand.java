package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class RecipeCommand implements BasicCommand {

    private final ForceItemBattle plugin;

    public RecipeCommand(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Dieser Befehl ist nur für Spieler.", NamedTextColor.RED));
            return;
        }

        GameManager gameManager = plugin.getGameManager();
        if (gameManager.getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) {
            player.sendMessage(plugin.getPrefix().append(Component.text("Du kannst Rezepte nur während des Spiels ansehen!", NamedTextColor.RED)));
            return;
        }

        GamePlayer gamePlayer = plugin.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || !gamePlayer.isInTeam()) {
            player.sendMessage(plugin.getPrefix().append(Component.text("Du bist in keinem Team!", NamedTextColor.RED)));
            return;
        }

        ForceItemBattleTeam team = gamePlayer.getTeam();
        ItemStack targetItem = team.getCurrentItem();
        List<Recipe> recipes = Bukkit.getRecipesFor(targetItem);

        if (recipes.isEmpty()) {
            player.sendMessage(plugin.getPrefix().append(Component.text("Für dieses Item gibt es kein Herstellungsrezept.", NamedTextColor.YELLOW)));
            return;
        }

        Recipe recipe = recipes.get(0); // Wir nehmen das erste gefundene Rezept
        openRecipeInventory(player, targetItem, recipe);
    }

    private void openRecipeInventory(Player player, ItemStack targetItem, Recipe recipe) {
        Component title = Component.text("Rezept für: ").append(Component.translatable(targetItem.translationKey()));
        Inventory recipeView = Bukkit.createInventory(null, 54, title);

        // Fülle den Hintergrund mit Platzhaltern
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        placeholder.editMeta(meta -> meta.displayName(Component.text(" ")));
        for (int i = 0; i < recipeView.getSize(); i++) {
            recipeView.setItem(i, placeholder);
        }

        // Setze das Ergebnis-Item und einen Pfeil
        recipeView.setItem(23, ItemBuilder.from(Material.ARROW).name(Component.text("->")).build());
        recipeView.setItem(25, targetItem);

        // Definiere die Slots für das 3x3 Crafting-Feld
        int[] craftingSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};

        if (recipe instanceof ShapedRecipe shapedRecipe) {
            Map<Character, ItemStack> ingredientMap = shapedRecipe.getIngredientMap();
            String[] shape = shapedRecipe.getShape();
            int slotIndex = 0;
            for (String row : shape) {
                for (char ingredientChar : row.toCharArray()) {
                    recipeView.setItem(craftingSlots[slotIndex], ingredientMap.get(ingredientChar));
                    slotIndex++;
                }
                slotIndex += (3 - row.length()); // Fülle leere Spalten in der Reihe auf
            }
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
            for (int i = 0; i < ingredients.size(); i++) {
                if(i < craftingSlots.length) {
                    recipeView.setItem(craftingSlots[i], ingredients.get(i));
                }
            }
        } else {
            // Für andere Rezept-Typen wie Schmelzen, Schmieden etc.
            player.sendMessage(plugin.getPrefix().append(Component.text("Dieser Rezept-Typ kann nicht angezeigt werden.", NamedTextColor.YELLOW)));
            return;
        }
        player.openInventory(recipeView);
    }

    @Override
    public @Nullable String permission() {
        return null; // Keine spezielle Permission benötigt
    }
}