package xyz.qincai.manhunt.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FabricConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ManhuntConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Path configPath;
    private ManhuntConfig config;

    public record ManhuntConfig(
            int configVersion,
            int countdownSeconds,
            int preHuntSeconds,
            boolean trackingEnabled,
            int trackingInterval,
            boolean autoGenerateWorlds,
            String worldPrefix,
            List<String> runnerEffects,
            List<String> hunterEffects
    ) {
        public static ManhuntConfig defaults() {
            return new ManhuntConfig(
                    1,
                    30,
                    60,
                    true,
                    20,
                    true,
                    "manhunt",
                    List.of("speed:2:infinite"),
                    List.of("speed:1:infinite")
            );
        }
    }

    public void load(MinecraftServer server) {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("manhuntng.json");
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) {
                config = GSON.fromJson(Files.readString(configPath), ManhuntConfig.class);
                LOGGER.info("Config loaded");
            } else {
                config = ManhuntConfig.defaults();
                save();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
            config = ManhuntConfig.defaults();
        }
    }

    public void save() {
        try {
            Files.writeString(configPath, GSON.toJson(config));
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public ManhuntConfig getConfig() {
        return config;
    }
}
