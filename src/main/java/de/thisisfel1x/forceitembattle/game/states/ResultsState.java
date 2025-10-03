package de.thisisfel1x.forceitembattle.game.states;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.GameState;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.util.Comparator;
import java.util.List;

public class ResultsState extends GameState {

    private final ForceItemBattle forceItemBattle;

    public ResultsState(GameManager gameManager) {
        super(gameManager);
        this.forceItemBattle = ForceItemBattle.getInstance();
    }

    @Override
    public void onEnter() {
        sendRankingToChat();
    }

    private void sendRankingToChat() {
        List<ForceItemBattleTeam> sortedTeams = this.forceItemBattle.getTeamManager().getTeams().stream()
                .sorted(Comparator.comparingInt( team -> ((ForceItemBattleTeam) team).getFoundItems().size()).reversed())
                .toList();

        this.forceItemBattle.getServer().broadcast(Component.text(""));
        this.forceItemBattle.getServer().broadcast(
                Component.text("▬▬▬▬▬ Endergebnis ▬▬▬▬▬", NamedTextColor.GOLD, TextDecoration.BOLD));
        this.forceItemBattle.getServer().broadcast(Component.text(""));

        int rank = 1;
        for (ForceItemBattleTeam team : sortedTeams) {
            Component rankComponent = Component.text("#" + rank + " ", NamedTextColor.GRAY);
            Component teamComponent = Component.text(team.getTeamName(), team.getTeamColor());
            Component itemsComponent = Component.text(" - " + team.getFoundItems().size() + " Items", NamedTextColor.YELLOW);

            this.forceItemBattle.getServer().broadcast(rankComponent.append(teamComponent).append(itemsComponent));
            rank++;
        }

        this.forceItemBattle.getServer().broadcast(Component.text(""));
        this.forceItemBattle.getServer().broadcast(
                Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    @Override
    public void onLeave() {
    }

    @Override
    public GameStateEnum getGameStateEnum() {
        return GameStateEnum.RESULTS;
    }
}