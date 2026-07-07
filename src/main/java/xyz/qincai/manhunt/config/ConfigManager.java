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
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        mergeWithDefaults("config.yml", config, configFile);
        mergeWithDefaults("messages.yml", messages, messagesFile);
    }

    public void reloadConfigs() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        mergeWithDefaults("config.yml", config, configFile);
        mergeWithDefaults("messages.yml", messages, messagesFile);
    }

    private void mergeWithDefaults(String resourcePath,
                                    FileConfiguration userConfig, File file) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) return;

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            int defaultVersion = defaultConfig.getInt("config-version", 0);
            int userVersion = userConfig.getInt("config-version", 0);

            if (userVersion >= defaultVersion) return;

            plugin.getLogger().info("Updating " + file.getName()
                    + " from v" + userVersion + " to v" + defaultVersion);

            if (mergeSection(defaultConfig, userConfig, true)) {
                userConfig.set("config-version", defaultVersion);
                userConfig.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not save updated " + file.getName(), e);
        }
    }

    private static boolean mergeSection(ConfigurationSection defaultSection,
                                         ConfigurationSection userSection,
                                         boolean isRoot) {
        boolean changed = false;
        for (String key : defaultSection.getKeys(false)) {
            if (isRoot && "config-version".equals(key)) continue;

            if (defaultSection.isConfigurationSection(key)) {
                ConfigurationSection defaultSub = defaultSection.getConfigurationSection(key);
                if (defaultSub == null) continue;

                if (!userSection.contains(key)) {
                    userSection.createSection(key);
                    changed = true;
                }
                ConfigurationSection userSub = userSection.getConfigurationSection(key);
                if (userSub != null
                        && mergeSection(defaultSub, userSub, false)) {
                    changed = true;
                }
            } else {
                if (!userSection.contains(key)) {
                    userSection.set(key, defaultSection.get(key));
                    changed = true;
                }
            }
        }
        return changed;
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

    public int getFormationSearchRadius() {
        return config.getInt("preHunt.formationSearchRadius", 20);
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
