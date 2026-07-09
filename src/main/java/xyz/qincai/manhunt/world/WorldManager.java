package xyz.qincai.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorldManager {
    private final ManhuntNG plugin;

    public WorldManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void createGameWorlds() {
        Match match = plugin.getGameManager().getMatch();

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
                plugin.getUiManager().broadcastMessage(plugin.getConfigManager().getMessage("world.load-failed", "{world}", worldName));
                match.setState(xyz.qincai.manhunt.game.GameState.WAITING);
                return;
            }
        }

        overworld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);

        World nether = Bukkit.getWorld(worldName + "_nether");
        if (nether == null) {
            nether = loadWorld(worldName + "_nether");
        }

        World end = Bukkit.getWorld(worldName + "_the_end");
        if (end == null) {
            end = loadWorld(worldName + "_the_end");
        }

        match.setGameWorld(overworld);
        match.setNetherWorld(nether);
        match.setEndWorld(end);

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
        cleanupOldWorlds();

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
        overworld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);

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

        match.setGameWorld(overworld);
        match.setNetherWorld(nether);
        match.setEndWorld(end);

        overworld.setSpawnLocation(overworld.getSpawnLocation());
    }

    private void cleanupOldWorlds() {
        Match match = plugin.getGameManager().getMatch();
        String currentWorldName = match.getWorldName();

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

            for (org.bukkit.entity.Player player : world.getPlayers()) {
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

    public void teleportToMainWorld() {
        Match match = plugin.getGameManager().getMatch();
        World mainWorld = Bukkit.getWorlds().get(0);
        org.bukkit.Location spawn = mainWorld.getSpawnLocation();

        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
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
}
