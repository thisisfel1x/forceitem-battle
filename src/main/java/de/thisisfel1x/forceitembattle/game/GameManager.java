package de.thisisfel1x.forceitembattle.game;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.states.IdleState;
import net.kyori.adventure.text.Component;

public class GameManager {

    private final ForceItemBattle forceItemBattle;

    private GameState currentGameState;

    public GameManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;

        // Set default IDLE State
        setCurrentGameState(new IdleState(this));
    }

    public void setCurrentGameState(GameState newState) {
        if (this.currentGameState != null) {
            this.currentGameState.onLeave();
        }

        this.currentGameState = newState;

        this.currentGameState.onEnter();

        this.forceItemBattle.getComponentLogger().debug(Component.text("Switched to " + currentGameState.getDisplayName()));
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }
}
