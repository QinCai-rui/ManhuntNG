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

public class TrackerManager {
    private final ManhuntNG plugin;
    private NamespacedKey trackerKey;
    private int taskId = -1;
    private final Map<World.Environment, Location> runnerLastKnownLocations = new HashMap<>();

    public TrackerManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void init() {
        trackerKey = new NamespacedKey(plugin, "tracking_compass");
    }

    public void startTracking() {
        if (!plugin.getConfigManager().isTrackingEnabled()) return;
        stopTracking();

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Match match = plugin.getGameManager().getMatch();
            if (match.getState() != GameState.RUNNING) return;
            if (match.getRunnerUuid() == null) return;

            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner == null) return;

            updateRunnerLastKnown(runner);

            for (UUID hunterUuid : match.getHunterUuids()) {
                Player hunter = Bukkit.getPlayer(hunterUuid);
                if (hunter == null) continue;
                if (hunter.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

                updateCompass(hunter, runner);
            }
        }, 0L, plugin.getConfigManager().getTrackingUpdateTicks()).getTaskId();
    }

    public void stopTracking() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        runnerLastKnownLocations.clear();
    }

    public void updateRunnerLastKnown(Player runner) {
        runnerLastKnownLocations.put(runner.getWorld().getEnvironment(), runner.getLocation().clone());
    }

    private void updateCompass(Player hunter, Player runner) {
        ItemStack compass = findCompass(hunter);
        if (compass == null) {
            compass = createTrackerCompass();
            hunter.getInventory().addItem(compass);
            return;
        }

        World runnerWorld = runner.getWorld();
        World hunterWorld = hunter.getWorld();

        Location target;
        if (hunterWorld.equals(runnerWorld)) {
            target = runner.getLocation();
        } else if (runnerWorld.getEnvironment() == World.Environment.NETHER && hunterWorld.getEnvironment() == World.Environment.NORMAL) {
            Location r = runner.getLocation();
            target = new Location(hunterWorld, r.getX() * 8, r.getY(), r.getZ() * 8);
        } else if (runnerWorld.getEnvironment() == World.Environment.NORMAL && hunterWorld.getEnvironment() == World.Environment.NETHER) {
            Location r = runner.getLocation();
            target = new Location(hunterWorld, r.getX() / 8, r.getY(), r.getZ() / 8);
        } else {
            Location lastKnown = runnerLastKnownLocations.get(hunterWorld.getEnvironment());
            target = (lastKnown != null && lastKnown.getWorld() != null) ? lastKnown : hunterWorld.getSpawnLocation();
        }

        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.setLodestone(target);
        meta.setLodestoneTracked(false);
        compass.setItemMeta(meta);
    }

    private ItemStack createTrackerCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        return compass;
    }

    private ItemStack findCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE)) {
                    return item;
                }
            }
        }
        return null;
    }

    public boolean isTrackerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE);
    }

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

    public void giveCompassToPlayer(Player player) {
        if (findCompass(player) == null) {
            player.getInventory().addItem(createTrackerCompass());
        }
    }
}
