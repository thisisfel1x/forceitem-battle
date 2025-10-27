package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnCommand implements BasicCommand {

    private final ForceItemBattle forceItemBattle;

    public SpawnCommand(ForceItemBattle forceItemBattle) {
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
            player.sendMessage(forceItemBattle.getPrefix().append(Component.text("Dieser Befehel kann nur während dem Spiel ausgeführt werden!", NamedTextColor.RED)));
            return;
        }

        GamePlayer gamePlayer = forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isSpectator()) {
            player.sendMessage(forceItemBattle.getPrefix().append(Component.text("Du bist kein Spieler!", NamedTextColor.RED)));
            return;
        }

        Location spawnLocation = player.getWorld().getHighestBlockAt(player.getWorld().getSpawnLocation())
                .getLocation().toCenterLocation().add(0, 0.5, 0);
        player.teleport(spawnLocation);

    }
}
