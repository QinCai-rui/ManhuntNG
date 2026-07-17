package xyz.qincai.manhunt;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.qincai.manhunt.chat.ChatManager;
import xyz.qincai.manhunt.command.ChatCommand;
import xyz.qincai.manhunt.command.CommandRegistrar;
import xyz.qincai.manhunt.command.ManhuntCommand;
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.formation.FormationManager;
import xyz.qincai.manhunt.game.GameManager;
import xyz.qincai.manhunt.listener.AdvancementListener;
import xyz.qincai.manhunt.listener.CombatListener;
import xyz.qincai.manhunt.listener.GameListenerState;
import xyz.qincai.manhunt.listener.GamePhaseListener;
import xyz.qincai.manhunt.listener.PlayerLifecycleListener;
import xyz.qincai.manhunt.listener.WorldInteractionListener;
import xyz.qincai.manhunt.loot.LootListener;
import xyz.qincai.manhunt.loot.LootManager;
import xyz.qincai.manhunt.player.PlayerManager;
import xyz.qincai.manhunt.player.NameTagManager;
import xyz.qincai.manhunt.player.PotionEffectManager;
import xyz.qincai.manhunt.stats.StatisticsManager;
import xyz.qincai.manhunt.tracker.TrackerManager;
import xyz.qincai.manhunt.ui.UIManager;
import xyz.qincai.manhunt.world.WorldManager;

public class ManhuntNG extends JavaPlugin {
    private static ManhuntNG instance;
    private ConfigManager configManager;
    private GameManager gameManager;
    private PlayerManager playerManager;
    private NameTagManager nameTagManager;
    private PotionEffectManager potionEffectManager;
    private FormationManager formationManager;
    private TrackerManager trackerManager;
    private WorldManager worldManager;
    private UIManager uiManager;
    private StatisticsManager statsManager;
    private ChatManager chatManager;
    private GameListenerState gameListenerState;
    private LootManager lootManager;
    private LootListener lootListener;
    private CommandRegistrar commandRegistrar;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        gameManager = new GameManager(this);
        playerManager = new PlayerManager(this);
        nameTagManager = new NameTagManager(this);
        potionEffectManager = new PotionEffectManager(this);
        formationManager = new FormationManager(this);
        trackerManager = new TrackerManager(this);
        worldManager = new WorldManager(this);
        uiManager = new UIManager(this);
        statsManager = new StatisticsManager(this);
        chatManager = new ChatManager(this);
        lootManager = new LootManager(this);
        lootManager.loadConfig();

        trackerManager.init();
        uiManager.init();

        commandRegistrar = new CommandRegistrar(this);
        ManhuntCommand manhuntCommand = new ManhuntCommand(this);
        commandRegistrar.register("manhunt", "ManhuntNG main command", manhuntCommand, manhuntCommand);

        gameListenerState = new GameListenerState(this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, gameListenerState), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, gameListenerState), this);
        getServer().getPluginManager().registerEvents(new WorldInteractionListener(this, gameListenerState), this);
        getServer().getPluginManager().registerEvents(new GamePhaseListener(this, gameListenerState), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(this), this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        lootListener = new LootListener(this);
        getServer().getPluginManager().registerEvents(lootListener, this);

        commandRegistrar.register("g", "Send a global message", new ChatCommand(this, true), null);
        commandRegistrar.register("t", "Send a team message", new ChatCommand(this, false), null);

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

    public NameTagManager getNameTagManager() {
        return nameTagManager;
    }

    public PotionEffectManager getPotionEffectManager() {
        return potionEffectManager;
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

    public ChatManager getChatManager() {
        return chatManager;
    }

    public GameListenerState getGameListenerState() {
        return gameListenerState;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public LootListener getLootListener() {
        return lootListener;
    }
}
