package de.thisisfel1x.forceitembattle.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GamePlayer {

    private final String name;
    private final UUID uniqueId;

    private ForceItemBattleTeam forceItemBattleTeam;

    private boolean spectator = false;

    public GamePlayer(Player player) {
        this.name = player.getName();
        this.uniqueId = player.getUniqueId();

        this.forceItemBattleTeam = null;
    }

    public void sendMessage(Component message) {
        this.getPlayer().sendMessage(message);
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.uniqueId);
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
        return this.getPlayer().isOnline();
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public void cleanOnJoin() {
        this.getPlayer().getInventory().clear();
        this.getPlayer().getInventory().setHelmet(null);
        this.getPlayer().getInventory().setChestplate(null);
        this.getPlayer().getInventory().setLeggings(null);
        this.getPlayer().getInventory().setBoots(null);

        this.getPlayer().setHealth(20);
        this.getPlayer().setFoodLevel(20);
    }

    public void setLobbyInventory() {
        this.getPlayer().getInventory().clear();

        ItemStack teamSelector = ItemBuilder.from(Material.AMETHYST_SHARD).name(Component.text("Wähle dein Team", NamedTextColor.WHITE)).glow().build();
        this.getPlayer().getInventory().setItem(0, teamSelector);

    }

    public void setSpectatorInventory() {
        this.getPlayer().getInventory().clear();

        this.getPlayer().setGameMode(GameMode.ADVENTURE);
        this.getPlayer().setAllowFlight(true);
        this.getPlayer().setFlying(true);

        for (GamePlayer gamePlayer : ForceItemBattle.getInstance().getTeamManager().getGamePlayers().values()) {
            if (!gamePlayer.isSpectator()) {
                gamePlayer.getPlayer().hidePlayer(ForceItemBattle.getInstance(), this.getPlayer());
            }
        }

        ItemStack teamSelector = ItemBuilder.from(Material.COMPASS).name(Component.text("Spieler beobachten", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)).build();
        this.getPlayer().getInventory().setItem(0, teamSelector);

    }
}
