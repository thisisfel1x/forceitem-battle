package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class IngameState extends GameState {

    private int taskId;
    private int timeLeftInSeconds;
    private final ForceItemBattle forceItemBattle;

    public IngameState(GameManager gameManager) {
        super(gameManager);
        this.forceItemBattle = ForceItemBattle.getInstance();
        this.timeLeftInSeconds = this.forceItemBattle.getGameTime() * 60;
    }

    @Override
    public void onEnter() {
        this.forceItemBattle.getTeamManager().getTeams().forEach(ForceItemBattleTeam::updateBossBar);

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            if (this.timeLeftInSeconds <= 0) {
                // TODO
                this.forceItemBattle.getServer().getScheduler().cancelTask(this.taskId);

                ResultsState resultsState = new ResultsState(this.forceItemBattle.getGameManager());
                this.forceItemBattle.getGameManager().setCurrentGameState(resultsState);
                return;
            }

            //this.forceItemBattle.getForceItemBattleScoreboardManager().updateSidebar();

            int minutes = this.timeLeftInSeconds / 60;
            int seconds = this.timeLeftInSeconds % 60;
            Component timeComponent = Component.text(String.format("%02d:%02d", minutes, seconds), NamedTextColor.YELLOW);

            for (Player player : this.forceItemBattle.getServer().getOnlinePlayers()) {
                GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

                Component actionBarMessage = timeComponent;
                if (gamePlayer.isSpectator()) {
                    actionBarMessage = actionBarMessage.append(Component.text(" | Spectator", NamedTextColor.GRAY));
                }
                player.sendActionBar(actionBarMessage);
            }

            this.timeLeftInSeconds--;
        }, 0L, 20L);
    }

    @Override
    public void onLeave() {
        this.forceItemBattle.getServer().getScheduler().cancelTask(this.taskId);
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.INGAME;
    }
}