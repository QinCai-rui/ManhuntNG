package xyz.qincai.manhunt;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.qincai.manhunt.game.GameManager;
import xyz.qincai.manhunt.paper.*;
import xyz.qincai.manhunt.paper.command.PaperCommandRegistrar;
import xyz.qincai.manhunt.paper.command.ManhuntCommand;
import xyz.qincai.manhunt.paper.listener.GameListener;
import xyz.qincai.manhunt.platform.*;

import java.util.logging.Logger;

public class PaperManhuntNG extends JavaPlugin implements ManhuntPlatform {
    private static PaperManhuntNG instance;
    private GameManager gameManager;
    private PaperConfigProvider configProvider;
    private PaperPlayerRegistry playerRegistry;
    private PaperUIFacade uiFacade;
    private PaperWorldProvider worldProvider;
    private PaperScheduler scheduler;
    private PaperCommandRegistrar commandRegistrar;

    @Override
    public void onEnable() {
        instance = this;

        this.scheduler = new PaperScheduler(this);
        this.configProvider = new PaperConfigProvider(this);
        this.configProvider.loadConfigs();
        this.playerRegistry = new PaperPlayerRegistry(this);
        this.uiFacade = new PaperUIFacade(this);
        this.worldProvider = new PaperWorldProvider(this);

        this.gameManager = new GameManager(this);

        this.commandRegistrar = new PaperCommandRegistrar(this);
        ManhuntCommand manhuntCommand = new ManhuntCommand(this);
        this.commandRegistrar.register("manhunt", "ManhuntNG main command", manhuntCommand, manhuntCommand);

        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getLogger().info("ManhuntNG has been enabled!");
    }

    @Override
    public void onDisable() {
        if (commandRegistrar != null) commandRegistrar.unregisterAll();
        if (gameManager != null && gameManager.isGameActive()) gameManager.stopGame();
        getLogger().info("ManhuntNG has been disabled!");
    }

    public static PaperManhuntNG getInstance() { return instance; }

    @Override public Logger getLogger() { return super.getLogger(); }
    @Override public Scheduler getScheduler() { return scheduler; }
    @Override public ConfigProvider getConfigProvider() { return configProvider; }
    @Override public UIFacade getUIFacade() { return uiFacade; }
    @Override public PlayerRegistry getPlayerRegistry() { return playerRegistry; }
    @Override public WorldProvider getWorldProvider() { return worldProvider; }
    @Override public String getPlatformName() { return "paper"; }

    public GameManager getGameManager() { return gameManager; }
    public PaperConfigProvider getPaperConfigProvider() { return configProvider; }
}
