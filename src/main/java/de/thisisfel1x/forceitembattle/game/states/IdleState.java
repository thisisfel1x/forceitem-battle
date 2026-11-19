package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.Message;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

public class IdleState extends GameState {

    private int taskId;
    private final ForceItemBattle forceItemBattle;

    private int animationTick = 0;

    public IdleState(GameManager gameManager) {
        super(gameManager);

        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            String dots = ".".repeat(this.animationTick % 4);
            for (Player player : this.forceItemBattle.getServer().getOnlinePlayers()) {
                player.sendActionBar(this.forceItemBattle.getLanguageManager().render(player,
                        Message.LOBBY_WAITING_FOR_PLAYERS, Placeholder.unparsed("dots", dots)));
            }
            this.animationTick++;
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
