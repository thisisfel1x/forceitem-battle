package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import org.bukkit.entity.Player;

public class IngameState extends GameState {

    private int taskId;
    private int counter = 3;
    private final ForceItemBattle forceItemBattle;

    public IngameState(GameManager gameManager) {
        super(gameManager);

        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            for (Player player : this.forceItemBattle.getServer().getOnlinePlayers()) {
                GamePlayer gamePlayer = this.forceItemBattle.getTeamManager().getGamePlayer(player.getUniqueId());

                if (gamePlayer.isInTeam()) {
                    player.sendActionBar(String.format("%s | Spectator=%s | State=INGAME", gamePlayer.getTeam().getTeamName(), gamePlayer.isSpectator()));
                } else {
                    player.sendActionBar("SPECTATOR");
                }
            }
        }, 0L, 20L);
    }

    @Override
    public void onLeave() {

    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.INGAME;
    }
}
