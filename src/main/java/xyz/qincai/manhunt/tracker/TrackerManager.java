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
    private final Map<UUID, Location> runnerLastKnownLocations = new HashMap<>();

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

        if (hunterWorld.equals(runnerWorld)) {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setLodestone(runner.getLocation());
            meta.setLodestoneTracked(false);
            compass.setItemMeta(meta);
        } else {
            World.Environment hunterEnv = hunterWorld.getEnvironment();
            Location lastKnown = runnerLastKnownLocations.get(hunterEnv);
            if (lastKnown != null && lastKnown.getWorld() != null) {
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setLodestone(lastKnown);
                meta.setLodestoneTracked(false);
                compass.setItemMeta(meta);
            } else {
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setLodestone(hunterWorld.getSpawnLocation());
                meta.setLodestoneTracked(false);
                compass.setItemMeta(meta);
            }
        }
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

            ItemStack compass = createTrackerCompass();
            hunter.getInventory().addItem(compass);
        }
    }

    public void giveCompassToPlayer(Player player) {
        ItemStack compass = createTrackerCompass();
        player.getInventory().addItem(compass);
    }
}
