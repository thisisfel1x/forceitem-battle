package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.settings.SettingType;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class BackPackCommand implements BasicCommand {

    private final ForceItemBattle forceItemBattle;

    public BackPackCommand(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            this.forceItemBattle.getLanguageManager().send(source.getSender(), Message.COMMAND_PLAYERS_ONLY);
            return;
        }

        GameManager gameManager = forceItemBattle.getGameManager();
        if (gameManager.getCurrentGameState().getGameStateEnum() != GameStateEnum.INGAME) {
            this.forceItemBattle.getLanguageManager().send(player, Message.COMMAND_ONLY_INGAME);
            return;
        }

        if (!this.forceItemBattle.getSettingsManager().isEnabled(SettingType.BACKPACK)) {
            this.forceItemBattle.getLanguageManager().send(player, Message.BACKPACK_DISABLED);
            return;
        }

        GamePlayer gamePlayer = forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isSpectator() || !gamePlayer.isInTeam()) {
            this.forceItemBattle.getLanguageManager().send(player, Message.COMMAND_NOT_IN_TEAM);
            return;
        }

        player.openInventory(gamePlayer.getTeam().getBackpack());
    }
}
