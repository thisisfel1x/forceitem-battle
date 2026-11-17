package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.game.states.StartingState;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

public class StartCommand implements BasicCommand {

    private static final int MAX_MINUTES = 24 * 60;

    private final ForceItemBattle forceItemBattle;

    public StartCommand(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        // Works for players and the console
        CommandSender sender = source.getSender();

        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.IDLE) {
            this.sendError(sender, "Das Spiel läuft bereits oder wurde schon gestartet");
            return;
        }

        if (args.length != 2) {
            this.sendError(sender, "Verwendung: /start <Dauer in Minuten> <Anzahl Joker>");
            return;
        }

        OptionalInt minutes = parseInt(args[0]);
        if (minutes.isEmpty() || minutes.getAsInt() < 1 || minutes.getAsInt() > MAX_MINUTES) {
            this.sendError(sender, "Die Dauer muss eine Zahl zwischen 1 und " + MAX_MINUTES + " Minuten sein");
            return;
        }

        OptionalInt jokerAmount = parseInt(args[1]);
        if (jokerAmount.isEmpty() || jokerAmount.getAsInt() < 0 || jokerAmount.getAsInt() > ForceItemBattleTeam.MAX_JOKER_STACK) {
            this.sendError(sender, "Die Joker-Anzahl muss eine Zahl zwischen 0 und " + ForceItemBattleTeam.MAX_JOKER_STACK + " sein");
            return;
        }

        boolean anyPlayer = this.forceItemBattle.getTeamManager().getGamePlayers().values().stream()
                .anyMatch(gamePlayer -> !gamePlayer.isSpectator());
        if (!anyPlayer) {
            this.sendError(sender, "Es sind keine Spieler online");
            return;
        }

        this.forceItemBattle.setGameTime(minutes.getAsInt());
        this.forceItemBattle.setJokerAmount(jokerAmount.getAsInt());

        sender.sendMessage(this.forceItemBattle.getPrefix().append(Component.text("Das Spiel wird gestartet", NamedTextColor.GREEN)));

        StartingState startingState = new StartingState(this.forceItemBattle.getGameManager());
        this.forceItemBattle.getGameManager().setCurrentGameState(startingState);
    }

    private void sendError(CommandSender sender, String message) {
        sender.sendMessage(this.forceItemBattle.getPrefix().append(Component.text(message, NamedTextColor.RED)));
    }

    private static OptionalInt parseInt(String input) {
        try {
            return OptionalInt.of(Integer.parseInt(input));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    @Override
    public @Nullable String permission() {
        return "forceitembattle.start";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        // args is empty while nothing has been typed after the command yet
        if (args.length <= 1) {
            return List.of("30", "60", "90", "120");
        }
        if (args.length == 2) {
            return List.of("0", "3", "5");
        }
        return List.of();
    }

}
