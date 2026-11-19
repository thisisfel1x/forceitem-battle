package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.game.states.StartingState;
import de.thisisfel1x.forceitembattle.i18n.LanguageManager;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
        LanguageManager languageManager = this.forceItemBattle.getLanguageManager();

        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.IDLE) {
            languageManager.send(sender, Message.COMMAND_START_ALREADY_RUNNING);
            return;
        }

        if (args.length != 2) {
            languageManager.send(sender, Message.COMMAND_START_USAGE);
            return;
        }

        OptionalInt minutes = parseInt(args[0]);
        if (minutes.isEmpty() || minutes.getAsInt() < 1 || minutes.getAsInt() > MAX_MINUTES) {
            languageManager.send(sender, Message.COMMAND_START_INVALID_DURATION,
                    Placeholder.unparsed("max", Integer.toString(MAX_MINUTES)));
            return;
        }

        OptionalInt jokerAmount = parseInt(args[1]);
        if (jokerAmount.isEmpty() || jokerAmount.getAsInt() < 0 || jokerAmount.getAsInt() > ForceItemBattleTeam.MAX_JOKER_STACK) {
            languageManager.send(sender, Message.COMMAND_START_INVALID_JOKERS,
                    Placeholder.unparsed("max", Integer.toString(ForceItemBattleTeam.MAX_JOKER_STACK)));
            return;
        }

        boolean anyPlayer = this.forceItemBattle.getTeamManager().getGamePlayers().values().stream()
                .anyMatch(gamePlayer -> !gamePlayer.isSpectator());
        if (!anyPlayer) {
            languageManager.send(sender, Message.COMMAND_START_NO_PLAYERS);
            return;
        }

        this.forceItemBattle.setGameTime(minutes.getAsInt());
        this.forceItemBattle.setJokerAmount(jokerAmount.getAsInt());

        languageManager.send(sender, Message.COMMAND_START_STARTING);

        StartingState startingState = new StartingState(this.forceItemBattle.getGameManager());
        this.forceItemBattle.getGameManager().setCurrentGameState(startingState);
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
