package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.utils.FoundItemData;
import de.thisisfel1x.forceitembattle.utils.LocationUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public class ResultsState extends GameState {

    private final ForceItemBattle forceItemBattle;
    private BukkitTask resultsTask;

    public ResultsState(GameManager gameManager) {
        super(gameManager);
        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        List<ForceItemBattleTeam> sortedTeams = this.forceItemBattle.getTeamManager().getTeams().stream()
                .sorted(Comparator.comparingInt(team -> ((ForceItemBattleTeam) team).getFoundItems().size()).reversed())
                .toList();

        this.resultsTask = new ResultsRunnable(sortedTeams).runTaskTimer(forceItemBattle, 0L, 4L);
    }

    @Override
    public void onLeave() {
        if (this.resultsTask != null && !this.resultsTask.isCancelled()) {
            this.resultsTask.cancel();
        }
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.RESULTS;
    }

    private class ResultsRunnable extends BukkitRunnable {

        private static final int SHOWCASE_ITEM_DURATION_TICKS = 100;
        private static final int REVEAL_DURATION_TICKS = 60;

        private final List<ForceItemBattleTeam> allSortedTeams;
        private final List<ForceItemBattleTeam> topTeams;
        private final Audience audience = Bukkit.getServer();

        private int currentTeamIndex = 0;
        private int currentItemIndex = 0;
        private int tickCounter = 0;
        private int shutdownCounter = 20;
        private boolean hasSentFinalRanking = false; // NEU

        public ResultsRunnable(List<ForceItemBattleTeam> sortedTeams) {
            this.allSortedTeams = sortedTeams;
            this.topTeams = sortedTeams.stream().filter(sortedTeam -> !sortedTeam.getFoundItems().isEmpty())
                    .limit(3).toList().reversed();
        }

        @Override
        public void run() {
            if (currentTeamIndex < topTeams.size()) {
                handleShowcase();
            } else {
                handleShutdown();
            }
            tickCounter++;
        }

        private void handleShowcase() {
            ForceItemBattleTeam team = topTeams.get(currentTeamIndex);

            if (tickCounter < SHOWCASE_ITEM_DURATION_TICKS) {
                // Item-Loop-Animation mit adaptiver Geschwindigkeit
                Component title = Component.text("Team ?", team.getTeamColor(), TextDecoration.BOLD);
                Component subtitle = Component.empty();

                List<FoundItemData> foundItems = team.getFoundItems();
                if (!foundItems.isEmpty()) {
                    // NEU: Geschwindigkeit anpassen
                    int itemCount = foundItems.size();
                    int updateInterval = Math.max(1, 5 - (itemCount / 10)); // Mehr Items = schnellerer Wechsel

                    if (tickCounter % updateInterval == 0) {
                        currentItemIndex = (currentItemIndex + 1) % foundItems.size();
                    }


                    FoundItemData itemData = foundItems.get(currentItemIndex);
                    subtitle = Component.translatable(itemData.item().translationKey())
                            .append(Component.text(itemData.usedJoker() ? " (Joker)" : "", NamedTextColor.YELLOW));
                }
                //audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_DISPENSER_FAIL, Sound.Source.MASTER, 1f, 1.2f));
                audience.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(100))));

            } else if (tickCounter < SHOWCASE_ITEM_DURATION_TICKS + REVEAL_DURATION_TICKS) {
                // Team-Enthüllung
                Component title = Component.text(team.getTeamName(), team.getTeamColor(), TextDecoration.BOLD);
                Component subtitle = Component.text(team.getFoundItems().size() + " Items gefunden!", NamedTextColor.WHITE);

                if (tickCounter == SHOWCASE_ITEM_DURATION_TICKS) {
                    audience.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.MASTER, 1f, 1.2f));
                }
                audience.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO, Duration.ofMillis(3000), Duration.ofMillis(200))));

            } else {
                // Übergang zum nächsten Team
                currentTeamIndex++;
                currentItemIndex = 0;
                tickCounter = -1;
            }
        }

        private void handleShutdown() {
            // NEU: Finale Rangliste einmalig im Chat ausgeben
            if (!hasSentFinalRanking) {
                sendFinalRankingToChat();
                hasSentFinalRanking = true;
                tickCounter = 0; // Setze den Zähler für den Shutdown-Countdown zurück
                return; // Warte einen Tick, bevor der Countdown startet
            }

            if (tickCounter % 25 == 0) {
                if (shutdownCounter > 0) {
                    Component msg = Component.text("Der Server stoppt in ", NamedTextColor.RED)
                            .append(Component.text(shutdownCounter, NamedTextColor.YELLOW))
                            .append(Component.text(" Sekunden...", NamedTextColor.RED));
                    audience.sendMessage(msg);
                    audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 1f, 1f));
                    shutdownCounter -= 5;
                } else {
                    Bukkit.shutdown();
                    this.cancel();
                }
            }
        }

        private void sendFinalRankingToChat() {
            audience.sendMessage(Component.text(""));
            audience.sendMessage(Component.text("▬▬▬▬▬ Endergebnis ▬▬▬▬▬", NamedTextColor.GOLD, TextDecoration.BOLD));
            audience.sendMessage(Component.text(""));

            int rank = 1;
            for (ForceItemBattleTeam team : allSortedTeams) {
                Component rankComponent = Component.text("#" + rank + " ", NamedTextColor.GRAY);
                Component teamComponent = Component.text(team.getTeamName(), team.getTeamColor());
                Component itemsComponent = Component.text(" - " + team.getFoundItems().size() + " Items", NamedTextColor.YELLOW);

                audience.sendMessage(rankComponent.append(teamComponent).append(itemsComponent));
                rank++;
            }

            audience.sendMessage(Component.text(""));
            audience.sendMessage(Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.GOLD, TextDecoration.BOLD));
        }
    }
}