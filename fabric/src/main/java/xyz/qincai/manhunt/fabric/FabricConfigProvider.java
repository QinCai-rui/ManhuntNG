package xyz.qincai.manhunt.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.qincai.manhunt.platform.ConfigProvider;
import xyz.qincai.manhunt.platform.ManhuntPlatform;
import xyz.qincai.manhunt.player.PlayerEffect;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FabricConfigProvider implements ConfigProvider {
    private final java.io.File dataFolder;
    private final java.util.logging.Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject config;
    private JsonObject messages;

    public FabricConfigProvider(java.io.File dataFolder, java.util.logging.Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public void loadConfigs() {
        loadJson("config.json", true);
        loadJson("messages.json", false);
    }

    private void loadJson(String fileName, boolean isConfig) {
        File file = new File(dataFolder, fileName);
        if (!file.exists()) {
            dataFolder.mkdirs();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                        if (isConfig) {
                            gson.toJson(createDefaultConfig(), writer);
                        } else {
                            gson.toJson(createDefaultMessages(), writer);
                        }
                    }
                }
        } catch (IOException e) {
            logger.severe("Could not create " + fileName + ": " + e.getMessage());
        }
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = gson.fromJson(reader, JsonObject.class);
            if (isConfig) config = obj;
            else messages = obj;
        } catch (IOException e) {
            logger.severe("Could not load " + fileName + ": " + e.getMessage());
        }
    }

    private JsonObject createDefaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("config-version", 1);
        JsonObject preHunt = new JsonObject();
        preHunt.addProperty("countdown", 5);
        preHunt.addProperty("hunterCircleRadius", 3.0);
        preHunt.addProperty("formationSearchRadius", 20);
        root.add("preHunt", preHunt);
        JsonObject tracking = new JsonObject();
        tracking.addProperty("enabled", true);
        tracking.addProperty("updateTicks", 5);
        tracking.addProperty("showDistance", true);
        root.add("tracking", tracking);
        root.addProperty("runner.lives", 1);
        JsonObject hunters = new JsonObject();
        hunters.addProperty("infiniteRespawns", true);
        hunters.addProperty("respawnLimit", -1);
        hunters.addProperty("keepInventory", false);
        hunters.addProperty("keepArmor", false);
        root.add("hunters", hunters);
        JsonObject scoreboard = new JsonObject();
        scoreboard.addProperty("enabled", true);
        root.add("scoreboard", scoreboard);
        JsonObject actionBar = new JsonObject();
        actionBar.addProperty("enabled", true);
        root.add("actionBar", actionBar);
        root.add("potionEffects", new JsonObject());
        return root;
    }

    private JsonObject createDefaultMessages() {
        JsonObject root = new JsonObject();
        root.addProperty("config-version", 1);
        return root;
    }

    @Override
    public void reloadConfigs() { loadConfigs(); }

    @Override
    public String getMessage(String key, String... replacements) {
        if (messages == null) return key;
        JsonElement el = messages.get(key);
        String msg = el != null ? el.getAsString() : key;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    private int getInt(String path, int def) {
        String[] parts = path.split("\\.");
        JsonObject obj = config;
        for (int i = 0; i < parts.length - 1; i++) {
            if (obj == null || !obj.has(parts[i])) return def;
            obj = obj.getAsJsonObject(parts[i]);
        }
        if (obj == null || !obj.has(parts[parts.length - 1])) return def;
        return obj.get(parts[parts.length - 1]).getAsInt();
    }

    private double getDouble(String path, double def) {
        String[] parts = path.split("\\.");
        JsonObject obj = config;
        for (int i = 0; i < parts.length - 1; i++) {
            if (obj == null || !obj.has(parts[i])) return def;
            obj = obj.getAsJsonObject(parts[i]);
        }
        if (obj == null || !obj.has(parts[parts.length - 1])) return def;
        return obj.get(parts[parts.length - 1]).getAsDouble();
    }

    private boolean getBool(String path, boolean def) {
        String[] parts = path.split("\\.");
        JsonObject obj = config;
        for (int i = 0; i < parts.length - 1; i++) {
            if (obj == null || !obj.has(parts[i])) return def;
            obj = obj.getAsJsonObject(parts[i]);
        }
        if (obj == null || !obj.has(parts[parts.length - 1])) return def;
        return obj.get(parts[parts.length - 1]).getAsBoolean();
    }

    @Override public int getPreHuntCountdown() { return getInt("preHunt.countdown", 5); }
    @Override public double getHunterCircleRadius() { return getDouble("preHunt.hunterCircleRadius", 3.0); }
    @Override public int getFormationSearchRadius() { return getInt("preHunt.formationSearchRadius", 20); }
    @Override public boolean isTrackingEnabled() { return getBool("tracking.enabled", true); }
    @Override public int getTrackingUpdateTicks() { return getInt("tracking.updateTicks", 5); }
    @Override public boolean isTrackingShowDistance() { return getBool("tracking.showDistance", true); }
    @Override public int getRunnerLives() { return getInt("runner.lives", 1); }
    @Override public boolean isHunterInfiniteRespawns() { return getBool("hunters.infiniteRespawns", true); }
    @Override public int getHunterRespawnLimit() { return getInt("hunters.respawnLimit", -1); }
    @Override public boolean isScoreboardEnabled() { return getBool("scoreboard.enabled", true); }
    @Override public boolean isActionBarEnabled() { return getBool("actionBar.enabled", true); }
    @Override public boolean isHunterKeepInventory() { return getBool("hunters.keepInventory", false); }
    @Override public boolean isHunterKeepArmor() { return getBool("hunters.keepArmor", false); }

    @Override
    public List<PlayerEffect> getRunnerPotionEffects() { return getEffects("potionEffects.runner"); }

    @Override
    public List<PlayerEffect> getHunterPotionEffects() { return getEffects("potionEffects.hunters"); }

    private List<PlayerEffect> getEffects(String path) {
        List<PlayerEffect> effects = new ArrayList<>();
        String[] parts = path.split("\\.");
        JsonObject obj = config;
        for (String part : parts) {
            if (obj == null || !obj.has(part)) return effects;
            obj = obj.getAsJsonObject(part);
        }
        return effects;
    }
}
