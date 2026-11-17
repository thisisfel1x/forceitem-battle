package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.gui.SimpleGui;
import de.thisisfel1x.forceitembattle.utils.Items;
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
        if (gamePlayer == null || gamePlayer.isSpectator() || !gamePlayer.isInTeam()) {
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

        // Bevorzuge ein Rezept für die Werkbank, da nur diese angezeigt werden können
        Recipe recipe = recipes.stream()
                .filter(candidate -> candidate instanceof ShapedRecipe || candidate instanceof ShapelessRecipe)
                .findFirst()
                .orElse(recipes.getFirst());
        openRecipeInventory(player, targetItem, recipe);
    }

    private void openRecipeInventory(Player player, ItemStack targetItem, Recipe recipe) {
        Component title = Component.text("Rezept für: ").append(Component.translatable(targetItem.translationKey()));
        SimpleGui recipeView = new SimpleGui(title, 6);

        // Fülle den Hintergrund mit Platzhaltern
        recipeView.fill(Items.named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" ")));

        // Setze das Ergebnis-Item und einen Pfeil
        recipeView.setItem(23, Items.named(Material.ARROW, Component.text("->")));
        recipeView.setItem(25, targetItem.clone());

        // Definiere die Slots für das 3x3 Crafting-Feld
        int[] craftingSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};
        for (int slot : craftingSlots) {
            recipeView.setItem(slot, null);
        }

        if (recipe instanceof ShapedRecipe shapedRecipe) {
            Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
            String[] shape = shapedRecipe.getShape();
            for (int row = 0; row < shape.length && row < 3; row++) {
                for (int column = 0; column < shape[row].length() && column < 3; column++) {
                    recipeView.setItem(craftingSlots[row * 3 + column], toDisplayItem(choiceMap.get(shape[row].charAt(column))));
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            List<RecipeChoice> choices = shapelessRecipe.getChoiceList();
            for (int i = 0; i < choices.size() && i < craftingSlots.length; i++) {
                recipeView.setItem(craftingSlots[i], toDisplayItem(choices.get(i)));
            }
        } else {
            // Für andere Rezept-Typen wie Schmelzen, Schmieden etc.
            player.sendMessage(plugin.getPrefix().append(Component.text("Dieser Rezept-Typ kann nicht angezeigt werden.", NamedTextColor.YELLOW)));
            return;
        }
        recipeView.open(player);
    }

    /**
     * Picks one representative item for an ingredient (e.g. the first plank type for "any planks").
     */
    private static @Nullable ItemStack toDisplayItem(@Nullable RecipeChoice choice) {
        return switch (choice) {
            case RecipeChoice.MaterialChoice materialChoice when !materialChoice.getChoices().isEmpty() ->
                    new ItemStack(materialChoice.getChoices().getFirst());
            case RecipeChoice.ExactChoice exactChoice when !exactChoice.getChoices().isEmpty() ->
                    exactChoice.getChoices().getFirst().clone();
            case null, default -> null;
        };
    }

    @Override
    public @Nullable String permission() {
        return null; // Keine spezielle Permission benötigt
    }
}