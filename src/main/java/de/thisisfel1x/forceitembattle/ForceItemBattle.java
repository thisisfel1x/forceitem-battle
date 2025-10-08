package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.commands.BackPackCommand;
import de.thisisfel1x.forceitembattle.commands.RecipeCommand;
import de.thisisfel1x.forceitembattle.commands.StartCommand;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.gui.SpectatorPlayerInventory;
import de.thisisfel1x.forceitembattle.gui.TeamSelectorInventory;
import de.thisisfel1x.forceitembattle.listeners.block.BlockBreakListener;
import de.thisisfel1x.forceitembattle.listeners.block.BlockPlaceListener;
import de.thisisfel1x.forceitembattle.listeners.entity.EntityDamageListener;
import de.thisisfel1x.forceitembattle.listeners.player.*;
import de.thisisfel1x.forceitembattle.teams.TeamManager;
import de.thisisfel1x.forceitembattle.utils.ForceItemBattleScoreboardManager;
import de.thisisfel1x.forceitembattle.utils.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ForceItemBattle extends JavaPlugin {

    private static ForceItemBattle instance;

    private int gameTime = -1;
    private int jokerAmount = -1;

    // Managers
    private GameManager gameManager;
    private TeamManager teamManager;
    private ForceItemBattleScoreboardManager forceItemBattleScoreboardManager;

    private final Map<String, String> textureMap = new HashMap<>();

    // Inventories
    private TeamSelectorInventory teamSelectorInventory;
    private SpectatorPlayerInventory spectatorPlayerInventory;

    @Override
    public void onEnable() {
        instance = this;

        this.loadTextureMap();

        ItemRegistry.initialize();

        this.forceItemBattleScoreboardManager = new ForceItemBattleScoreboardManager(this);
        this.gameManager = new GameManager(this);
        this.teamManager = new TeamManager(this);

        this.teamSelectorInventory = new TeamSelectorInventory(this);
        this.spectatorPlayerInventory = new SpectatorPlayerInventory(this);

        this.registerListeners();
        this.registerCommands();

        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.KEEP_INVENTORY, true));
    }

    @Override
    public void onDisable() {
        if (this.forceItemBattleScoreboardManager != null) {
            this.forceItemBattleScoreboardManager.cleanup();
        }
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        // ENTITY
        pluginManager.registerEvents(new EntityDamageListener(this), this);

        // PLAYER
        pluginManager.registerEvents(new JoinListener(this), this);
        pluginManager.registerEvents(new QuitListener(this), this);
        pluginManager.registerEvents(new InteractListener(this), this);
        pluginManager.registerEvents(new InventoryClickListener(this), this);
        pluginManager.registerEvents(new ItemFoundListener(this), this);
        pluginManager.registerEvents(new FoodLevelChangeListener(this), this);
        pluginManager.registerEvents(new PlayerMoveListener(this), this);

        // BLOCK
        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(new BlockPlaceListener(this), this);
    }

    private void registerCommands() {
        this.registerCommand("start", new StartCommand(this));
        this.registerCommand("itemrecipe", new RecipeCommand(this));
        this.registerCommand("backpack", List.of("bp"), new BackPackCommand(this));
    }

    public static ForceItemBattle getInstance() {
        return instance;
    }

    public Component getPrefix() {
        return Component.text("ForceItemBattle", TextColor.fromHexString("#F9A03F"))
                .append(Component.text(" ● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("", NamedTextColor.GRAY));
    }

    private void loadTextureMap() {
        File configFile = new File(getDataFolder(), "texture_map.yml");
        if (!configFile.exists()) {
            saveResource("texture_map.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String key : config.getKeys(false)) {
            textureMap.put(key, config.getString(key));
        }
        getLogger().info(textureMap.size() + " Textur-Mappings wurden erfolgreich geladen.");
    }

    // Getter, damit andere Klassen darauf zugreifen können
    public Map<String, String> getTextureMap() {
        return textureMap;
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

    public SpectatorPlayerInventory getSpectatorPlayerInventory() {
        return spectatorPlayerInventory;
    }

    public int getGameTime() {
        return gameTime;
    }

    public void setGameTime(int gameTime) {
        this.gameTime = gameTime;
    }

    public int getJokerAmount() {
        return jokerAmount;
    }

    public void setJokerAmount(int jokerAmount) {
        this.jokerAmount = jokerAmount;
    }
}
