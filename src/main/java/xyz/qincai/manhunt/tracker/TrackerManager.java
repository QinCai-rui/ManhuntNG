package xyz.qincai.manhunt.tracker;

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

    // Stores runner's last-known location per dimension (OVERWORLD, NETHER, END)
    private final Map<World.Environment, Location> runnerLastKnownLocations = new HashMap<>();

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
     * - Updates runner last-known location.
     * - Updates each hunter's compass.
     * - Runs only during RUNNING state.
     */
    public void startTracking() {
        if (!plugin.getConfigManager().isTrackingEnabled()) return;

        stopTracking(); // Ensure no duplicate tasks

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Match match = plugin.getGameManager().getMatch();

            // Only track during RUNNING
            if (match.getState() != GameState.RUNNING) return;
            if (match.getRunnerUuid() == null) return;

            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner == null) return;

            // Update last-known location for runner's current dimension
            updateRunnerLastKnown(runner);

            // Update each hunter's compass
            for (UUID hunterUuid : match.getHunterUuids()) {
                Player hunter = Bukkit.getPlayer(hunterUuid);
                if (hunter == null) continue;
                if (hunter.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

                updateCompass(hunter, runner);
            }
        }, 0L, plugin.getConfigManager().getTrackingUpdateTicks()).getTaskId();
    }

    /*
     * Stops tracking and clears last-known locations.
     */
    public void stopTracking() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        runnerLastKnownLocations.clear();
    }

    /*
     * Stores runner's last-known location for their current dimension.
     */
    public void updateRunnerLastKnown(Player runner) {
        runnerLastKnownLocations.put(
                runner.getWorld().getEnvironment(),
                runner.getLocation().clone()
        );
    }

    /*
     * Returns last-known runner location for a given dimension.
     */
    public Location getLastRunnerLocation(World.Environment environment) {
        Location loc = runnerLastKnownLocations.get(environment);
        return loc == null ? null : loc.clone();
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
            // Different dimension -> use last-known location
            World.Environment hunterEnv = hunterWorld.getEnvironment();
            Location lastKnown = runnerLastKnownLocations.get(hunterEnv);

            // No last-known location -> warn and disable compass
            if (lastKnown == null || lastKnown.getWorld() == null) {
                String envName = hunterEnv.toString();

                // Console warning
                plugin.getLogger().warning("[Tracker] No last-known runner location for "
                        + envName + ". Compass for " + hunter.getName() + " will not update.");

                // Send message to hunter
                hunter.sendMessage("§cTracking unavailable: Runner has not entered the "
                        + envName + " yet. Check server console for more info.");

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
