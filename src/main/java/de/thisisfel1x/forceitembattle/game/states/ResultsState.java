package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.i18n.LanguageManager;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.utils.FoundItemData;
import de.thisisfel1x.forceitembattle.utils.LocationUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ResultsState extends GameState {

    private final ForceItemBattle forceItemBattle;
    private BukkitTask currentResultsTask;

    private List<Location> lobbySpawns = List.of();
    private int nextSpawnIndex = 0;

    /**
     * All teams with members, best team first
     */
    private List<ForceItemBattleTeam> ranking = List.of();
    /**
     * Teams that found at least one item in the order they are revealed: worst first, winner last
     */
    private List<ForceItemBattleTeam> showcaseTeams = List.of();
    private final Audience audience = Bukkit.getServer();

    public ResultsState(GameManager gameManager) {
        super(gameManager);
        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        this.forceItemBattle.getSettingsManager().removeAllGameSettings();

        this.forceItemBattle.getTeamManager().getGamePlayers().values().forEach(GamePlayer::removeArmorstand);
        Bukkit.getOnlinePlayers().forEach(Player::closeInventory);

        setupAndTeleportPlayers();

        this.forceItemBattle.getTeamManager().getGamePlayers().values().forEach(GamePlayer::freezePlayer);
        this.forceItemBattle.getTeamManager().getTeams().forEach(ForceItemBattleTeam::clearBossBar);

        // Most items first; on a tie the team that reached its score earlier is ranked higher
        this.ranking = this.forceItemBattle.getTeamManager().getTeams().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .sorted(Comparator.comparingInt((ForceItemBattleTeam team) -> team.getFoundItems().size()).reversed()
                        .thenComparingLong(ForceItemBattleTeam::getLastFoundTimestamp))
                .toList();

        this.showcaseTeams = this.ranking.stream()
                .filter(team -> !team.getFoundItems().isEmpty())
                .toList()
                .reversed();

        startIntroTask();
    }

    private void setupAndTeleportPlayers() {
        Location worldSpawnPoint = this.forceItemBattle.getMainWorld().getSpawnLocation();
        worldSpawnPoint.getChunk().load();
        int maxPlayers = this.forceItemBattle.getTeamManager().getMaxPlayers();
        double radius = 10;
        this.lobbySpawns = LocationUtil.calculateCircleSpawns(worldSpawnPoint, maxPlayers, radius);
        this.nextSpawnIndex = 0;

        this.forceItemBattle.getTeamManager().getGamePlayers().values()
                .forEach(gamePlayer -> gamePlayer.ifOnline(this::teleportPlayerToNextSpawn));
    }

    public void teleportPlayerToNextSpawn(Player player) {
        if (lobbySpawns.isEmpty()) return;
        Location spawnLocation = lobbySpawns.get(nextSpawnIndex);
        player.teleport(spawnLocation);
        nextSpawnIndex = (nextSpawnIndex + 1) % lobbySpawns.size();
    }

    private int getRank(ForceItemBattleTeam team) {
        return this.ranking.indexOf(team) + 1;
    }

    private void startIntroTask() {
        currentResultsTask = new ShowIntroTask().runTaskLater(forceItemBattle, 20L);
    }

    /**
     * Shows the team at the given showcase position or finishes with the final ranking
     * if every team has been shown
     */
    private void startTeamShowcaseOrFinish(int showcaseIndex) {
        if (showcaseIndex < showcaseTeams.size()) {
            currentResultsTask = new TeamShowcaseTask(showcaseIndex).runTaskTimer(forceItemBattle, 0L, 10L);
        } else {
            startFinalRankingTask();
        }
    }

    private void startTeamRevealTask(ForceItemBattleTeam team, int showcaseIndex) {
        currentResultsTask = new TeamRevealTask(team, showcaseIndex).runTaskLater(forceItemBattle, 0L);
    }

    private void startFinalRankingTask() {
        sendFinalRankingToChat();

        if (this.forceItemBattle.getConfig().getBoolean("results.shutdown-server", true)) {
            int delay = Math.max(1, this.forceItemBattle.getConfig().getInt("results.shutdown-delay-seconds", 20));
            currentResultsTask = new ShutdownCountdownTask(delay).runTaskTimer(forceItemBattle, 0L, 20L);
        }
    }

    private class ShowIntroTask extends BukkitRunnable {
        @Override
        public void run() {
            Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1));
            showTitle(player -> Title.title(render(player, Message.RESULTS_TITLE), Component.empty(), times));
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 0.8f, 1f));

            currentResultsTask = new BukkitRunnable() {
                @Override
                public void run() {
                    startTeamShowcaseOrFinish(0);
                }
            }.runTaskLater(forceItemBattle, 100L);
        }
    }

    private class TeamShowcaseTask extends BukkitRunnable {
        private final int showcaseIndex;
        private final ForceItemBattleTeam team;
        private final List<FoundItemData> foundItems;
        private int currentItemIndex = 0;

        public TeamShowcaseTask(int showcaseIndex) {
            this.showcaseIndex = showcaseIndex;
            this.team = showcaseTeams.get(showcaseIndex);
            this.foundItems = List.copyOf(team.getFoundItems());
        }

        @Override
        public void run() {
            if (currentItemIndex >= foundItems.size()) {
                this.cancel();
                startTeamRevealTask(team, showcaseIndex);
                return;
            }

            FoundItemData itemData = foundItems.get(currentItemIndex);
            Component item = Component.translatable(itemData.item().translationKey());
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(100));

            showTitle(player -> {
                Component title = render(player, Message.RESULTS_HIDDEN_TEAM)
                        .colorIfAbsent(team.getTeamColor()).decorate(TextDecoration.BOLD);
                Component subtitle = itemData.usedJoker() ? item.append(render(player, Message.RESULTS_JOKER_SUFFIX)) : item;
                return Title.title(title, subtitle, times);
            });
            audience.playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK, Sound.Source.MASTER, 0.5f, 1.5f));

            currentItemIndex++;
        }
    }

    private class TeamRevealTask extends BukkitRunnable {
        private final ForceItemBattleTeam team;
        private final int showcaseIndex;

        public TeamRevealTask(ForceItemBattleTeam team, int showcaseIndex) {
            this.team = team;
            this.showcaseIndex = showcaseIndex;
        }

        @Override
        public void run() {
            Component title = Component.text("#" + getRank(team) + " " + team.getTeamName(), team.getTeamColor(), TextDecoration.BOLD);
            TagResolver items = LanguageManager.count("items", team.getFoundItems().size());
            Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1));

            showTitle(player -> Title.title(title, render(player, Message.RESULTS_TEAM_ITEMS, items), times));
            audience.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.MASTER, 1f, 1.2f));

            currentResultsTask = new BukkitRunnable() {
                @Override
                public void run() {
                    startTeamShowcaseOrFinish(showcaseIndex + 1);
                }
            }.runTaskLater(forceItemBattle, 100L);
        }
    }

    private void sendFinalRankingToChat() {
        List<CommandSender> receivers = new ArrayList<>(Bukkit.getOnlinePlayers());
        receivers.add(Bukkit.getConsoleSender());

        LanguageManager languageManager = this.forceItemBattle.getLanguageManager();
        for (CommandSender receiver : receivers) {
            receiver.sendMessage(Component.empty());
            languageManager.send(receiver, Message.RESULTS_FINAL_HEADER);
            receiver.sendMessage(Component.empty());

            for (ForceItemBattleTeam team : ranking) {
                languageManager.send(receiver, Message.RESULTS_FINAL_LINE,
                        Placeholder.unparsed("rank", Integer.toString(getRank(team))),
                        Placeholder.component("team", Component.text(team.getTeamName(), team.getTeamColor())),
                        LanguageManager.count("items", team.getFoundItems().size()));
            }

            receiver.sendMessage(Component.empty());
            languageManager.send(receiver, Message.RESULTS_FINAL_FOOTER);
        }
    }

    private Component render(Player player, Message message, TagResolver... placeholders) {
        return this.forceItemBattle.getLanguageManager().render(player, message, placeholders);
    }

    /**
     * Shows every online player a title in its language
     */
    private void showTitle(Function<Player, Title> title) {
        Bukkit.getOnlinePlayers().forEach(player -> player.showTitle(title.apply(player)));
    }

    private class ShutdownCountdownTask extends BukkitRunnable {
        private int secondsLeft;
        private final Set<Integer> announcements = Set.of(60, 30, 20, 15, 10, 5, 4, 3, 2, 1);

        public ShutdownCountdownTask(int secondsLeft) {
            this.secondsLeft = secondsLeft;
        }

        @Override
        public void run() {
            if (announcements.contains(secondsLeft)) {
                forceItemBattle.getLanguageManager().broadcast(Message.RESULTS_SHUTDOWN,
                        LanguageManager.count("seconds", secondsLeft));
                audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 1f, 1f));
            }

            if (secondsLeft <= 0) {
                this.cancel();
                Bukkit.shutdown();
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
