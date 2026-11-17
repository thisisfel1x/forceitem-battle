package de.thisisfel1x.forceitembattle;

import de.thisisfel1x.forceitembattle.commands.BackPackCommand;
import de.thisisfel1x.forceitembattle.commands.RecipeCommand;
import de.thisisfel1x.forceitembattle.commands.SpawnCommand;
import de.thisisfel1x.forceitembattle.commands.StartCommand;
import de.thisisfel1x.forceitembattle.commands.TopCommand;
import de.thisisfel1x.forceitembattle.game.GameManager;
import de.thisisfel1x.forceitembattle.game.ItemTracker;
import de.thisisfel1x.forceitembattle.gui.GuiListener;
import de.thisisfel1x.forceitembattle.gui.SettingsInventory;
import de.thisisfel1x.forceitembattle.gui.SpectatorPlayerInventory;
import de.thisisfel1x.forceitembattle.gui.TeamSelectorInventory;
import de.thisisfel1x.forceitembattle.listeners.block.BlockBreakListener;
import de.thisisfel1x.forceitembattle.listeners.block.BlockPlaceListener;
import de.thisisfel1x.forceitembattle.listeners.entity.EntityDamageListener;
import de.thisisfel1x.forceitembattle.listeners.entity.EntityExhaustionListener;
import de.thisisfel1x.forceitembattle.listeners.player.DropItemListener;
import de.thisisfel1x.forceitembattle.listeners.player.FoodLevelChangeListener;
import de.thisisfel1x.forceitembattle.listeners.player.InteractListener;
import de.thisisfel1x.forceitembattle.listeners.player.InventoryClickListener;
import de.thisisfel1x.forceitembattle.listeners.player.ItemFoundListener;
import de.thisisfel1x.forceitembattle.listeners.player.JoinListener;
import de.thisisfel1x.forceitembattle.listeners.player.PlayerChangedWorldListener;
import de.thisisfel1x.forceitembattle.listeners.player.PlayerMoveListener;
import de.thisisfel1x.forceitembattle.listeners.player.QuitListener;
import de.thisisfel1x.forceitembattle.listeners.player.SpectatorListener;
import de.thisisfel1x.forceitembattle.settings.SettingsManager;
import de.thisisfel1x.forceitembattle.teams.TeamManager;
import de.thisisfel1x.forceitembattle.utils.ForceItemBattleScoreboardManager;
import de.thisisfel1x.forceitembattle.utils.IconManager;
import de.thisisfel1x.forceitembattle.utils.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

public final class ForceItemBattle extends JavaPlugin {

    private static ForceItemBattle instance;

    private int gameTime = -1;
    private int jokerAmount = -1;

    // Managers
    private GameManager gameManager;
    private TeamManager teamManager;
    private ItemTracker itemTracker;

    private IconManager iconManager;

    private ScoreboardLibrary scoreboardLibrary;
    private ForceItemBattleScoreboardManager forceItemBattleScoreboardManager;
    private SettingsManager settingsManager;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Inventories
    private TeamSelectorInventory teamSelectorInventory;
    private SpectatorPlayerInventory spectatorPlayerInventory;
    private SettingsInventory settingsInventory;

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();

        this.iconManager = new IconManager(this);
        this.iconManager.loadIcons();

        ItemRegistry.initialize(this.getConfig().getStringList("items.additional-banned"), this.getLogger());
        this.warnAboutMissingIcons();

        try {
            this.scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException ignored) {
            this.scoreboardLibrary = new NoopScoreboardLibrary();
            this.getLogger().warning("No scoreboard packet adapter available!");
        }
        this.forceItemBattleScoreboardManager = new ForceItemBattleScoreboardManager(this);

        this.teamManager = new TeamManager(this);
        this.itemTracker = new ItemTracker(this);
        this.gameManager = new GameManager(this);
        this.settingsManager = new SettingsManager(this);

        this.teamSelectorInventory = new TeamSelectorInventory(this);
        this.spectatorPlayerInventory = new SpectatorPlayerInventory(this);
        this.settingsInventory = new SettingsInventory(this);

        this.registerListeners();
        this.registerCommands();

        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRules.KEEP_INVENTORY, true);
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        });
    }

    @Override
    public void onDisable() {
        if (this.forceItemBattleScoreboardManager != null) {
            this.forceItemBattleScoreboardManager.cleanup();
        }

        if (this.scoreboardLibrary != null) {
            this.scoreboardLibrary.close();
        }
    }

    /**
     * The icon mapping belongs to a specific resource pack version. Items that were added to the
     * game afterwards have no glyph and are shown by name only.
     */
    private void warnAboutMissingIcons() {
        if (!this.iconManager.isLoaded()) return;

        List<String> missing = ItemRegistry.getFindableMaterials().stream()
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .filter(name -> !this.iconManager.hasIcon("fib_" + name))
                .sorted()
                .toList();

        if (!missing.isEmpty()) {
            this.getLogger().warning(missing.size() + " Items im Pool haben kein Icon im Resource-Pack-Mapping "
                    + "(werden nur mit Namen angezeigt): " + String.join(", ", missing));
        }
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        // GUI
        pluginManager.registerEvents(new GuiListener(), this);

        // ENTITY
        pluginManager.registerEvents(new EntityDamageListener(this), this);
        pluginManager.registerEvents(new EntityExhaustionListener(this), this);

        // PLAYER
        pluginManager.registerEvents(new JoinListener(this), this);
        pluginManager.registerEvents(new QuitListener(this), this);
        pluginManager.registerEvents(new InteractListener(this), this);
        pluginManager.registerEvents(new InventoryClickListener(this), this);
        pluginManager.registerEvents(new ItemFoundListener(this), this);
        pluginManager.registerEvents(new FoodLevelChangeListener(this), this);
        pluginManager.registerEvents(new PlayerMoveListener(this), this);
        pluginManager.registerEvents(new DropItemListener(this), this);
        pluginManager.registerEvents(new PlayerChangedWorldListener(this), this);
        pluginManager.registerEvents(new SpectatorListener(this), this);

        // BLOCK
        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(new BlockPlaceListener(this), this);
    }

    private void registerCommands() {
        this.registerCommand("start", new StartCommand(this));
        this.registerCommand("itemrecipe", new RecipeCommand(this));
        this.registerCommand("backpack", List.of("bp"), new BackPackCommand(this));
        this.registerCommand("spawn", new SpawnCommand(this));
        this.registerCommand("top", new TopCommand(this));
    }

    public static ForceItemBattle getInstance() {
        return instance;
    }

    public Component getPrefix() {
        return Component.text("ForceItemBattle", TextColor.fromHexString("#F9A03F"))
                .append(Component.text(" ● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("", NamedTextColor.GRAY));
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    /**
     * The primary (overworld) world of the server, independent of its configured name.
     */
    public World getMainWorld() {
        return Bukkit.getWorlds().getFirst();
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ItemTracker getItemTracker() {
        return itemTracker;
    }

    public IconManager getIconManager() {
        return iconManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public ForceItemBattleScoreboardManager getForceItemBattleScoreboardManager() {
        return forceItemBattleScoreboardManager;
    }

    public ScoreboardLibrary getScoreboardLibrary() {
        return scoreboardLibrary;
    }

    public TeamSelectorInventory getTeamSelectorInventory() {
        return teamSelectorInventory;
    }

    public SpectatorPlayerInventory getSpectatorPlayerInventory() {
        return spectatorPlayerInventory;
    }

    public SettingsInventory getSettingsInventory() {
        return settingsInventory;
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
