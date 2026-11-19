package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public QuitListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

        GameStateEnum currentGameState = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();

        // Every player gets the message in its own language (the leaving player is still online)
        event.quitMessage(null);
        this.forceItemBattle.getLanguageManager().broadcast(Message.PLAYER_LEFT,
                Placeholder.unparsed("player", player.getName()));

        if (gamePlayer != null) {
            if (currentGameState == GameStateEnum.IDLE) {
                if (gamePlayer.isInTeam()) {
                    gamePlayer.getTeam().removePlayer(gamePlayer);
                }
                this.forceItemBattle.getTeamManager().getGamePlayers().remove(player.getUniqueId());
            } else if (gamePlayer.isSpectator()) {
                this.forceItemBattle.getForceItemBattleScoreboardManager().removePlayerFromTeam(gamePlayer);
                this.forceItemBattle.getTeamManager().getGamePlayers().remove(player.getUniqueId());
            } else {
                // Running game: keep the GamePlayer (and its team) so the player can rejoin,
                // but do not leave the item armor stand behind in the world
                gamePlayer.removeArmorstand();
            }
        }

        this.forceItemBattle.getForceItemBattleScoreboardManager().handlePlayerQuit(player);
    }

}
