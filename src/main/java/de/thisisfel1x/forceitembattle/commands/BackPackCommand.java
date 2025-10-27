package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class BackPackCommand implements BasicCommand {

    private final ForceItemBattle forceItemBattle;

    public BackPackCommand(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Dieser Befehl ist nur für Spieler.", NamedTextColor.RED));
            return;
        }

        GameManager gameManager = forceItemBattle.getGameManager();
        if (gameManager.getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) {
            player.sendMessage(forceItemBattle.getPrefix().append(Component.text("Du kannst dein Backpack nur während dem Spiel öffnen!", NamedTextColor.RED)));
            return;
        }

        if ((boolean) this.forceItemBattle.getSettingsManager().getSetting("BACKPACK").getValue()) {
            player.sendMessage(forceItemBattle.getPrefix().append(Component.text("Der Backpack ist deaktiviert", NamedTextColor.RED)));
            return;
        }

        GamePlayer gamePlayer = forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || !gamePlayer.isInTeam()) {
            player.sendMessage(forceItemBattle.getPrefix().append(Component.text("Du bist in keinem Team!", NamedTextColor.RED)));
            return;
        }

        player.openInventory(gamePlayer.getTeam().getBackpack());
    }
}
