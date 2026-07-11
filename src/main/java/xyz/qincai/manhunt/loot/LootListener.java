package xyz.qincai.manhunt.loot;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootListener implements Listener {
    private final ManhuntNG plugin;

    // Track last chest interaction per player for LootGenerateEvent correlation
    private final Map<BlockKey, Player> lastChestInteract = new HashMap<>();

    public LootListener(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    /**
     * World-aware block key for tracking chest interactions across different worlds.
     */
    private record BlockKey(java.util.UUID worldUid, int x, int y, int z) {}

    /**
     * Tracks which player last interacted with a container block.
     * Used to identify the opener in LootGenerateEvent.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Container)) return;

        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();
        if (!match.isParticipant(player.getUniqueId())) return;

        BlockKey key = blockKey(block.getLocation());
        lastChestInteract.put(key, player);
    }

    /**
     * Clean up tracking when a container block is broken.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Container)) return;
        lastChestInteract.remove(blockKey(block.getLocation()));
    }

    // ---- Mob drops ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getLootManager().isEnabled()) return;

        Match match = plugin.getGameManager().getMatch();
        if (!isActive(match)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!match.isParticipant(killer.getUniqueId())) return;

        PlayerRole role = plugin.getPlayerManager().getRole(killer.getUniqueId());
        EntityType entityType = event.getEntityType();

        LootConfig.MobDropSource source = plugin.getLootManager().getMobDrops(entityType);
        if (source == null) return;
        if (!plugin.getLootManager().shouldApplyRole(source.role(), role)) return;

        List<ItemStack> extraDrops = new ArrayList<>();
        for (LootConfig.LootDrop drop : source.drops()) {
            ItemStack item = plugin.getLootManager().createItem(drop);
            if (item != null) {
                extraDrops.add(item);
            }
        }

        event.getDrops().addAll(extraDrops);
    }

    // ---- Piglin bartering ----

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {
        if (!plugin.getLootManager().isEnabled()) return;

        Match match = plugin.getGameManager().getMatch();
        if (!isActive(match)) return;

        // Find the nearest participant to the piglin (bartering range ~16 blocks)
        Piglin piglin = event.getEntity();
        Location piglinLoc = piglin.getLocation();
        Player barteringPlayer = findNearestParticipant(piglinLoc, match, 16.0);
        if (barteringPlayer == null) return;

        PlayerRole role = plugin.getPlayerManager().getRole(barteringPlayer.getUniqueId());

        // Check all bartering sources (usually just "all")
        for (LootConfig.BarteringSource source : plugin.getLootManager().getAllBarteringSources().values()) {
            if (!plugin.getLootManager().shouldApplyRole(source.role(), role)) continue;

            List<ItemStack> items = plugin.getLootManager().pickBarteringItems(source);
            if (!items.isEmpty()) {
                event.getOutcome().clear();
                event.getOutcome().addAll(items);
                return;
            }
        }
    }

    // ---- Chest loot ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getLootManager().isEnabled()) return;

        Match match = plugin.getGameManager().getMatch();
        if (!isActive(match)) return;

        // Try to find the player who triggered this loot
        Player opener = null;

        // Method 1: Check entity context
        Entity entity = event.getEntity();
        if (entity instanceof Player p) {
            opener = p;
        }

        // Method 2: Check inventory holder
        if (opener == null && event.getInventoryHolder() != null) {
            if (event.getInventoryHolder() instanceof Container container) {
                BlockKey key = blockKey(container.getBlock().getLocation());
                opener = lastChestInteract.get(key);
            }
        }

        // Method 3: Find nearest participant to the loot source
        if (opener == null) {
            Location loc = null;
            if (event.getInventoryHolder() instanceof Container container) {
                loc = container.getBlock().getLocation();
            } else if (entity != null) {
                loc = entity.getLocation();
            }
            if (loc != null) {
                opener = findNearestParticipant(loc, match, 6.0);
            }
        }

        if (opener == null) return;
        if (!match.isParticipant(opener.getUniqueId())) return;

        PlayerRole role = plugin.getPlayerManager().getRole(opener.getUniqueId());

        // Get loot table key
        String lootTableKey = event.getLootTable().getKey().toString();
        LootConfig.ChestLootSource source = plugin.getLootManager().getChestLoot(lootTableKey);
        if (source == null) return;
        if (!plugin.getLootManager().shouldApplyRole(source.role(), role)) return;

        List<ItemStack> extraLoot = new ArrayList<>();
        for (LootConfig.LootDrop drop : source.drops()) {
            ItemStack item = plugin.getLootManager().createItem(drop);
            if (item != null) {
                extraLoot.add(item);
            }
        }

        event.getLoot().addAll(extraLoot);
    }

    // ---- Cleanup on game end ----

    public void clearTracking() {
        lastChestInteract.clear();
    }

    // ---- Helpers ----

    private boolean isActive(Match match) {
        return match.getState() == GameState.RUNNING ||
               match.getState() == GameState.PRE_HUNT ||
               match.getState() == GameState.HEADSTART ||
               match.getState() == GameState.PAUSED;
    }

    private Player findNearestParticipant(Location center, Match match, double maxDistance) {
        Player nearest = null;
        double nearestDistSq = maxDistance * maxDistance;

        for (var uuid : match.getRunnerUuids()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline() && p.getWorld().equals(center.getWorld())) {
                double distSq = p.getLocation().distanceSquared(center);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = p;
                }
            }
        }
        for (var uuid : match.getHunterUuids()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline() && p.getWorld().equals(center.getWorld())) {
                double distSq = p.getLocation().distanceSquared(center);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    private BlockKey blockKey(Location loc) {
        return new BlockKey(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
