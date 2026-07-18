package xyz.qincai.manhunt.loot;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.player.PlayerRole;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {
    private final ManhuntNG plugin;
    private LootConfig config;

    public LootManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        InputStream stream = plugin.getResource("loot.json");
        config = LootConfig.load(plugin.getDataFolder(), stream);
        plugin.getLogger().info("[Loot] Loaded loot.json (v" + config.getConfigVersion()
                + ", enabled=" + config.isEnabled() + ")");
    }

    public void reloadConfig() {
        loadConfig();
    }

    public boolean isEnabled() {
        return config != null && config.isEnabled();
    }

    /**
     * Returns configured mob drops for an entity type, or null if none.
     */
    public LootConfig.MobDropSource getMobDrops(org.bukkit.entity.EntityType entityType) {
        if (!isEnabled()) return null;
        String key = entityType.name().toLowerCase();
        return config.getMobDrops().get(key);
    }

    /**
     * Returns all configured piglin bartering sources (usually just "all").
     */
    public LootConfig.BarteringSource getBarteringSource(String key) {
        if (!isEnabled()) return null;
        return config.getPiglinBartering().get(key.toLowerCase());
    }

    /**
     * Returns all piglin bartering sources.
     */
    public java.util.Map<String, LootConfig.BarteringSource> getAllBarteringSources() {
        if (!isEnabled()) return java.util.Collections.emptyMap();
        return config.getPiglinBartering();
    }

    /**
     * Returns configured chest loot for a loot table key, or null if none.
     */
    public LootConfig.ChestLootSource getChestLoot(String lootTableKey) {
        if (!isEnabled()) return null;
        // Strip "minecraft:" prefix for matching
        String normalized = lootTableKey.toLowerCase();
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        // Try direct match, then suffix match (e.g., "chests/bastion_treasure" -> "bastion_treasure")
        LootConfig.ChestLootSource source = config.getChestLoot().get(normalized);
        if (source != null) return source;

        // Try matching the last segment after "/"
        int slashIdx = normalized.lastIndexOf('/');
        if (slashIdx >= 0) {
            String suffix = normalized.substring(slashIdx + 1);
            source = config.getChestLoot().get(suffix);
        }
        return source;
    }

    /**
     * Checks if a loot entry's role filter matches the given player role.
     */
    public boolean shouldApplyRole(String configuredRole, PlayerRole playerRole) {
        if ("all".equalsIgnoreCase(configuredRole)) return true;
        return switch (playerRole) {
            case RUNNER -> "runner".equalsIgnoreCase(configuredRole);
            case HUNTER -> "hunter".equalsIgnoreCase(configuredRole);
            case SPECTATOR -> false;
        };
    }

    /**
     * Creates an ItemStack from a LootDrop config entry with a random amount
     * between min and max. Returns null if the roll fails (based on drop chance).
     */
    public ItemStack createItem(LootConfig.LootDrop drop) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (drop.dropChance() < 1.0 && rng.nextDouble() >= drop.dropChance()) {
            return null;
        }

        int amount = rng.nextInt(drop.minAmount(), drop.maxAmount() + 1);
        if (amount <= 0) return null;

        Material material = drop.potionType() != null && drop.potionForm() != null
                ? drop.potionForm() : drop.material();
        ItemStack item = new ItemStack(material, amount);
        applyPotionMeta(item, drop.potionType());
        applyEnchantments(item, drop.enchantments());

        if (drop.displayName() != null && !drop.displayName().isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(drop.displayName()));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    /**
     * Creates a random ItemStack from a WeightedItem config entry.
     */
    public ItemStack createItem(LootConfig.WeightedItem item) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int amount = rng.nextInt(item.minAmount(), item.maxAmount() + 1);
        if (amount <= 0) return null;

        Material material = item.potionType() != null && item.potionForm() != null
                ? item.potionForm() : item.material();
        ItemStack stack = new ItemStack(material, amount);
        applyPotionMeta(stack, item.potionType());
        applyEnchantments(stack, item.enchantments());
        return stack;
    }

    /**
     * Applies PotionMeta to an ItemStack if a potion type is specified.
     */
    private void applyPotionMeta(ItemStack item, PotionType potionType) {
        if (potionType == null) return;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;
        meta.setBasePotionType(potionType);
        item.setItemMeta(meta);
    }

    /**
     * Applies enchantments to an ItemStack from a list of EnchantmentEntry.
     */
    private void applyEnchantments(ItemStack item, List<LootConfig.EnchantmentEntry> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (LootConfig.EnchantmentEntry entry : enchantments) {
            int level = rng.nextInt(entry.minLevel(), entry.maxLevel() + 1);
            item.addUnsafeEnchantment(entry.enchantment(), level);
        }
    }

    /**
     * Picks a random item from a weighted bartering source list.
     * Returns a list (piglin bartering always drops one item type per barter).
     */
    public List<ItemStack> pickBarteringItems(LootConfig.BarteringSource source) {
        List<LootConfig.WeightedItem> items = source.items();
        if (items.isEmpty()) return Collections.emptyList();

        double totalWeight = 0;
        for (LootConfig.WeightedItem item : items) {
            totalWeight += item.weight();
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0;
        for (LootConfig.WeightedItem item : items) {
            cumulative += item.weight();
            if (roll < cumulative) {
                ItemStack stack = createItem(item);
                if (stack != null) {
                    return List.of(stack);
                }
                break;
            }
        }
        return Collections.emptyList();
    }
}
