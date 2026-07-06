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
import xyz.qincai.manhunt.game.Match;

import java.util.UUID;

public class TrackerManager {
    private final ManhuntNG plugin;
    private NamespacedKey trackerKey;
    private long taskId = -1;

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
            if (match.getState() != xyz.qincai.manhunt.game.GameState.RUNNING) return;
            if (match.getRunnerUuid() == null) return;

            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner == null) return;

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
    }

    private void updateCompass(Player hunter, Player runner) {
        ItemStack compass = findCompass(hunter);
        if (compass == null) {
            compass = createTrackerCompass();
            hunter.getInventory().addItem(compass);
            return;
        }

        World runnerWorld = runner.getWorld();
        if (hunter.getWorld().equals(runnerWorld)) {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setLodestone(runner.getLocation());
            meta.setLodestoneTracked(false);
            compass.setItemMeta(meta);
        } else if (isLinkedDimension(hunter.getWorld(), runnerWorld)) {
            Location linked = getLinkedLocation(hunter.getLocation(), runner.getLocation(), runnerWorld);
            if (linked != null) {
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setLodestone(linked);
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

    private boolean isLinkedDimension(World world1, World world2) {
        return (world1.getEnvironment() == World.Environment.NORMAL && world2.getEnvironment() == World.Environment.NETHER)
                || (world1.getEnvironment() == World.Environment.NETHER && world2.getEnvironment() == World.Environment.NORMAL);
    }

    private Location getLinkedLocation(Location from, Location to, World targetWorld) {
        double scale;
        if (from.getWorld().getEnvironment() == World.Environment.NETHER) {
            scale = 8.0;
        } else {
            scale = 0.125;
        }
        return new Location(targetWorld, to.getX() / scale, to.getY(), to.getZ() / scale);
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
}
