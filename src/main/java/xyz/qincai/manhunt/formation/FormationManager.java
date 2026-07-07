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
        if (gameWorld == null) return;
        if (match.getRunnerUuid() == null) return;

        Player runner = org.bukkit.Bukkit.getPlayer(match.getRunnerUuid());
        if (runner == null) return;

        double radius = plugin.getConfigManager().getHunterCircleRadius();
        List<UUID> hunterUuids = List.copyOf(match.getHunterUuids());
        int hunterCount = hunterUuids.size();

        Location spawnLoc = gameWorld.getSpawnLocation();
        Location center = findSafeSurfaceLocation(spawnLoc);
        boolean formationFound = center != null
                && (hunterCount == 0 || areHunterPositionsSafe(center, radius, hunterCount, gameWorld));

        if (!formationFound) {
            int spawnX = spawnLoc.getBlockX();
            int spawnZ = spawnLoc.getBlockZ();

            outer:
            for (int range = 1; range <= 20; range++) {
                for (int dx = -range; dx <= range; dx++) {
                    for (int dz = -range; dz <= range; dz++) {
                        if (Math.abs(dx) != range && Math.abs(dz) != range) continue;

                        Location candidate = new Location(gameWorld, spawnX + dx + 0.5, 0, spawnZ + dz + 0.5);
                        Location safe = findSafeSurfaceLocation(candidate);
                        if (safe == null) continue;
                        if (!isSafeForPlayer(safe)) continue;
                        if (hunterCount > 0 && !areHunterPositionsSafe(safe, radius, hunterCount, gameWorld))
                            continue;

                        center = safe;
                        formationFound = true;
                        break outer;
                    }
                }
            }
        }

        if (center == null || !isSafeForPlayer(center)) {
            center = findSafeLocationGuaranteed(gameWorld, spawnLoc);
        }

        runner.teleport(center);

        if (hunterCount == 0) return;

        double y = center.getY();
        double angleStep = 2 * Math.PI / hunterCount;
        for (int i = 0; i < hunterCount; i++) {
            UUID hunterUuid = hunterUuids.get(i);
            Player hunter = org.bukkit.Bukkit.getPlayer(hunterUuid);
            if (hunter == null) continue;

            if (formationFound) {
                double angle = angleStep * i;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                Location hunterLoc = new Location(gameWorld, x, y, z);
                hunterLoc.setDirection(center.toVector().subtract(hunterLoc.toVector()).setY(0).normalize());
                hunter.teleport(hunterLoc);
            } else {
                hunter.teleport(center);
            }
        }
    }

    private Location findSafeSurfaceLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int highestY = world.getHighestBlockYAt(x, z);
        for (int y = highestY; y >= world.getMinHeight(); y--) {
            if (y + 2 >= world.getMaxHeight()) continue;
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (ground.getType().isSolid()
                    && (feet.isEmpty() || feet.isPassable()) && !feet.isLiquid()
                    && (head.isEmpty() || head.isPassable()) && !head.isLiquid()) {
                return new Location(world, x + 0.5, y + 1.0, z + 0.5);
            }
        }
        return null;
    }

    private boolean isSafeForPlayer(Location loc) {
        Block feet = loc.getBlock();
        Block below = loc.clone().add(0, -1, 0).getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();
        return (feet.isEmpty() || feet.isPassable()) && !feet.isLiquid()
                && (above.isEmpty() || above.isPassable()) && !above.isLiquid()
                && below.getType().isSolid();
    }

    // All hunter positions must be on the same surface level as the runner.
    private boolean areHunterPositionsSafe(Location center, double radius, int hunterCount, World world) {
        double angleStep = 2 * Math.PI / hunterCount;
        double y = center.getY();
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
        return origin;
    }
}
