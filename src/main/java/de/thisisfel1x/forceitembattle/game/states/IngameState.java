package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
        this.forceItemBattle.getSettingsManager().applyAllGameSettings();

        this.forceItemBattle.getTeamManager().getTeams().forEach(ForceItemBattleTeam::updateBossBar);
        this.forceItemBattle.getTeamManager().getGamePlayers().values().forEach(GamePlayer::updateArmorstandItem);
        this.forceItemBattle.getTeamManager().getGamePlayers().values().forEach(GamePlayer::unfreezePlayer);

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            if (this.timeLeftInSeconds <= 0) {
                this.forceItemBattle.getServer().getScheduler().cancelTask(this.taskId);

                ResultsState resultsState = new ResultsState(this.forceItemBattle.getGameManager());
                this.forceItemBattle.getGameManager().setCurrentGameState(resultsState);
                return;
            }

            // Safety net for every way of obtaining an item that is not covered by an event
            this.forceItemBattle.getItemTracker().scanInventories();

            int minutes = this.timeLeftInSeconds / 60;
            int seconds = this.timeLeftInSeconds % 60;
            TagResolver time = Placeholder.unparsed("time", String.format("%02d:%02d", minutes, seconds));

            for (Player player : this.forceItemBattle.getServer().getOnlinePlayers()) {
                Message message = this.forceItemBattle.getTeamManager().isSpectator(player)
                        ? Message.GAME_TIMER_SPECTATOR
                        : Message.GAME_TIMER;
                player.sendActionBar(this.forceItemBattle.getLanguageManager().render(player, message, time));
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
