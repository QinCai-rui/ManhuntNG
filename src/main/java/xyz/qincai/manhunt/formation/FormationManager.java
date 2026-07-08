package xyz.qincai.manhunt.formation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.util.List;
import java.util.UUID;

public class FormationManager {
    private final ManhuntNG plugin;

    public FormationManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void teleportToFormation() {
        Match match = plugin.getGameManager().getMatch();
        World gameWorld = match.getGameWorld();
        if (gameWorld == null) return; // No world loaded -> cannot form anything
        if (match.getRunnerUuids().isEmpty()) return; // No runners selected yet

        double radius = plugin.getConfigManager().getHunterCircleRadius(); // Circle radius for hunters
        int searchRadius = plugin.getConfigManager().getFormationSearchRadius(); // How far we search for safe formation center
        List<UUID> hunterUuids = List.copyOf(match.getHunterUuids()); // Copy to avoid modification at a time
        int hunterCount = hunterUuids.size();

        Location spawnLoc = gameWorld.getSpawnLocation();
        Location center = findSafeSurfaceLocation(spawnLoc); // Try vanilla spawn first

        // Check if vanilla spawn is safe AND hunter circle is safe
        boolean formationFound = center != null
                && (hunterCount == 0 || areHunterPositionsSafe(center, radius, hunterCount, gameWorld));

        // If vanilla spawn isn't safe, search outward in a square ring pattern
        if (!formationFound) {
            int spawnX = spawnLoc.getBlockX();
            int spawnZ = spawnLoc.getBlockZ();

            outer:
            for (int range = 1; range <= searchRadius; range++) {
                // iterate perimeter of the square at distance = range
                for (int dx = -range; dx <= range; dx++) {
                    for (int dz = -range; dz <= range; dz++) {
                        // Only check perimeter, skip inner blocks
                        if (Math.abs(dx) != range && Math.abs(dz) != range) continue;

                        Location candidate = new Location(gameWorld, spawnX + dx + 0.5, 0, spawnZ + dz + 0.5);
                        Location safe = findSafeSurfaceLocation(candidate);
                        if (safe == null) continue; // No safe ground here
                        if (!isSafeForPlayer(safe)) continue; // Player cannot stand here

                        // Ensure hunters can also stand safely in a circle around this point
                        if (hunterCount > 0 && !areHunterPositionsSafe(safe, radius, hunterCount, gameWorld))
                            continue;

                        center = safe;
                        formationFound = true;
                        break outer; // Stop searching once a valid formation is found
                    }
                }
            }
        }

        // If still no safe center found, fall back to guaranteed safe location
        if (center == null || !isSafeForPlayer(center)) {
            center = findSafeLocationGuaranteed(gameWorld, spawnLoc);
        }

        // Teleport all runners to the center
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = org.bukkit.Bukkit.getPlayer(runnerUuid);
            if (runner != null) {
                runner.teleport(center.clone().add(0, 0.5, 0));
            }
        }

        if (hunterCount == 0) return; // No hunters -> formation ends here

        // Compute hunter positions in a CIRCLE around the center
        double y = center.getY();
        double angleStep = 2 * Math.PI / hunterCount;

        for (int i = 0; i < hunterCount; i++) {
            UUID hunterUuid = hunterUuids.get(i);
            Player hunter = org.bukkit.Bukkit.getPlayer(hunterUuid);
            if (hunter == null) continue; // Skip offline hunters

            double angle = angleStep * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location hunterLoc;

            if (formationFound) {
                // Use exact circle position if formation center is safe
                hunterLoc = new Location(gameWorld, x, y, z);
            } else {
                // Otherwise try to find safe ground near the circle position
                Location ground = findSafeSurfaceLocation(new Location(gameWorld, x, center.getY(), z));
                hunterLoc = ground != null ? ground : center; // Fallback to center if needed
            }

            // Make hunter face the center
            hunterLoc.setDirection(center.toVector().subtract(hunterLoc.toVector()).setY(0).normalize());
            hunter.teleport(hunterLoc);
        }
    }

    private Location findSafeSurfaceLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // Start from highest block and search downward for a safe standing spot
        int highestY = world.getHighestBlockYAt(x, z);
        for (int y = highestY; y >= world.getMinHeight(); y--) {
            // Ensure head space is within world height
            if (y + 2 >= world.getMaxHeight()) continue;

            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            // Check for solid ground + two air/passable blocks above
            if (ground.getType().isSolid()
                    && (feet.isEmpty() || feet.isPassable()) && !feet.isLiquid()
                    && (head.isEmpty() || head.isPassable()) && !head.isLiquid()) {
                return new Location(world, x + 0.5, y + 1.0, z + 0.5);
            }
        }
        return null; // No safe surface found
    }

    private boolean isSafeForPlayer(Location loc) {
        // Validate 3-block vertically: below, feet, head
        Block feet = loc.getBlock();
        Block below = loc.clone().add(0, -1, 0).getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();

        return (feet.isEmpty() || feet.isPassable()) && !feet.isLiquid()
                && (above.isEmpty() || above.isPassable()) && !above.isLiquid()
                && below.getType().isSolid(); // Must stand on solid block
    }

    // All hunter positions must be on the same surface level as the runner.
    private boolean areHunterPositionsSafe(Location center, double radius, int hunterCount, World world) {
        double angleStep = 2 * Math.PI / hunterCount;
        double y = center.getY();

        // Check each hunter’s circle position for safety
        for (int i = 0; i < hunterCount; i++) {
            double angle = angleStep * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, y, z);

            if (!isSafeForPlayer(loc)) return false;
        }
        return true;
    }

    private Location findSafeLocationGuaranteed(World world, Location origin) {
        // Absolute fallback: scan vertically for ANY safe spot at origin X/Z
        int x = origin.getBlockX();
        int z = origin.getBlockZ();

        for (int y = world.getMaxHeight() - 2; y >= world.getMinHeight(); y--) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            if (ground.getType().isSolid()
                    && (feet.isEmpty() || feet.isPassable()) && !feet.isLiquid()
                    && (head.isEmpty() || head.isPassable()) && !head.isLiquid()) {
                return new Location(world, x + 0.5, y + 1.0, z + 0.5);
            }
        }

        // If literally nothing is safe, return origin unchanged
        return origin;
    }
}
