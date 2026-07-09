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
        if (match.getRunnerUuids().isEmpty()) return; // No runner selected yet

        List<UUID> runnerUuids = List.copyOf(match.getRunnerUuids()); // Copy to avoid concurrent modification
        List<UUID> hunterUuids = List.copyOf(match.getHunterUuids());
        int runnerCount = runnerUuids.size();
        int hunterCount = hunterUuids.size();

        // Decide who forms the ring and who stands in the center.
        // If there are strictly more runners than hunters, the runners surround the hunter(s).
        // Otherwise (hunters >= runners) the hunters surround the runner(s) - the default behaviour.
        boolean runnersSurround = runnerCount > hunterCount && hunterCount > 0;

        // The center holds the smaller group: a hunter when runners surround, otherwise a runner.
        List<UUID> centerGroup = runnersSurround ? hunterUuids : runnerUuids;
        // The outer ring holds the larger group: the runners when they surround, otherwise the hunters.
        List<UUID> outerGroup = runnersSurround ? runnerUuids : hunterUuids;
        int outerCount = outerGroup.size();

        UUID centerUuid = centerGroup.get(0);
        Player centerPlayer = org.bukkit.Bukkit.getPlayer(centerUuid);
        if (centerPlayer == null) return; // Center player offline or not found

        double radius = plugin.getConfigManager().getHunterCircleRadius(); // Circle radius for the outer ring
        int searchRadius = plugin.getConfigManager().getFormationSearchRadius(); // How far we search for safe formation center

        Location spawnLoc = gameWorld.getSpawnLocation();
        Location center = findSafeSurfaceLocation(spawnLoc); // Try vanilla spawn first

        // Check if vanilla spawn is safe AND the outer ring is safe
        boolean formationFound = center != null
                && (outerCount == 0 || areOuterPositionsSafe(center, radius, outerCount, gameWorld));

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

                        // Ensure the outer ring can also stand safely around this point
                        if (outerCount > 0 && !areOuterPositionsSafe(safe, radius, outerCount, gameWorld))
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

        // Set world spawn to the formation center so players without a bed respawn here
        gameWorld.setSpawnLocation(center);

        // Teleport the center group. The first member stands exactly at the center;
        // any remaining members cluster tightly around it (the inner ring).
        for (int i = 0; i < centerGroup.size(); i++) {
            Player member = org.bukkit.Bukkit.getPlayer(centerGroup.get(i));
            if (member == null) continue;

            if (i == 0) {
                member.teleport(center);
                continue;
            }

            double innerAngle = 2 * Math.PI / centerGroup.size() * i;
            double innerRadius = Math.min(2.0, radius * 0.5);
            double x = center.getX() + innerRadius * Math.cos(innerAngle);
            double z = center.getZ() + innerRadius * Math.sin(innerAngle);

            Location innerLoc;
            if (formationFound) {
                innerLoc = new Location(gameWorld, x, center.getY(), z);
            } else {
                Location ground = findSafeSurfaceLocation(new Location(gameWorld, x, center.getY(), z));
                innerLoc = ground != null ? ground : center;
            }
            member.teleport(innerLoc);
        }

        if (outerCount == 0) return; // No outer members -> formation ends here

        // Compute outer ring positions in a CIRCLE around the center
        double y = center.getY();
        double angleStep = 2 * Math.PI / outerCount;

        for (int i = 0; i < outerCount; i++) {
            UUID outerUuid = outerGroup.get(i);
            Player outer = org.bukkit.Bukkit.getPlayer(outerUuid);
            if (outer == null) continue; // Skip offline players

            double angle = angleStep * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location outerLoc;

            if (formationFound) {
                // Use exact circle position if formation center is safe
                outerLoc = new Location(gameWorld, x, y, z);
            } else {
                // Otherwise try to find safe ground near the circle position
                Location ground = findSafeSurfaceLocation(new Location(gameWorld, x, center.getY(), z));
                outerLoc = ground != null ? ground : center; // Fallback to center if needed
            }

            // Make the outer member face the center
            // Guard against zero-length vector when outerLoc equals center
            if (!outerLoc.toVector().equals(center.toVector())) {
                outerLoc.setDirection(center.toVector().subtract(outerLoc.toVector()).setY(0).normalize());
            }
            outer.teleport(outerLoc);
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

    // All outer-ring positions must be on the same surface level as the center.
    private boolean areOuterPositionsSafe(Location center, double radius, int outerCount, World world) {
        double angleStep = 2 * Math.PI / outerCount;
        double y = center.getY();

        // Check each outer ring member's circle position for safety
        for (int i = 0; i < outerCount; i++) {
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
