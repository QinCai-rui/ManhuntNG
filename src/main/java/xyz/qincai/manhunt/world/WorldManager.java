package xyz.qincai.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldManager {
    private final ManhuntNG plugin;

    public WorldManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void createGameWorlds() {
        cleanupOldWorlds();

        String worldName = "manhunt_" + System.currentTimeMillis();

        Long seed = plugin.getGameManager().getMatch().getSeed();

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

        Match match = plugin.getGameManager().getMatch();
        match.setGameWorld(overworld);
        match.setNetherWorld(nether);
        match.setEndWorld(end);

        overworld.setSpawnLocation(overworld.getSpawnLocation());
    }

    private void cleanupOldWorlds() {
        List<World> toRemove = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("manhunt_")) {
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
}
