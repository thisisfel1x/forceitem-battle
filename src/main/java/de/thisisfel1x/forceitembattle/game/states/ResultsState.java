package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.utils.FoundItemData;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        this.resultsTask = new ResultsRunnable(sortedTeams).runTaskTimer(forceItemBattle, 0L, 1L);
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

        private static final int ITEM_SHOWCASE_DURATION_TICKS = 200; // 10 seconds
        private static final int REVEAL_DURATION_TICKS = 60; // 3 seconds

        private final List<ForceItemBattleTeam> allSortedTeams;
        private final List<ForceItemBattleTeam> topTeams;
        private final Audience audience = Bukkit.getServer();

        private int currentTeamIndex = 0;
        private int currentItemIndex = 0;
        private int tickCounter = 0;
        private int shutdownTicksLeft = 20 * 20;
        private boolean hasSentFinalRanking = false;
        private final Set<Integer> announcementsMade = new HashSet<>();

        public ResultsRunnable(List<ForceItemBattleTeam> sortedTeams) {
            this.allSortedTeams = sortedTeams;
            this.topTeams = sortedTeams.stream()
                    .filter(team -> !team.getFoundItems().isEmpty())
                    .limit(3)
                    .toList().reversed();
        }

        @Override
        public void run() {
            if (currentTeamIndex < topTeams.size()) {
                handleShowcase();
                tickCounter++;
            } else {
                handleShutdown();
            }
        }

        private void handleShowcase() {
            ForceItemBattleTeam team = topTeams.get(currentTeamIndex);
            List<FoundItemData> foundItems = team.getFoundItems();

            if (tickCounter < ITEM_SHOWCASE_DURATION_TICKS) {
                if (tickCounter % 10 == 0) { // Switch item every half-second
                    currentItemIndex = (currentItemIndex + 1) % foundItems.size();
                    audience.playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK, Sound.Source.MASTER, 0.5f, 1.5f));
                }

                FoundItemData itemData = foundItems.get(currentItemIndex);
                Component title = Component.text(team.getTeamName(), team.getTeamColor(), TextDecoration.BOLD);
                Component subtitle = Component.translatable(itemData.item().translationKey())
                        .append(Component.text(itemData.usedJoker() ? " (Joker)" : "", NamedTextColor.YELLOW));
                audience.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO, Duration.ofMillis(200), Duration.ofMillis(50))));

            } else if (tickCounter < ITEM_SHOWCASE_DURATION_TICKS + REVEAL_DURATION_TICKS) {
                if (tickCounter == ITEM_SHOWCASE_DURATION_TICKS) {
                    Component title = Component.text(team.getTeamName(), team.getTeamColor(), TextDecoration.BOLD);
                    Component subtitle = Component.text(team.getFoundItems().size() + " Items gefunden!", NamedTextColor.WHITE);
                    audience.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO, Duration.ofMillis(3000), Duration.ofMillis(200))));
                    audience.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.MASTER, 1f, 1.2f));
                }
            } else {
                currentTeamIndex++;
                currentItemIndex = 0;
                tickCounter = -1;
            }
        }

        private void handleShutdown() {
            if (!hasSentFinalRanking) {
                sendFinalRankingToChat();
                hasSentFinalRanking = true;
                return;
            }

            if (shutdownTicksLeft <= 0) {
                Bukkit.shutdown();
                this.cancel();
                return;
            }

            int secondsLeft = (int) Math.ceil(shutdownTicksLeft / 20.0);
            boolean shouldAnnounce = (secondsLeft <= 5 && secondsLeft > 0) || secondsLeft == 10 || secondsLeft == 15 || secondsLeft == 20;

            if (shouldAnnounce && !announcementsMade.contains(secondsLeft)) {
                Component msg = forceItemBattle.getPrefix()
                        .append(Component.text("Der Server stoppt in ", NamedTextColor.RED))
                        .append(Component.text(secondsLeft, NamedTextColor.YELLOW))
                        .append(Component.text(secondsLeft == 1 ? " Sekunde..." : " Sekunden...", NamedTextColor.RED));
                audience.sendMessage(msg);
                audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 1f, 1f));
                announcementsMade.add(secondsLeft);
            }
            shutdownTicksLeft--;
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