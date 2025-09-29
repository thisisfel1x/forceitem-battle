package de.thisisfel1x.forceitembattle.game;

public abstract class GameState {

    protected final GameManager gameManager;

    public GameState(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public abstract void onEnter();

    public abstract void onLeave();

    public abstract GameStateEnum getGameStateEnum();

    public String getDisplayName() {
        String name = this.getGameStateEnum().name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
