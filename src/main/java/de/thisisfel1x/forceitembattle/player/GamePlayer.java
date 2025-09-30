package de.thisisfel1x.forceitembattle.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.game.GameStateEnum;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GamePlayer {

    private final Player player;
    private final String name;
    private final UUID uniqueId;

    private ForceItemBattleTeam forceItemBattleTeam;

    private boolean spectator = false;

    public GamePlayer(Player player) {

        this.player = player;
        this.name = player.getName();
        this.uniqueId = player.getUniqueId();

        this.forceItemBattleTeam = null;
    }

    public void sendMessage(Component message) {
        this.player.sendMessage(message);
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Player getPlayer() {
        return player;
    }

    public ForceItemBattleTeam getTeam() {
        return forceItemBattleTeam;
    }

    public void setTeam(ForceItemBattleTeam forceItemBattleTeam) {
        this.forceItemBattleTeam = forceItemBattleTeam;
    }

    public boolean isInTeam() {
        return forceItemBattleTeam != null;
    }

    public boolean isPlayer() {
        return !this.isSpectator();
    }

    public boolean isActive() {
        return this.player.isOnline();
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public void cleanOnJoin() {
        GameStateEnum currentGameState = ForceItemBattle.getInstance().getGameManager().getCurrentGameState().getGameStateEnum();

        if (currentGameState != GameStateEnum.IDLE) return;

        this.player.getInventory().clear();
        this.player.getInventory().setHelmet(null);
        this.player.getInventory().setChestplate(null);
        this.player.getInventory().setLeggings(null);
        this.player.getInventory().setBoots(null);

        this.player.setHealth(20);
        this.player.setFoodLevel(20);
    }

    public void setLobbyInventory() {
        this.player.getInventory().clear();

        ItemStack teamSelector = ItemBuilder.from(Material.AMETHYST_SHARD).name(Component.text("Wähle dein Team", NamedTextColor.WHITE)).glow().build();
        this.player.getInventory().setItem(0, teamSelector);

    }
}
