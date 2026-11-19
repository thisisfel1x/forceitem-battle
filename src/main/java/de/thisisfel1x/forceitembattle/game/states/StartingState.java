package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.LanguageManager;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;

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
            final LanguageManager languageManager = this.forceItemBattle.getLanguageManager();

            if (counter <= 0) {
                this.forceItemBattle.getServer().getScheduler().cancelTask(taskId);

                Title.Times goTimes = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1200), Duration.ofMillis(500));
                for (Player player : this.forceItemBattle.getServer().getOnlinePlayers()) {
                    player.showTitle(Title.title(languageManager.render(player, Message.STARTING_GO), Component.empty(), goTimes));
                }

                audience.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1.2f));

                IngameState ingameState = new IngameState(this.forceItemBattle.getGameManager());
                this.forceItemBattle.getGameManager().setCurrentGameState(ingameState);
                return;
            }

            TextColor titleColor = (this.counter == 3) ? NamedTextColor.GREEN :
                    (this.counter == 2) ? NamedTextColor.YELLOW : NamedTextColor.RED;

            Component mainTitle = Component.text(this.counter, titleColor, TextDecoration.BOLD);
            Title.Times countdownTimes = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(0));
            TagResolver seconds = LanguageManager.count("seconds", this.counter);

            for (Player player : this.forceItemBattle.getServer().getOnlinePlayers()) {
                player.showTitle(Title.title(mainTitle, languageManager.render(player, Message.STARTING_SUBTITLE), countdownTimes));
                player.sendActionBar(languageManager.render(player, Message.STARTING_ACTIONBAR, seconds));
            }

            float pitch = 0.7f + ((startCounter - this.counter) / (float) startCounter);
            audience.playSound(Sound.sound(SoundEventKeys.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, pitch));

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
