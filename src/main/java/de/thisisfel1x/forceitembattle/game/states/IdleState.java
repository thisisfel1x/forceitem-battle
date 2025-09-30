package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import net.kyori.adventure.text.Component;

public class IdleState extends GameState {

    private int taskId;
    private final ForceItemBattle forceItemBattle;

    public IdleState(GameManager gameManager) {
        super(gameManager);

        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            this.forceItemBattle.getServer().sendActionBar(Component.text("IDLE"));
        }, 0L, 20L);

    }

    @Override
    public void onLeave() {
        this.forceItemBattle.getServer().getScheduler().cancelTask(this.taskId);
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.IDLE;
    }
}
