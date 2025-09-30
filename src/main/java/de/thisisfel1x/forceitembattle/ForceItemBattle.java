package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.gui.TeamSelectorInventory;
import de.thisisfel1x.forceitembattle.listeners.player.InteractListener;
import de.thisisfel1x.forceitembattle.listeners.player.InventoryClickListener;
import de.thisisfel1x.forceitembattle.listeners.player.JoinListener;
import de.thisisfel1x.forceitembattle.listeners.player.QuitListener;
import de.thisisfel1x.forceitembattle.teams.TeamManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForceItemBattle extends JavaPlugin {

    private static ForceItemBattle instance;

    // Managers
    private GameManager gameManager;
    private TeamManager teamManager;

    // Inventories
    private TeamSelectorInventory teamSelectorInventory;

    @Override
    public void onEnable() {
        instance = this;

        this.gameManager = new GameManager(this);
        this.teamManager = new TeamManager(this);

        this.teamSelectorInventory = new TeamSelectorInventory(this);

        this.registerListeners();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        // PLAYER
        pluginManager.registerEvents(new JoinListener(this), this);
        pluginManager.registerEvents(new QuitListener(this), this);
        pluginManager.registerEvents(new InteractListener(this), this);
        pluginManager.registerEvents(new InventoryClickListener(this), this);
    }

    public static ForceItemBattle getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public TeamSelectorInventory getTeamSelectorInventory() {
        return teamSelectorInventory;
    }
}
