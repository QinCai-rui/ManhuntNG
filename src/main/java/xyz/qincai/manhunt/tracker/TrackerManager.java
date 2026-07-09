package xyz.qincai.manhunt.tracker;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

/*
 * Handles the tracking compass system used by hunters.
 *
 * what it does:
 *  - Gives hunters a special compass marked with a PDC key.
 *  - Compass always points to runner's current location (same dimension).
 *  - If hunter is in a different dimension, compass points to runner's last-known location in that dimension (stored separately per dimension).
 *  - Updates every n ticks based on config.
 *  - Removes tracking compasses on death.
 */
public class TrackerManager {
    private final ManhuntNG plugin;

    // PersistentDataContainer key used to mark tracking compasses
    private NamespacedKey trackerKey;

    // Task ID for repeating tracking update
    private int taskId = -1;

    // Stores each runner's last-known location per dimension (OVERWORLD, NETHER, END)
    // Map<RunnerUUID, Map<Environment, Location>>
    private final Map<UUID, Map<World.Environment, Location>> allRunnerLastKnownLocations = new HashMap<>();

    public TrackerManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    /*
     * init the PDC key for identifying tracking compasses.
     * Called during plugin startup.
     */
    public void init() {
        trackerKey = new NamespacedKey(plugin, "tracking_compass");
    }

    /*
     * Starts the repeating tracking task.
     * - Updates each runner's last-known location.
     * - For each hunter, finds the nearest runner and updates compass.
     * - Runs only during RUNNING state.
     */
    public void startTracking() {
        if (!plugin.getConfigManager().isTrackingEnabled()) return;

        stopTracking(); // Ensure no duplicate tasks

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Match match = plugin.getGameManager().getMatch();

            // Only track during RUNNING
            if (match.getState() != GameState.RUNNING) return;
            if (match.getRunnerUuids().isEmpty()) return;

            // Update last-known location for all runners
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner == null) continue;
                updateRunnerLastKnown(runner);
            }

            // For each hunter, find nearest runner and update compass
            for (UUID hunterUuid : match.getHunterUuids()) {
                Player hunter = Bukkit.getPlayer(hunterUuid);
                if (hunter == null) continue;
                if (hunter.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

                Player nearestRunner = findNearestRunner(hunter);
                if (nearestRunner != null) {
                    updateCompass(hunter, nearestRunner);
                }
            }
        }, 0L, plugin.getConfigManager().getTrackingUpdateTicks()).getTaskId();
    }

    /*
     * Finds the nearest alive runner for a given player.
     */
    public Player findNearestRunner(Player hunter) {
        Match match = plugin.getGameManager().getMatch();
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner == null) continue;
            if (runner.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

            double distance = hunter.getWorld().equals(runner.getWorld())
                    ? hunter.getLocation().distanceSquared(runner.getLocation())
                    : Double.MAX_VALUE;

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = runner;
            }
        }

        // If no runner is in the same dimension, just return the first runner
        if (nearest == null) {
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null && runner.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    nearest = runner;
                    break;
                }
            }
        }

        return nearest;
    }

    /*
     * Stops tracking and clears last-known locations.
     */
    public void stopTracking() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        allRunnerLastKnownLocations.clear();
    }

    /*
     * Stores a runner's last-known location for their current dimension.
     */
    public void updateRunnerLastKnown(Player runner) {
        allRunnerLastKnownLocations
                .computeIfAbsent(runner.getUniqueId(), k -> new HashMap<>())
                .put(runner.getWorld().getEnvironment(), runner.getLocation().clone());
    }

    /*
     * Returns last-known location for a specific runner in a given dimension.
     */
    public Location getRunnerLastKnownLocation(UUID runnerUuid, World.Environment environment) {
        Map<World.Environment, Location> runnerLocs = allRunnerLastKnownLocations.get(runnerUuid);
        if (runnerLocs == null) return null;
        Location loc = runnerLocs.get(environment);
        return loc == null ? null : loc.clone();
    }

    /*
     * Returns the last-known location for any runner in a given dimension.
     * Used by the debug command to show last-known runner positions per environment.
     */
    public Location getLastRunnerLocation(World.Environment environment) {
        for (Map<World.Environment, Location> runnerLocs : allRunnerLastKnownLocations.values()) {
            Location loc = runnerLocs.get(environment);
            if (loc != null) return loc.clone();
        }
        return null;
    }

    /**
     * Updates a hunter's tracking compass to point to the correct location.
     * - Same dimension -> direct runner location.
     * - Different dimension -> last-known location in hunter's dimension.
     * - If no last-known location exists -> warn hunter + console and disable compass.
     */
    private void updateCompass(Player hunter, Player runner) {
        ItemStack compass = findCompass(hunter);

        // If hunter doesn't have a tracking compass, give them one
        if (compass == null) {
            compass = createTrackerCompass();
            hunter.getInventory().addItem(compass);
            return;
        }

        World runnerWorld = runner.getWorld();
        World hunterWorld = hunter.getWorld();

        Location target;

        // Same dimension -> track runner directly
        if (hunterWorld.equals(runnerWorld)) {
            target = runner.getLocation();
        } else {
            // Different dimension -> use last-known location for this runner
            World.Environment hunterEnv = hunterWorld.getEnvironment();
            Location lastKnown = getRunnerLastKnownLocation(runner.getUniqueId(), hunterEnv);

            // No last-known location -> warn and disable compass
            if (lastKnown == null || lastKnown.getWorld() == null) {
                String envName = hunterEnv.toString();

                // Console warning
                plugin.getLogger().warning("[Tracker] No last-known location for runner "
                        + runner.getName() + " in " + envName
                        + ". Compass for " + hunter.getName() + " will not update.");

                // Send message to hunter
                hunter.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tracking unavailable: " + runner.getName()
                        + " has not entered the " + envName + " yet."));

                // Disable compass target
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setLodestone(null);           // Remove target entirely
                meta.setLodestoneTracked(false);   // Required for custom tracking
                compass.setItemMeta(meta);

                return;
            }

            target = lastKnown;
        }

        // Update compass normally
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.setLodestone(target);
        meta.setLodestoneTracked(false);
        compass.setItemMeta(meta);
    }


    /*
     * Creates a tracking compass and marks it with a PDC key.
     */
    private ItemStack createTrackerCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();

        // Mark compass as a tracking compass
        meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);

        compass.setItemMeta(meta);
        return compass;
    }

    /*
     * Finds a tracking compass in a player's inventory.
     * Returns null if none found.
     */
    private ItemStack findCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                if (item.hasItemMeta() &&
                    item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE)) {
                    return item;
                }
            }
        }
        return null;
    }

    /*
     * Checks if an ItemStack is a tracking compass.
     */
    public boolean isTrackerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!item.hasItemMeta()) return false;

        return item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE);
    }

    /*
     * Gives tracking compasses to all hunters who don't already have one.
     */
    public void giveCompassToAll() {
        Match match = plugin.getGameManager().getMatch();

        for (UUID uuid : match.getHunterUuids()) {
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter == null) continue;

            if (findCompass(hunter) == null) {
                hunter.getInventory().addItem(createTrackerCompass());
            }
        }
    }

    /*
     * Gives a tracking compass to a single hunter if they don't already have one.
     */
    public void giveCompassToPlayer(Player player) {
        if (findCompass(player) == null) {
            player.getInventory().addItem(createTrackerCompass());
        }
    }
}
