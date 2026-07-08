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
        // get config file references
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Copy default files from jar file if missing
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // Load YAML files into memory
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // merge missing keys from defaults (so that updates don't break old configs)
        mergeWithDefaults("config.yml", config, configFile);
        mergeWithDefaults("messages.yml", messages, messagesFile);

        // Validate (on load)
        validateConfig();
    }

    public void reloadConfigs() {
        // Reload config files without overwriting user changes
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

        // Validate (on reload)
        validateConfig();
    }

    /*
     * Validates all known config keys once at load/reload.
     */
    private void validateConfig() {
        plugin.getLogger().info("[Config] Validating configuration...");

        validateInt("headstart.duration", 10);
        validateBoolean("headstart.compassEnabled", false);

        validateInt("preHunt.countdown", 5);
        validateDouble("preHunt.hunterCircleRadius", 3.0);
        validateInt("preHunt.formationSearchRadius", 20);

        validateBoolean("tracking.enabled", true);
        validateInt("tracking.updateTicks", 5);
        validateBoolean("tracking.showDistance", true);

        validateInt("runner.lives", 1);

        validateBoolean("hunters.infiniteRespawns", true);
        validateInt("hunters.respawnLimit", -1);

        validateBoolean("scoreboard.enabled", true);
        validateBoolean("actionBar.enabled", true);

        validateBoolean("hunters.keepInventory", false);
        validateBoolean("hunters.keepArmor", false);
        validateBoolean("hunters.keepOffhand", false);

        validatePotionEffects("potionEffects.runner");
        validatePotionEffects("potionEffects.hunters");

        plugin.getLogger().info("[Config] Validation complete.");
    }

    private void validateInt(String path, int fallback) {
        if (!config.contains(path)) {
            plugin.getLogger().warning("[Config] Missing '" + path + "'. Using fallback: " + fallback);
            return;
        }
        Object raw = config.get(path);
        if (!(raw instanceof Number)) {
            plugin.getLogger().warning("[Config] Invalid type for '" + path +
                    "'. Expected int, got " + raw.getClass().getSimpleName() +
                    ". Using fallback: " + fallback);
        }
    }

    private void validateDouble(String path, double fallback) {
        if (!config.contains(path)) {
            plugin.getLogger().warning("[Config] Missing '" + path + "'. Using fallback: " + fallback);
            return;
        }
        Object raw = config.get(path);
        if (!(raw instanceof Number)) {
            plugin.getLogger().warning("[Config] Invalid type for '" + path +
                    "'. Expected double, got " + raw.getClass().getSimpleName() +
                    ". Using fallback: " + fallback);
        }
    }

    private void validateBoolean(String path, boolean fallback) {
        if (!config.contains(path)) {
            plugin.getLogger().warning("[Config] Missing '" + path + "'. Using fallback: " + fallback);
            return;
        }
        Object raw = config.get(path);
        if (!(raw instanceof Boolean)) {
            plugin.getLogger().warning("[Config] Invalid type for '" + path +
                    "'. Expected boolean, got " + raw.getClass().getSimpleName() +
                    ". Using fallback: " + fallback);
        }
    }

    private void validatePotionEffects(String path) {
        List<?> list = config.getList(path);

        if (list == null) {
            plugin.getLogger().warning("[Config] Missing potion effect list at '" + path + "'. Using empty list.");
            return;
        }

        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                plugin.getLogger().warning("[Config] Invalid potion effect entry at '" + path + "'. Expected a map.");
                continue;
            }

            Object typeObj = map.get("type");
            if (typeObj == null) {
                plugin.getLogger().warning("[Config] Potion effect missing 'type' at '" + path + "'. Skipping entry.");
                continue;
            }

            PotionEffectType type = PotionEffectType.getByName(typeObj.toString());
            if (type == null) {
                plugin.getLogger().warning("[Config] Unknown potion effect type '" + typeObj + "' at '" + path + "'. Skipping entry.");
                continue;
            }

            if (!(map.get("level") instanceof Number)) {
                plugin.getLogger().warning("[Config] Potion effect '" + type.getName()
                        + "' missing or invalid 'level' at '" + path + "'. Using default level = 1.");
            }

            if (!(map.get("duration") instanceof Number)) {
                plugin.getLogger().warning("[Config] Potion effect '" + type.getName()
                        + "' missing or invalid 'duration' at '" + path + "'. Using infinite duration.");
            }

            if (!(map.get("ambient") instanceof Boolean)) {
                plugin.getLogger().warning("[Config] Potion effect '" + type.getName()
                        + "' missing or invalid 'ambient' at '" + path + "'. Using default ambient = true.");
            }
        }
    }

    /**
     * Merges missing keys from the default config into the user's config.
     * This allows new config options to be added without breaking old installs.
     */
    private void mergeWithDefaults(String resourcePath,
                                   FileConfiguration userConfig, File file) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) return; // No default resource found

            // Load default config from plugin JAR
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            // Versioning system: prevents overwriting user configs unless needed
            int defaultVersion = defaultConfig.getInt("config-version", 0);
            int userVersion = userConfig.getInt("config-version", 0);

            // If user config is up-to-date, skip merging
            if (userVersion >= defaultVersion) return;

            plugin.getLogger().info("Updating " + file.getName()
                    + " from v" + userVersion + " to v" + defaultVersion);

            // Merge missing keys recursively
            if (mergeSection(defaultConfig, userConfig, true)) {
                userConfig.set("config-version", defaultVersion);
                userConfig.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not save updated " + file.getName(), e);
        }
    }

    /**
     * Recursively merges missing keys from defaultSection into userSection.
     * Does NOT overwrite existing user values.
     */
    private static boolean mergeSection(ConfigurationSection defaultSection,
                                        ConfigurationSection userSection,
                                        boolean isRoot) {
        boolean changed = false;

        for (String key : defaultSection.getKeys(false)) {
            // Root-level version key is handled separately
            if (isRoot && "config-version".equals(key)) continue;

            if (defaultSection.isConfigurationSection(key)) {
                // Handle nested sections
                ConfigurationSection defaultSub = defaultSection.getConfigurationSection(key);
                if (defaultSub == null) continue;

                if (!userSection.contains(key)) {
                    // Create missing section
                    userSection.createSection(key);
                    changed = true;
                }

                ConfigurationSection userSub = userSection.getConfigurationSection(key);
                if (userSub != null && mergeSection(defaultSub, userSub, false)) {
                    changed = true;
                }
            } else {
                // Handle simple values
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

    /*
     * Retrieves a message and applies placeholder replacements.
     */
    public String getMessage(String key, String... replacements) {
        String msg = messages.getString(key, key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    // -------- Config getters --------
    public int getHeadstartDuration() {
        return config.getInt("headstart.duration", 10);
    }

    public boolean isHeadstartCompassEnabled() {
        return config.getBoolean("headstart.compassEnabled", false);
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

    public boolean isTrackingShowDistance() {
        return config.getBoolean("tracking.showDistance", true);
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

    public boolean isHunterKeepOffhand() {
        return config.getBoolean("hunters.keepOffhand", false);
    }

    public List<PotionEffect> getRunnerPotionEffects() {
        return getPotionEffects("potionEffects.runner");
    }

    public List<PotionEffect> getHunterPotionEffects() {
        return getPotionEffects("potionEffects.hunters");
    }

    /*
     * Parses potion effects from config.
     * EG:
     *   - type: SPEED
     *   - level: 2
     *   - duration: 200 (ticks)
     *   - ambient: true
     *
     * Missing values fallback
     */
    private List<PotionEffect> getPotionEffects(String path) {
        List<PotionEffect> effects = new ArrayList<>();
        List<?> list = config.getList(path);
        if (list == null) return effects;

        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            // Effect type
            Object typeObj = map.get("type");
            if (typeObj == null) continue;

            PotionEffectType type = PotionEffectType.getByName(typeObj.toString());
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect type: " + typeObj);
                continue;
            }

            // Level (amplifier)
            int level = 1;
            Object levelObj = map.get("level");
            if (levelObj instanceof Number num) {
                level = num.intValue();
            }

            // Duration (ticks)
            int duration = -1;
            Object durationObj = map.get("duration");
            if (durationObj instanceof Number num) {
                duration = num.intValue();
            }
            if (duration < 0) {
                // Infinite duration fallback
                duration = Integer.MAX_VALUE;
            }

            // Ambient flag
            boolean ambient = true;
            Object ambientObj = map.get("ambient");
            if (ambientObj instanceof Boolean bool) {
                ambient = bool;
            }

            // Add parsed effect
            effects.add(new PotionEffect(type, duration, level - 1, ambient, true));
        }
        return effects;
    }
}
