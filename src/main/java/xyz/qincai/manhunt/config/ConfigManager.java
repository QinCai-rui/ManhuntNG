package xyz.qincai.manhunt.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.qincai.manhunt.ManhuntNG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {
    private final ManhuntNG plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    public ConfigManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        saveDefaultConfig();
        saveDefaultMessages();
        reloadConfigs();
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defMessagesStream = plugin.getResource("messages.yml");
        if (defMessagesStream != null) {
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defMessagesStream, StandardCharsets.UTF_8));
            messages.setDefaults(defMessages);
        }
    }

    private void saveDefaultConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveDefaultMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String getMessage(String key, String... replacements) {
        String msg = messages.getString(key, key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public int getPreHuntCountdown() {
        return config.getInt("preHunt.countdown", 5);
    }

    public double getHunterCircleRadius() {
        return config.getDouble("preHunt.hunterCircleRadius", 3.0);
    }

    public boolean isTrackingEnabled() {
        return config.getBoolean("tracking.enabled", true);
    }

    public int getTrackingUpdateTicks() {
        return config.getInt("tracking.updateTicks", 5);
    }

    public int getRunnerLives() {
        return config.getInt("runner.lives", 1);
    }

    public boolean isHunterInfiniteRespawns() {
        return config.getBoolean("hunters.infiniteRespawns", true);
    }

    public int getHunterRespawnLimit() {
        return config.getInt("hunters.respawnLimit", -1);
    }

    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", true);
    }

    public boolean isActionBarEnabled() {
        return config.getBoolean("actionBar.enabled", true);
    }

    public boolean isHunterKeepInventory() {
        return config.getBoolean("hunters.keepInventory", false);
    }

    public boolean isHunterKeepArmor() {
        return config.getBoolean("hunters.keepArmor", false);
    }
}
