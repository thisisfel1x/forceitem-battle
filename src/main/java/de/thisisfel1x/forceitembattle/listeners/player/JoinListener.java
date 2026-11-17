package de.thisisfel1x.forceitembattle.listeners.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
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

        this.forceItemBattle.getResourcePackService().sendTo(player);

        // Register the sidebar/team view first, the code below may already change scoreboard teams
        this.forceItemBattle.getForceItemBattleScoreboardManager().handlePlayerJoin(player);

        if (currentGameState == GameStateEnum.IDLE) {
            GamePlayer gamePlayer = new GamePlayer(player);
            this.forceItemBattle.getTeamManager().getGamePlayers().put(player.getUniqueId(), gamePlayer);

            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.displayName(null);

            gamePlayer.cleanOnJoin();
            gamePlayer.setLobbyInventory();
            gamePlayer.freezePlayer();
            return;
        }

        GamePlayer existingGamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

        if (existingGamePlayer != null && !existingGamePlayer.isSpectator() && existingGamePlayer.isInTeam()) {
            this.handleRejoin(player, existingGamePlayer, currentGameState);
        } else {
            this.handleSpectatorJoin(player);
        }
    }

    /**
     * A player of the running game came back: restore everything that is bound to the session.
     */
    private void handleRejoin(Player player, GamePlayer gamePlayer, GameStateEnum currentGameState) {
        this.forceItemBattle.getTeamManager().hideSpectatorsFrom(player);
        this.forceItemBattle.getForceItemBattleScoreboardManager().addPlayerToTeam(gamePlayer);

        if (currentGameState != GameStateEnum.RESULTS) {
            player.showBossBar(gamePlayer.getTeam().getBossBar());
        }

        if (currentGameState == GameStateEnum.INGAME) {
            gamePlayer.unfreezePlayer();
            gamePlayer.updateArmorstandItem();
        } else {
            // STARTING (countdown) or RESULTS: nobody moves
            gamePlayer.freezePlayer();
        }

        player.sendMessage(this.forceItemBattle.getPrefix()
                .append(Component.text("Willkommen zurück! Du bist wieder in ", NamedTextColor.GRAY))
                .append(Component.text(gamePlayer.getTeam().getTeamName(), gamePlayer.getTeam().getTeamColor()))
                .append(Component.text(".", NamedTextColor.GRAY)));
    }

    private void handleSpectatorJoin(Player player) {
        GamePlayer gamePlayer = new GamePlayer(player);
        gamePlayer.setSpectator(true);
        this.forceItemBattle.getTeamManager().getGamePlayers().put(player.getUniqueId(), gamePlayer);

        gamePlayer.cleanOnJoin();
        gamePlayer.setSpectatorInventory();

        player.displayName(player.name().decoration(TextDecoration.ITALIC, true));

        this.forceItemBattle.getForceItemBattleScoreboardManager().setPlayerSpectator(gamePlayer);
    }

}
