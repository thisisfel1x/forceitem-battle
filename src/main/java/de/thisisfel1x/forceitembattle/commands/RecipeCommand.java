package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.gui.SimpleGui;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.utils.Items;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
            this.plugin.getLanguageManager().send(source.getSender(), Message.COMMAND_PLAYERS_ONLY);
            return;
        }

        GameManager gameManager = plugin.getGameManager();
        if (gameManager.getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) {
            this.plugin.getLanguageManager().send(player, Message.COMMAND_ONLY_INGAME);
            return;
        }

        GamePlayer gamePlayer = plugin.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isSpectator() || !gamePlayer.isInTeam()) {
            this.plugin.getLanguageManager().send(player, Message.COMMAND_NOT_IN_TEAM);
            return;
        }

        ForceItemBattleTeam team = gamePlayer.getTeam();
        ItemStack targetItem = team.getCurrentItem();
        List<Recipe> recipes = Bukkit.getRecipesFor(targetItem);

        if (recipes.isEmpty()) {
            this.plugin.getLanguageManager().send(player, Message.RECIPE_NONE);
            return;
        }

        // Prefer a crafting table recipe, only those can be shown
        Recipe recipe = recipes.stream()
                .filter(candidate -> candidate instanceof ShapedRecipe || candidate instanceof ShapelessRecipe)
                .findFirst()
                .orElse(recipes.getFirst());
        openRecipeInventory(player, targetItem, recipe);
    }

    private void openRecipeInventory(Player player, ItemStack targetItem, Recipe recipe) {
        Component title = this.plugin.getLanguageManager().render(player, Message.RECIPE_TITLE,
                Placeholder.component("item", Component.translatable(targetItem.translationKey())));
        SimpleGui recipeView = new SimpleGui(title, 6);

        // Background
        recipeView.fill(Items.named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" ")));

        // Result and an arrow
        recipeView.setItem(23, Items.named(Material.ARROW, Component.text("->")));
        recipeView.setItem(25, targetItem.clone());

        // 3x3 crafting grid
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
            // Smelting, smithing, ...
            this.plugin.getLanguageManager().send(player, Message.RECIPE_UNSUPPORTED);
            return;
        }
        recipeView.open(player);
    }

    /**
     * Picks one representative item for an ingredient (e.g. the first plank type for "any planks")
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
        return null; // Everybody
    }
}