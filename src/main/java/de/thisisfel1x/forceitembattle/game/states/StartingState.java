package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;

import java.time.Duration;

public class StartingState extends GameState {

    private int taskId;
    private int counter = 3;
    private final int startCounter = this.counter;
    private final ForceItemBattle forceItemBattle;

    public StartingState(GameManager gameManager) {
        super(gameManager);

        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        this.forceItemBattle.getTeamManager().assignAllPlayersWithoutTeam();
        this.forceItemBattle.getTeamManager().getGamePlayers().values().stream()
                .filter(GamePlayer::isPlayer)
                .forEach(gamePlayer -> {
                    gamePlayer.cleanOnJoin();
                    gamePlayer.addEffectsForGame();
                });

        int jokerAmount = this.forceItemBattle.getJokerAmount();
        boolean jokersPerTeam = this.forceItemBattle.getConfig().getBoolean("jokers.per-team", true);
        this.forceItemBattle.getTeamManager().getTeams()
                .forEach(team -> team.addJokersToInventory(jokerAmount, jokersPerTeam));

        // World specific modifications
        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRules.ADVANCE_TIME, true);
            world.setGameRule(GameRules.ADVANCE_WEATHER, true);
        });

        this.taskId = this.forceItemBattle.getServer().getScheduler().scheduleSyncRepeatingTask(this.forceItemBattle, () -> {
            final var audience = this.forceItemBattle.getServer();

            if (counter <= 0) {
                this.forceItemBattle.getServer().getScheduler().cancelTask(taskId);

                Component finalTitle = Component.text("GO!", NamedTextColor.GOLD, TextDecoration.BOLD);
                audience.showTitle(Title.title(finalTitle, Component.empty(),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1200), Duration.ofMillis(500))));

                audience.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1.2f));

                IngameState ingameState = new IngameState(this.forceItemBattle.getGameManager());
                this.forceItemBattle.getGameManager().setCurrentGameState(ingameState);
                return;
            }

            TextColor titleColor = (this.counter == 3) ? NamedTextColor.GREEN :
                    (this.counter == 2) ? NamedTextColor.YELLOW : NamedTextColor.RED;

            String sekundenText = (this.counter == 1) ? " Sekunde" : " Sekunden";

            Component mainTitle = Component.text(this.counter, titleColor, TextDecoration.BOLD);
            Component subTitle = Component.text("Das Spiel beginnt...", NamedTextColor.GRAY);
            Component actionBar = Component.text("Das Spiel startet in " + this.counter + sekundenText, NamedTextColor.WHITE);

            Title countdownTitle = Title.title(mainTitle, subTitle,
                    Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(0)));

            float pitch = 0.7f + ((startCounter - this.counter) / (float) startCounter);
            Sound plingSound = Sound.sound(SoundEventKeys.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, pitch);

            audience.showTitle(countdownTitle);
            audience.sendActionBar(actionBar);
            audience.playSound(plingSound);

            this.counter--;
        }, 0L, 20L);
    }

    @Override
    public void onLeave() {
        this.forceItemBattle.getServer().getScheduler().cancelTask(this.taskId);
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.STARTING;
    }
}
