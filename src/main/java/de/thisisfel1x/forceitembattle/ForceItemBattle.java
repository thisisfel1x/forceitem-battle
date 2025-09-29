package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.listeners.JoinListener;
import de.thisisfel1x.forceitembattle.listeners.QuitListener;
import de.thisisfel1x.forceitembattle.teams.TeamManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForceItemBattle extends JavaPlugin {

    private static ForceItemBattle instance;

    // Managers
    private GameManager gameManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;

        this.gameManager = new GameManager(this);
        this.teamManager = new TeamManager(this);

        this.registerListeners();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        pluginManager.registerEvents(new JoinListener(this), this);
        pluginManager.registerEvents(new QuitListener(this), this);
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
}
