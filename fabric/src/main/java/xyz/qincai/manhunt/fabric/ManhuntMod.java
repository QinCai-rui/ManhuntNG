package xyz.qincai.manhunt.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.qincai.manhunt.fabric.command.ManhuntCommand;
import xyz.qincai.manhunt.fabric.config.FabricConfigManager;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;
import xyz.qincai.manhunt.fabric.listener.FabricGameListener;

public class ManhuntMod implements ModInitializer {
    public static final String MOD_ID = "manhuntng";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ManhuntMod instance;
    private MinecraftServer server;
    private FabricGameManager gameManager;
    private FabricConfigManager configManager;
    private FabricGameListener gameListener;

    public static ManhuntMod getInstance() {
        return instance;
    }

    @Override
    public void onInitialize() {
        instance = this;

        this.configManager = new FabricConfigManager();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.server = server;
            this.configManager.load(server);
            this.gameManager = new FabricGameManager(server);
            this.gameListener = new FabricGameListener(gameManager);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (gameManager != null) {
                gameManager.shutdown();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ManhuntCommand.register(dispatcher, this);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (gameManager != null) {
                gameManager.onPlayerRespawn(newPlayer);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> {
            if (gameManager != null) {
                gameManager.onPlayerJoin(handler.getPlayer());
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> {
            if (gameManager != null) {
                gameManager.onPlayerDisconnect(handler.getPlayer());
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server1 -> {
            if (gameManager != null) {
                gameManager.tick();
            }
        });

        LOGGER.info("ManhuntNG initialized");
    }

    public MinecraftServer getServer() {
        return server;
    }

    public FabricGameManager getGameManager() {
        return gameManager;
    }

    public FabricConfigManager getConfigManager() {
        return configManager;
    }
}
