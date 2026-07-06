package xyz.qincai.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.io.File;
import java.util.logging.Level;

public class WorldManager {
    private final ManhuntNG plugin;

    public WorldManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void createGameWorlds() {
        String worldName = "manhunt_" + System.currentTimeMillis();

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(true);
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
        World nether = netherCreator.createWorld();
        if (nether == null) {
            plugin.getLogger().severe("Failed to create nether world!");
            return;
        }

        WorldCreator endCreator = new WorldCreator(worldName + "_the_end");
        endCreator.environment(World.Environment.THE_END);
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

    public void deleteGameWorlds() {
        Match match = plugin.getGameManager().getMatch();
        World overworld = match.getGameWorld();
        World nether = match.getNetherWorld();
        World end = match.getEndWorld();

        if (overworld != null) {
            for (Player player : overworld.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }
        if (nether != null) {
            for (Player player : nether.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }
        if (end != null) {
            for (Player player : end.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (nether != null) {
                Bukkit.unloadWorld(nether, false);
                deleteWorldFolder(nether.getName());
            }
            if (end != null) {
                Bukkit.unloadWorld(end, false);
                deleteWorldFolder(end.getName());
            }
            if (overworld != null) {
                Bukkit.unloadWorld(overworld, false);
                deleteWorldFolder(overworld.getName());
            }
        }, 20L);
    }

    private void deleteWorldFolder(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            deleteRecursive(worldFolder);
            plugin.getLogger().info("Deleted world folder: " + worldName);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
