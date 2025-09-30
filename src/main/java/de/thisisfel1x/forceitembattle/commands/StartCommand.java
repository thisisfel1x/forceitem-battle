package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.game.states.StartingState;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.Nullable;

public class StartCommand implements BasicCommand {

    private final ForceItemBattle forceItemBattle;

    public StartCommand(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {

        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.IDLE) {
            source.getSender().sendMessage(Component.text("Nicht im IDLE State",  NamedTextColor.RED));
            return;
        }

        // TODO arg für Zeit
        StartingState startingState = new StartingState(this.forceItemBattle.getGameManager());
        this.forceItemBattle.getGameManager().setCurrentGameState(startingState);


    }

    @Override
    public @Nullable String permission() {
        return "forceitembattle.start";
    }

    @Override
    public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("1", "2", "3", "4", "5");
        }
        return java.util.List.of();
    }
}
