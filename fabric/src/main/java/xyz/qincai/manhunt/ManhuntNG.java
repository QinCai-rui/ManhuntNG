package xyz.qincai.manhunt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.qincai.manhunt.fabric.*;
import xyz.qincai.manhunt.game.GameManager;
import xyz.qincai.manhunt.platform.*;

import java.util.logging.Level;

public class ManhuntNG implements ModInitializer, ManhuntPlatform {
    public static final String MOD_ID = "manhuntng";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ManhuntNG instance;

    private GameManager gameManager;
    private FabricConfigProvider configProvider;
    private FabricPlayerRegistry playerRegistry;
    private FabricUIFacade uiFacade;
    private FabricWorldProvider worldProvider;
    private FabricScheduler scheduler;
    private MinecraftServer server;
    private final java.util.logging.Logger julLogger = new Slf4jLogger();

    private static class Slf4jLogger extends java.util.logging.Logger {
        Slf4jLogger() {
            super(MOD_ID, null);
        }
        @Override public void info(String msg) { LOGGER.info(msg); }
        @Override public void warning(String msg) { LOGGER.warn(msg); }
        @Override public void severe(String msg) { LOGGER.error(msg); }
        @Override public void log(java.util.logging.Level level, String msg) {
            if (level.intValue() >= Level.SEVERE.intValue()) LOGGER.error(msg);
            else if (level.intValue() >= Level.WARNING.intValue()) LOGGER.warn(msg);
            else LOGGER.info(msg);
        }
    }

    @Override
    public void onInitialize() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.server = server;
            this.scheduler = new FabricScheduler(server);
            java.io.File dataFolder = new java.io.File(server.getServerDirectory().toFile(), "config/" + MOD_ID);
            this.configProvider = new FabricConfigProvider(dataFolder, julLogger);
            this.configProvider.loadConfigs();
            this.playerRegistry = new FabricPlayerRegistry();
            this.uiFacade = new FabricUIFacade();
            this.worldProvider = new FabricWorldProvider(this);
            this.gameManager = new GameManager(this);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryContext, environment) -> {
            FabricCommandRegistrar.register(dispatcher, registryContext, this);
        });

        LOGGER.info("ManhuntNG has been initialized!");
    }

    public static ManhuntNG getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public MinecraftServer getServer() { return server; }

    @Override public java.util.logging.Logger getLogger() { return julLogger; }
    @Override public Scheduler getScheduler() { return scheduler; }
    @Override public ConfigProvider getConfigProvider() { return configProvider; }
    @Override public UIFacade getUIFacade() { return uiFacade; }
    @Override public PlayerRegistry getPlayerRegistry() { return playerRegistry; }
    @Override public WorldProvider getWorldProvider() { return worldProvider; }
    @Override public String getPlatformName() { return "fabric"; }
}
