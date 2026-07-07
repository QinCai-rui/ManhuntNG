package xyz.qincai.manhunt.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xyz.qincai.manhunt.PaperManhuntNG;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.platform.ConfigProvider;
import xyz.qincai.manhunt.platform.WorldProvider;
import xyz.qincai.manhunt.player.PlayerEffect;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class PaperWorldProvider implements WorldProvider {
    private final PaperManhuntNG plugin;

    public PaperWorldProvider(PaperManhuntNG plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createGameWorlds(Match match) {
        if (match.isUsingExistingWorld()) {
            loadExistingWorlds(match);
        } else {
            createGeneratedWorlds(match);
        }
    }

    private void loadExistingWorlds(Match match) {
        String worldName = match.getWorldName();

        World overworld = Bukkit.getWorld(worldName);
        if (overworld == null) {
            overworld = loadWorld(worldName);
            if (overworld == null) {
                plugin.getLogger().severe("Failed to load existing world: " + worldName);
                plugin.getUIFacade().broadcastMessage("\u00a7cFailed to load world: " + worldName);
                match.setState(xyz.qincai.manhunt.game.GameState.WAITING);
                return;
            }
        }

        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(GameRule.DO_FIRE_TICK, true);

        World nether = Bukkit.getWorld(worldName + "_nether");
        if (nether == null) {
            nether = loadWorld(worldName + "_nether");
        }

        World end = Bukkit.getWorld(worldName + "_the_end");
        if (end == null) {
            end = loadWorld(worldName + "_the_end");
        }

        match.setGameWorldName(overworld.getName());
        if (nether != null) match.setNetherWorldName(nether.getName());
        if (end != null) match.setEndWorldName(end.getName());

        plugin.getLogger().info("Loaded existing world: " + worldName);
    }

    private World loadWorld(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            plugin.getLogger().warning("World folder does not exist: " + worldName);
            return null;
        }

        WorldCreator creator = new WorldCreator(worldName);
        return creator.createWorld();
    }

    private void createGeneratedWorlds(Match match) {
        cleanupOldWorlds(match);

        String worldName = "manhunt_" + System.currentTimeMillis();
        Long seed = match.getSeed();

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(true);
        if (seed != null) {
            creator.seed(seed);
        }
        World overworld = creator.createWorld();
        if (overworld == null) {
            plugin.getLogger().severe("Failed to create overworld!");
            return;
        }
        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(GameRule.DO_FIRE_TICK, true);

        WorldCreator netherCreator = new WorldCreator(worldName + "_nether");
        netherCreator.environment(World.Environment.NETHER);
        if (seed != null) {
            netherCreator.seed(seed);
        }
        World nether = netherCreator.createWorld();
        if (nether == null) {
            plugin.getLogger().severe("Failed to create nether world!");
            return;
        }

        WorldCreator endCreator = new WorldCreator(worldName + "_the_end");
        endCreator.environment(World.Environment.THE_END);
        if (seed != null) {
            endCreator.seed(seed);
        }
        World end = endCreator.createWorld();
        if (end == null) {
            plugin.getLogger().severe("Failed to create end world!");
            return;
        }

        match.setGameWorldName(overworld.getName());
        match.setNetherWorldName(nether.getName());
        match.setEndWorldName(end.getName());

        overworld.setSpawnLocation(overworld.getSpawnLocation());
    }

    private void cleanupOldWorlds(Match match) {
        String currentWorldName = match.getGameWorldName();

        List<World> toRemove = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("manhunt_")) {
                if (currentWorldName != null && world.getName().equals(currentWorldName)) {
                    continue;
                }
                toRemove.add(world);
            }
        }

        for (World world : toRemove) {
            plugin.getLogger().info("Removing old manhunt world: " + world.getName());

            for (Player player : world.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }

            Bukkit.unloadWorld(world, false);
            File worldFolder = world.getWorldFolder();
            deleteFolder(worldFolder);
        }
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }

    @Override
    public void teleportToMainWorld(Match match) {
        World mainWorld = Bukkit.getWorlds().get(0);
        Location spawn = mainWorld.getSpawnLocation();

        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null && runner.isOnline()) {
                runner.teleport(spawn);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawn);
            }
        }

        for (UUID uuid : match.getSpectatorUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawn);
            }
        }
    }

    @Override
    public void teleportToFormation(Match match) {
        String gameWorldName = match.getGameWorldName();
        if (gameWorldName == null) return;
        if (match.getRunnerUuid() == null) return;

        World gameWorld = Bukkit.getWorld(gameWorldName);
        if (gameWorld == null) return;

        Player runner = Bukkit.getPlayer(match.getRunnerUuid());
        if (runner == null) return;

        ConfigProvider config = plugin.getConfigProvider();
        double radius = config.getHunterCircleRadius();
        int searchRadius = config.getFormationSearchRadius();
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
            for (int range = 1; range <= searchRadius; range++) {
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
            Player hunter = Bukkit.getPlayer(hunterUuid);
            if (hunter == null) continue;

            double angle = angleStep * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location hunterLoc;
            if (formationFound) {
                hunterLoc = new Location(gameWorld, x, y, z);
            } else {
                Location ground = findSafeSurfaceLocation(new Location(gameWorld, x, center.getY(), z));
                hunterLoc = ground != null ? ground : center;
            }
            hunterLoc.setDirection(center.toVector().subtract(hunterLoc.toVector()).setY(0).normalize());
            hunter.teleport(hunterLoc);
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

    @Override
    public void freezeAllPlayers(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.setWalkSpeed(0f);
                runner.setFlySpeed(0f);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setWalkSpeed(0f);
                player.setFlySpeed(0f);
            }
        }
    }

    @Override
    public void unfreezeAllPlayers(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.setWalkSpeed(0.2f);
                runner.setFlySpeed(0.1f);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
            }
        }
    }

    @Override
    public void unfreezeHorizontalAllPlayers(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.setWalkSpeed(0f);
                runner.setFlySpeed(0f);
                runner.setInvulnerable(true);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
            }
        }
    }

    @Override
    public void setAllPlayersSurvival(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.setGameMode(GameMode.SURVIVAL);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @Override
    public void healAllPlayers(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) healPlayer(runner);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) healPlayer(player);
        }
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @Override
    public void clearPlayerState(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) clearPlayer(runner);
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) clearPlayer(player);
        }
    }

    private void clearPlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInMainHand(null);
        player.getInventory().setItemInOffHand(null);

        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(iterator.next());
            progress.getAwardedCriteria().forEach(progress::revokeCriteria);
        }
    }

    @Override
    public void setAllPlayersInvulnerable(Match match, boolean invulnerable) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.setInvulnerable(invulnerable);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setInvulnerable(invulnerable);
        }
    }

    @Override
    public void setInvulnerable(UUID uuid, boolean invulnerable) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.setInvulnerable(invulnerable);
    }

    @Override
    public void setGameRule(Match match, String rule, boolean value) {
        String worldName = match.getGameWorldName();
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        @SuppressWarnings("unchecked")
        GameRule<Boolean> gameRule = (GameRule<Boolean>) GameRule.getByName(rule);
        if (gameRule != null) {
            world.setGameRule(gameRule, value);
        }
    }

    @Override
    public void clearMobTargets(Match match) {
        String worldName = match.getGameWorldName();
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Mob mob) {
                if (mob.getTarget() instanceof Player) {
                    mob.setTarget(null);
                }
            }
        }
    }

    @Override
    public void clearEffects(Match match) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.getActivePotionEffects().stream().map(PotionEffect::getType)
                        .forEach(runner::removePotionEffect);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter != null) {
                hunter.getActivePotionEffects().stream().map(PotionEffect::getType)
                        .forEach(hunter::removePotionEffect);
            }
        }
    }

    @Override
    public void sendTitle(UUID playerUuid, String title, String subtitle) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        Component titleComp = Component.text(title, NamedTextColor.GOLD);
        Component subtitleComp = subtitle != null ? Component.text(subtitle, NamedTextColor.YELLOW) : Component.empty();

        Title titleObj = Title.title(titleComp, subtitleComp, Title.Times.times(
                Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));

        player.showTitle(titleObj);
    }
}
