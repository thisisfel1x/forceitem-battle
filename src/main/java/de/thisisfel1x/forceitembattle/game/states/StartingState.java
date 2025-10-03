package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.TitlePart;

public class StartingState extends GameState {

    private int taskId;
    private int counter = 3;
    private final ForceItemBattle forceItemBattle;

    public StartingState(GameManager gameManager) {
        super(gameManager);

        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        this.forceItemBattle.getTeamManager().assignAllPlayersWithoutTeam();
        this.forceItemBattle.getTeamManager().getGamePlayers().values().forEach(GamePlayer::cleanOnJoin);
        this.forceItemBattle.getTeamManager().getTeams().forEach(ForceItemBattleTeam::addJokersToInventory);

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {

            if (counter == 0) {
                this.forceItemBattle.getServer().getScheduler().cancelTask(taskId);

                IngameState ingameState = new IngameState(this.forceItemBattle.getGameManager());
                this.forceItemBattle.getGameManager().setCurrentGameState(ingameState);
            } else {
                this.forceItemBattle.getServer().sendTitlePart(TitlePart.TITLE, Component.text(this.counter));
                this.forceItemBattle.getServer().playSound(Sound.sound()
                        .type(SoundEventKeys.BLOCK_NOTE_BLOCK_PLING)
                        .pitch((this.counter / 9f))
                        .build());
            }

            this.counter--;
        }, 0L, 20L);
    }

    @Override
    public void onLeave() {

    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.STARTING;
    }
}
