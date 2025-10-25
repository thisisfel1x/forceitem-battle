package de.thisisfel1x.forceitembattle.commands;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.game.states.StartingState;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public class StartCommand implements BasicCommand {

    private final ForceItemBattle forceItemBattle;

    public StartCommand(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) return;

        if (this.forceItemBattle.getGameManager().getCurrentGameState().getGameStateEnum() != GameStateEnum.IDLE) {
            source.getSender().sendMessage(Component.text("Nicht im IDLE State",  NamedTextColor.RED));
            return;
        }

        if (args.length != 2) {
            return;
        }

        int minutes = Integer.parseInt(args[0]);
        int jokerAmount = Integer.parseInt(args[1]);
        this.forceItemBattle.setGameTime(minutes);
        this.forceItemBattle.setJokerAmount(jokerAmount);

        // TODO arg für Zeit
        StartingState startingState = new StartingState(this.forceItemBattle.getGameManager());
        this.forceItemBattle.getGameManager().setCurrentGameState(startingState);

        player.sendMessage(this.forceItemBattle.getPrefix().append(Component.text("Das Spiel wird gestartet", NamedTextColor.GREEN)));

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
