package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.gui.TeamSelectorInventory;
import de.thisisfel1x.forceitembattle.listeners.player.InteractListener;
import de.thisisfel1x.forceitembattle.listeners.player.InventoryClickListener;
import de.thisisfel1x.forceitembattle.listeners.player.JoinListener;
import de.thisisfel1x.forceitembattle.listeners.player.QuitListener;
import de.thisisfel1x.forceitembattle.teams.TeamManager;
import de.thisisfel1x.forceitembattle.utils.ForceItemBattleScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ForceItemBattle extends JavaPlugin {

    private static ForceItemBattle instance;

    // Managers
    private GameManager gameManager;
    private TeamManager teamManager;
    private ForceItemBattleScoreboardManager forceItemBattleScoreboardManager;

    // Inventories
    private TeamSelectorInventory teamSelectorInventory;

    @Override
    public void onEnable() {
        instance = this;

        this.forceItemBattleScoreboardManager = new ForceItemBattleScoreboardManager(this);
        this.gameManager = new GameManager(this);
        this.teamManager = new TeamManager(this);

        this.teamSelectorInventory = new TeamSelectorInventory(this);

        this.registerListeners();
    }

    @Override
    public void onDisable() {
        if (this.forceItemBattleScoreboardManager != null) {
            this.forceItemBattleScoreboardManager.cleanup();
        }
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

    public Component getPrefix() {
        return Component.text("ForceItemBattle", TextColor.fromHexString("#F9A03F"))
                .append(Component.text(" ● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("", NamedTextColor.GRAY));
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ForceItemBattleScoreboardManager getForceItemBattleScoreboardManager() {
        return forceItemBattleScoreboardManager;
    }

    public TeamSelectorInventory getTeamSelectorInventory() {
        return teamSelectorInventory;
    }


}
