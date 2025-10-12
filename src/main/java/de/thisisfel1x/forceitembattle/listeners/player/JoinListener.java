package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.game.states.IdleState;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final ForceItemBattle forceItemBattle;

    public JoinListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameStateEnum currentGameState = this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum();

        event.joinMessage(this.forceItemBattle.getPrefix()
                .append(Component.text(player.getName() + " hat das Spiel betreten", NamedTextColor.GRAY)));

        if (currentGameState == GameStateEnum.IDLE) {
            GamePlayer gamePlayer = new GamePlayer(player);
            this.forceItemBattle.getTeamManager().getGamePlayers().put(player.getUniqueId(), gamePlayer);

            gamePlayer.cleanOnJoin();
            gamePlayer.setLobbyInventory();

            Bukkit.getScheduler().runTask(this.forceItemBattle, () -> {
                ((IdleState) this.forceItemBattle.getGameManager().getCurrentGameState()).teleportPlayerToNextSpawn(player);
            });
        } else {
            // First, check if GamePlayer exists
            GamePlayer foundGamePlayer =  this.forceItemBattle.getTeamManager().getGamePlayers().get(player.getUniqueId());

            if (foundGamePlayer != null) {
                // GamePlayer exists -> REJOIN
                // TODO
                player.showBossBar(foundGamePlayer.getTeam().getBossBar());
            } else {
                // GamePlayer doesnt exists -> Spectator
                GamePlayer gamePlayer = new GamePlayer(player);
                gamePlayer.setSpectator(true);
                gamePlayer.cleanOnJoin();
                gamePlayer.setSpectatorInventory();

                player.displayName(player.displayName().decoration(TextDecoration.ITALIC, true));

                this.forceItemBattle.getTeamManager().getGamePlayers().put(player.getUniqueId(), gamePlayer);

                this.forceItemBattle.getForceItemBattleScoreboardManager().setPlayerSpectator(gamePlayer);
            }
        }

        this.forceItemBattle.getForceItemBattleScoreboardManager().handlePlayerJoin(player);
    }

}
