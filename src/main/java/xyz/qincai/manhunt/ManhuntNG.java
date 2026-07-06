package xyz.qincai.manhunt;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.qincai.manhunt.command.CommandRegistrar;
import xyz.qincai.manhunt.command.ManhuntCommand;
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.formation.FormationManager;
import xyz.qincai.manhunt.game.GameManager;
import xyz.qincai.manhunt.listener.GameListener;
import xyz.qincai.manhunt.player.PlayerManager;
import xyz.qincai.manhunt.stats.StatisticsManager;
import xyz.qincai.manhunt.tracker.TrackerManager;
import xyz.qincai.manhunt.ui.UIManager;
import xyz.qincai.manhunt.world.WorldManager;

public class ManhuntNG extends JavaPlugin {
    private static ManhuntNG instance;
    private ConfigManager configManager;
    private GameManager gameManager;
    private PlayerManager playerManager;
    private FormationManager formationManager;
    private TrackerManager trackerManager;
    private WorldManager worldManager;
    private UIManager uiManager;
    private StatisticsManager statsManager;
    private CommandRegistrar commandRegistrar;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        gameManager = new GameManager(this);
        playerManager = new PlayerManager(this);
        formationManager = new FormationManager(this);
        trackerManager = new TrackerManager(this);
        worldManager = new WorldManager(this);
        uiManager = new UIManager(this);
        statsManager = new StatisticsManager(this);

        trackerManager.init();
        uiManager.init();

        commandRegistrar = new CommandRegistrar(this);
        registerCommands();

        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getLogger().info("ManhuntNG has been enabled!");
    }

    @Override
    public void onDisable() {
        if (commandRegistrar != null) {
            commandRegistrar.unregisterAll();
        }
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.stopGame();
        }
        getLogger().info("ManhuntNG has been disabled!");
    }

    private void registerCommands() {
        ManhuntCommand manhuntCommand = new ManhuntCommand(this);
        commandRegistrar.register("manhunt", "ManhuntNG main command", manhuntCommand, manhuntCommand);
    }

    public static ManhuntNG getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public FormationManager getFormationManager() {
        return formationManager;
    }

    public TrackerManager getTrackerManager() {
        return trackerManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public UIManager getUiManager() {
        return uiManager;
    }

    public StatisticsManager getStatsManager() {
        return statsManager;
    }
}
