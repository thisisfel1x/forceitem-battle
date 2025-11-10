package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResultsState extends GameState {

    private final ForceItemBattle forceItemBattle;
    private BukkitTask currentResultsTask;

    private List<Location> lobbySpawns;
    private int nextSpawnIndex = 0;

    private List<ForceItemBattleTeam> allSortedTeams;
    private List<ForceItemBattleTeam> topTeams;
    private final Audience audience = Bukkit.getServer();

    public ResultsState(GameManager gameManager) {
        super(gameManager);
        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        this.forceItemBattle.getSettingsManager().removeAllGameSettings();

        Bukkit.getOnlinePlayers().forEach(player -> player.getPassengers().forEach(passenger -> {
            player.removePassenger(passenger);
            passenger.remove();
        }));

        setupAndTeleportPlayers();

        this.forceItemBattle.getTeamManager().getGamePlayers().values().forEach(GamePlayer::freezePlayer);
        this.forceItemBattle.getTeamManager().getTeams().forEach(ForceItemBattleTeam::clearBossBar);

        this.allSortedTeams = this.forceItemBattle.getTeamManager().getTeams().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .sorted(Comparator.comparingInt(team -> ((ForceItemBattleTeam) team).getFoundItems().size()).reversed())
                .toList().reversed();

        this.topTeams = allSortedTeams.stream()
                .filter(team -> !team.getFoundItems().isEmpty())
                //.limit(3)
                .toList();

        startIntroTask();
    }

    private void setupAndTeleportPlayers() {
        Location worldSpawnPoint = Bukkit.getWorld("world").getSpawnLocation();
        worldSpawnPoint.getChunk().load();
        int maxPlayers = this.forceItemBattle.getTeamManager().getTeams().size() * 2;
        double radius = 10;
        this.lobbySpawns = LocationUtil.calculateCircleSpawns(worldSpawnPoint, maxPlayers, radius);
        this.nextSpawnIndex = 0;

        this.forceItemBattle.getTeamManager().getGamePlayers().values()
                .forEach(gamePlayer -> this.teleportPlayerToNextSpawn(gamePlayer.getPlayer()));
    }

    public void teleportPlayerToNextSpawn(Player player) {
        if (lobbySpawns.isEmpty()) return;
        Location spawnLocation = lobbySpawns.get(nextSpawnIndex);
        player.teleport(spawnLocation);
        nextSpawnIndex = (nextSpawnIndex + 1) % lobbySpawns.size();
    }

    private void startIntroTask() {
        currentResultsTask = new ShowIntroTask().runTaskLater(forceItemBattle, 20L);
    }

    private void startTeamShowcaseTask(int teamIndex) {
        currentResultsTask = new TeamShowcaseTask(teamIndex).runTaskTimer(forceItemBattle, 0L, 10L);
    }

    private void startTeamRevealTask(ForceItemBattleTeam team, int teamRank) {
        currentResultsTask = new TeamRevealTask(team, teamRank).runTaskLater(forceItemBattle, 0L);
    }

    private void startFinalRankingTask() {
        sendFinalRankingToChat();
        currentResultsTask = new ShutdownCountdownTask().runTaskTimer(forceItemBattle, 0L, 20L);
    }

    private class ShowIntroTask extends BukkitRunnable {
        @Override
        public void run() {
            Component title = Component.text("Ergebnisse", NamedTextColor.GOLD, TextDecoration.BOLD);
            Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1));
            audience.showTitle(Title.title(title, Component.empty(), times));
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 0.8f, 1f));

            currentResultsTask = new BukkitRunnable() {
                @Override
                public void run() {
                    startTeamShowcaseTask(0);
                }
            }.runTaskLater(forceItemBattle, 100L);
        }
    }

    private class TeamShowcaseTask extends BukkitRunnable {
        private final int teamIndex;
        private final ForceItemBattleTeam team;
        private final List<FoundItemData> foundItems;
        private int currentItemIndex = 0;

        public TeamShowcaseTask(int teamIndex) {
            this.teamIndex = teamIndex;
            this.team = topTeams.get(teamIndex);
            this.foundItems = team.getFoundItems();
        }

        @Override
        public void run() {
            if (currentItemIndex >= foundItems.size()) {
                this.cancel();
                startTeamRevealTask(team, teamIndex);
                return;
            }

            FoundItemData itemData = foundItems.get(currentItemIndex);
            Component title = Component.text("Team ?", team.getTeamColor(), TextDecoration.BOLD);
            Component subtitle = Component.translatable(itemData.item().translationKey())
                    .append(Component.text(itemData.usedJoker() ? " (Joker)" : "", NamedTextColor.YELLOW));

            audience.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(100))));
            audience.playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK, Sound.Source.MASTER, 0.5f, 1.5f));

            currentItemIndex++;
        }
    }

    private class TeamRevealTask extends BukkitRunnable {
        private final ForceItemBattleTeam team;
        private final int teamRank;

        public TeamRevealTask(ForceItemBattleTeam team, int teamRank) {
            this.team = team;
            this.teamRank = teamRank;
        }

        @Override
        public void run() {
            Component title = Component.text("#" + (topTeams.size() - teamRank) + " " + team.getTeamName(), team.getTeamColor(), TextDecoration.BOLD);
            Component subtitle = Component.text(team.getFoundItems().size() + " Items gefunden!", NamedTextColor.WHITE);
            Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1));

            audience.showTitle(Title.title(title, subtitle, times));
            audience.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.MASTER, 1f, 1.2f));

            currentResultsTask = new BukkitRunnable() {
                @Override
                public void run() {
                    int nextTeamIndex = teamRank + 1;
                    if (nextTeamIndex < topTeams.size()) {
                        startTeamShowcaseTask(nextTeamIndex);
                    } else {
                        startFinalRankingTask();
                    }
                }
            }.runTaskLater(forceItemBattle, 100L);
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

    private class ShutdownCountdownTask extends BukkitRunnable {
        private int secondsLeft = 20;
        private final Set<Integer> announcements = Set.of(20, 15, 10, 5, 4, 3, 2, 1);

        @Override
        public void run() {
            if (announcements.contains(secondsLeft)) {
                Component msg = forceItemBattle.getPrefix()
                        .append(Component.text("Der Server stoppt in ", NamedTextColor.RED))
                        .append(Component.text(secondsLeft, NamedTextColor.YELLOW))
                        .append(Component.text(secondsLeft == 1 ? " Sekunde..." : " Sekunden...", NamedTextColor.RED));
                audience.sendMessage(msg);
                audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 1f, 1f));
            }

            if (secondsLeft <= 0) {
                Bukkit.shutdown();
                this.cancel();
                return;
            }
            secondsLeft--;
        }
    }

    @Override
    public void onLeave() {
        if (this.currentResultsTask != null && !this.currentResultsTask.isCancelled()) {
            this.currentResultsTask.cancel();
        }
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.RESULTS;
    }
}