package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TopCommand implements BasicCommand {

    private final ForceItemBattle forceItemBattle;

    public TopCommand(ForceItemBattle forceItemBattle) {
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

        GamePlayer gamePlayer = forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isSpectator()) {
            this.forceItemBattle.getLanguageManager().send(player, Message.COMMAND_NOT_A_PLAYER);
            return;
        }

        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            this.forceItemBattle.getLanguageManager().send(player, Message.COMMAND_TOP_OVERWORLD_ONLY);
        } else {
            Location topLocation = player.getWorld().getHighestBlockAt(player.getLocation())
                    .getLocation().toCenterLocation().add(0, 0.5, 0);
            player.teleport(topLocation);
        }


    }
}
