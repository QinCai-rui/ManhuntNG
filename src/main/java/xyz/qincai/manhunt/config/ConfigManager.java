package xyz.qincai.manhunt.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xyz.qincai.manhunt.ManhuntNG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        checkConfigVersion();
    }

    private void checkConfigVersion() {
        FileConfiguration defaultConfig = loadDefaultConfig();
        if (defaultConfig == null) return;

        int currentVersion = config.getInt("config-version", 0);
        int latestVersion = defaultConfig.getInt("config-version", 1);

        if (currentVersion < latestVersion) {
            plugin.getLogger().info("Config outdated (v" + currentVersion + "). Merging new options from v" + latestVersion + "...");
            mergeMissingKeys(defaultConfig, config);
            config.set("config-version", latestVersion);
            try {
                config.save(configFile);
                plugin.getLogger().info("Config updated to v" + latestVersion);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not save updated config", e);
            }
        }
    }

    private FileConfiguration loadDefaultConfig() {
        InputStream in = plugin.getResource("config.yml");
        if (in == null) return null;
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private void mergeMissingKeys(ConfigurationSection source, ConfigurationSection target) {
        mergeSection(source, target, "");
    }

    private void mergeSection(ConfigurationSection source, ConfigurationSection target, String prefix) {
        for (String key : source.getKeys(false)) {
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            if (source.isConfigurationSection(key)) {
                ConfigurationSection childSource = source.getConfigurationSection(key);
                if (childSource == null) continue;
                if (!target.isConfigurationSection(fullPath)) {
                    for (String childKey : childSource.getKeys(true)) {
                        String childPath = fullPath + "." + childKey;
                        if (!childSource.isConfigurationSection(childKey)) {
                            target.set(childPath, childSource.get(childKey));
                        }
                    }
                } else {
                    ConfigurationSection childTarget = target.getConfigurationSection(fullPath);
                    if (childTarget != null) {
                        mergeSection(childSource, childTarget, "");
                    }
                }
            } else if (!target.contains(fullPath)) {
                target.set(fullPath, source.get(key));
            }
        }
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

    public List<PotionEffect> getRunnerPotionEffects() {
        return getPotionEffects("potionEffects.runner");
    }

    public List<PotionEffect> getHunterPotionEffects() {
        return getPotionEffects("potionEffects.hunters");
    }

    private List<PotionEffect> getPotionEffects(String path) {
        List<PotionEffect> effects = new ArrayList<>();
        List<?> list = config.getList(path);
        if (list == null) return effects;

        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            Object typeObj = map.get("type");
            if (typeObj == null) continue;
            PotionEffectType type = PotionEffectType.getByName(typeObj.toString());
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect type: " + typeObj);
                continue;
            }

            int level = 1;
            Object levelObj = map.get("level");
            if (levelObj instanceof Number num) {
                level = num.intValue();
            }

            int duration = -1;
            Object durationObj = map.get("duration");
            if (durationObj instanceof Number num) {
                duration = num.intValue();
            }
            if (duration < 0) {
                duration = Integer.MAX_VALUE;
            }

            boolean ambient = true;
            Object ambientObj = map.get("ambient");
            if (ambientObj instanceof Boolean bool) {
                ambient = bool;
            }

            effects.add(new PotionEffect(type, duration, level - 1, ambient, true));
        }
        return effects;
    }
}
