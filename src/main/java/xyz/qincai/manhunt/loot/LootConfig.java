package xyz.qincai.manhunt.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class LootConfig {
    private boolean enabled = true;
    private int configVersion = 1;
    private final Map<String, MobDropSource> mobDrops = new HashMap<>();
    private final Map<String, BarteringSource> piglinBartering = new HashMap<>();
    private final Map<String, ChestLootSource> chestLoot = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public Map<String, MobDropSource> getMobDrops() {
        return Collections.unmodifiableMap(mobDrops);
    }

    public Map<String, BarteringSource> getPiglinBartering() {
        return Collections.unmodifiableMap(piglinBartering);
    }

    public Map<String, ChestLootSource> getChestLoot() {
        return Collections.unmodifiableMap(chestLoot);
    }

    /**
     * Loads loot config from a file on disk, falling back to the bundled resource.
     */
    public static LootConfig load(File dataFolder, InputStream defaultResource) {
        LootConfig config = new LootConfig();
        File file = new File(dataFolder, "loot.json");

        try (InputStream resource = defaultResource) {
            if (file.exists()) {
                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    config.parse(JsonParser.parseReader(reader).getAsJsonObject());
                }
            } else if (resource != null) {
                try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                    config.parse(JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("ManhuntNG").log(Level.SEVERE,
                    "Failed to load loot.json, using empty config", e);
            config = new LootConfig();
        }

        return config;
    }

    private void parse(JsonObject root) {
        configVersion = getInt(root, "config-version", 1);
        enabled = getBool(root, "enabled", true);

        if (!root.has("sources")) return;
        JsonObject sources = root.getAsJsonObject("sources");

        // Mob drops
        if (sources.has("mob-drops")) {
            JsonObject mobSection = sources.getAsJsonObject("mob-drops");
            boolean mobEnabled = getBool(mobSection, "enabled", true);
            if (mobEnabled) {
                for (Map.Entry<String, JsonElement> entry : mobSection.entrySet()) {
                    if ("enabled".equals(entry.getKey())) continue;
                    if (entry.getValue().isJsonObject()) {
                        MobDropSource source = parseMobDropSource(entry.getValue().getAsJsonObject());
                        if (source != null) {
                            mobDrops.put(entry.getKey().toLowerCase(), source);
                        }
                    }
                }
            }
        }

        // Piglin bartering
        if (sources.has("piglin-bartering")) {
            JsonObject barterSection = sources.getAsJsonObject("piglin-bartering");
            boolean barterEnabled = getBool(barterSection, "enabled", true);
            if (barterEnabled) {
                for (Map.Entry<String, JsonElement> entry : barterSection.entrySet()) {
                    if ("enabled".equals(entry.getKey())) continue;
                    if (entry.getValue().isJsonObject()) {
                        BarteringSource source = parseBarteringSource(entry.getValue().getAsJsonObject());
                        if (source != null) {
                            piglinBartering.put(entry.getKey().toLowerCase(), source);
                        }
                    }
                }
            }
        }

        // Chest loot
        if (sources.has("chest-loot")) {
            JsonObject chestSection = sources.getAsJsonObject("chest-loot");
            boolean chestEnabled = getBool(chestSection, "enabled", true);
            if (chestEnabled) {
                for (Map.Entry<String, JsonElement> entry : chestSection.entrySet()) {
                    if ("enabled".equals(entry.getKey())) continue;
                    if (entry.getValue().isJsonObject()) {
                        ChestLootSource source = parseChestLootSource(entry.getValue().getAsJsonObject());
                        if (source != null) {
                            chestLoot.put(entry.getKey().toLowerCase(), source);
                        }
                    }
                }
            }
        }
    }

    private MobDropSource parseMobDropSource(JsonObject obj) {
        String role = getString(obj, "role", "all");
        List<LootDrop> drops = parseLootDrops(obj, "drops");
        if (drops.isEmpty()) return null;
        return new MobDropSource(role, drops);
    }

    private BarteringSource parseBarteringSource(JsonObject obj) {
        String role = getString(obj, "role", "all");
        List<WeightedItem> items = new ArrayList<>();

        if (obj.has("items") && obj.get("items").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("items");
            for (JsonElement elem : arr) {
                if (!elem.isJsonObject()) continue;
                JsonObject item = elem.getAsJsonObject();
                String material = getString(item, "material", "");
                if (material.isEmpty()) continue;

                Material mat = parseMaterial(material);
                if (mat == null) continue;

                int minAmount = getInt(item, "min-amount", 1);
                int maxAmount = getInt(item, "max-amount", 1);
                double weight = getDouble(item, "weight", 1.0);
                PotionType potionType = parsePotionType(getString(item, "potion-type", null));
                Material potionForm = parsePotionForm(getString(item, "potion-form", null), mat);
                List<EnchantmentEntry> enchantments = parseEnchantments(item);

                // Validate: weight must be positive
                if (weight <= 0) continue;

                // Validate: min-amount must not exceed max-amount
                if (minAmount > maxAmount) {
                    int temp = minAmount;
                    minAmount = maxAmount;
                    maxAmount = temp;
                }

                items.add(new WeightedItem(mat, minAmount, maxAmount, weight, potionType, potionForm, enchantments));
            }
        }

        if (items.isEmpty()) return null;
        return new BarteringSource(role, items);
    }

    private ChestLootSource parseChestLootSource(JsonObject obj) {
        String role = getString(obj, "role", "all");
        List<LootDrop> drops = parseLootDrops(obj, "drops");
        if (drops.isEmpty()) return null;
        return new ChestLootSource(role, drops);
    }

    private List<LootDrop> parseLootDrops(JsonObject obj, String key) {
        List<LootDrop> drops = new ArrayList<>();
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return drops;

        JsonArray arr = obj.getAsJsonArray(key);
        for (JsonElement elem : arr) {
            if (!elem.isJsonObject()) continue;
            JsonObject item = elem.getAsJsonObject();
            String material = getString(item, "material", "");
            if (material.isEmpty()) continue;

            Material mat = parseMaterial(material);
            if (mat == null) continue;

            int minAmount = getInt(item, "min-amount", 1);
            int maxAmount = getInt(item, "max-amount", 1);
            double dropChance = getDouble(item, "drop-chance", 1.0);
            String displayName = getString(item, "display-name", null);
            PotionType potionType = parsePotionType(getString(item, "potion-type", null));
            Material potionForm = parsePotionForm(getString(item, "potion-form", null), mat);
            List<EnchantmentEntry> enchantments = parseEnchantments(item);

            // Validate: min-amount must not exceed max-amount
            if (minAmount > maxAmount) {
                int temp = minAmount;
                minAmount = maxAmount;
                maxAmount = temp;
            }

            drops.add(new LootDrop(mat, minAmount, maxAmount, dropChance, displayName, potionType, potionForm, enchantments));
        }
        return drops;
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private PotionType parsePotionType(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return PotionType.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Material parsePotionForm(String name, Material originalMaterial) {
        if (name == null || name.isEmpty()) {
            return originalMaterial;
        }
        try {
            return Material.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return originalMaterial;
        }
    }

    private List<EnchantmentEntry> parseEnchantments(JsonObject obj) {
        List<EnchantmentEntry> enchants = new ArrayList<>();
        if (!obj.has("enchantments") || !obj.get("enchantments").isJsonArray()) return enchants;

        JsonArray arr = obj.getAsJsonArray("enchantments");
        for (JsonElement elem : arr) {
            if (!elem.isJsonObject()) continue;
            JsonObject enchantObj = elem.getAsJsonObject();
            String name = getString(enchantObj, "enchantment", "");
            if (name.isEmpty()) continue;

            try {
                Enchantment ench = Enchantment.getByName(name.toUpperCase().replace(" ", "_"));
                if (ench == null) continue;
                int minLevel = getInt(enchantObj, "min-level", getInt(enchantObj, "level", 1));
                int maxLevel = getInt(enchantObj, "max-level", minLevel);
                enchants.add(new EnchantmentEntry(ench, minLevel, maxLevel));
            } catch (Exception e) {
                // skip invalid enchantment
            }
        }
        return enchants;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        if (!obj.has(key)) return def;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        if (!obj.has(key)) return def;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean getBool(JsonObject obj, String key, boolean def) {
        if (!obj.has(key)) return def;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return def;
        }
    }

    private static String getString(JsonObject obj, String key, String def) {
        if (!obj.has(key)) return def;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return def;
        }
    }

    // ---- Data classes ----

    public record LootDrop(Material material, int minAmount, int maxAmount, double dropChance, String displayName, PotionType potionType, Material potionForm, List<EnchantmentEntry> enchantments) {}

    public record WeightedItem(Material material, int minAmount, int maxAmount, double weight, PotionType potionType, Material potionForm, List<EnchantmentEntry> enchantments) {}

    public record EnchantmentEntry(Enchantment enchantment, int minLevel, int maxLevel) {}

    public record MobDropSource(String role, List<LootDrop> drops) {}

    public record BarteringSource(String role, List<WeightedItem> items) {}

    public record ChestLootSource(String role, List<LootDrop> drops) {}
}
